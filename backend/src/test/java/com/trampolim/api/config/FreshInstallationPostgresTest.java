package com.trampolim.api.config;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.trampolim.api.BackendApplication;
class FreshInstallationPostgresTest {
    @Test void secondaryDatabaseOutageDoesNotPreventCandidateRegistration() throws Exception {
        String schema="outage_test_"+UUID.randomUUID().toString().replace("-","");
        List<JdbcTemplate> created=new ArrayList<>();
        List<String> arguments=new ArrayList<>(List.of("--server.port=0","--spring.jpa.hibernate.ddl-auto=validate","--app.mail.enabled=false"));
        try {
            configure(arguments,created,schema,"spring.datasource","OPORTUNIDADES",env("DATABASE_URL","jdbc:postgresql://localhost:5432/trampolim_db"),env("DATABASE_USERNAME","root"),env("DATABASE_PASSWORD","root"));
            for(String trail:List.of("empresas","capacita")) {
                String prefix="--trampolim.datasource."+trail;
                arguments.add(prefix+".enabled=true");
                // Closed local port exercises a real connection refusal without stopping any database.
                arguments.add(prefix+".url=jdbc:postgresql://127.0.0.1:1/unavailable_"+trail+"?connectTimeout=1");
                arguments.add(prefix+".username=outage_fixture");
                arguments.add(prefix+".password=outage_fixture");
            }
            try(var context=new SpringApplicationBuilder(BackendApplication.class).run(arguments.toArray(String[]::new))) {
                int port=context.getEnvironment().getRequiredProperty("local.server.port",Integer.class);
                var client=java.net.http.HttpClient.newHttpClient();
                for(String trail:List.of("empresas","capacita")) {
                    context.getBean(trail+"DataSource",com.zaxxer.hikari.HikariDataSource.class).setConnectionTimeout(1000);
                    var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/"+trail+"/auth/login"))
                        .timeout(java.time.Duration.ofSeconds(10)).header("Content-Type","application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"email\":\"outage@example.test\",\"password\":\"FixturePassword!\"}")).build();
                    var response=client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());
                    assertEquals(503,response.statusCode());
                    assertTrue(response.body().contains("temporariamente indisponível"));
                    assertFalse(response.body().contains("jdbc:postgresql"));
                }
                var registration=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/auth/cadastro"))
                    .timeout(java.time.Duration.ofSeconds(10)).header("Content-Type","application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"name\":\"Outage Fixture\",\"email\":\"outage@example.test\",\"password\":\"FixturePassword!\",\"termsAccepted\":true}")).build();
                assertEquals(200,client.send(registration,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode());
            }
        } finally { for(var jdbc:created) jdbc.execute("DROP SCHEMA "+schema+" CASCADE"); }
    }
    @Test void freshInstallationStartsWithFlywayAndHibernateValidation() throws Exception {
        String schema="fresh_test_"+UUID.randomUUID().toString().replace("-","");
        List<JdbcTemplate> created=new ArrayList<>();
        List<String> arguments=new ArrayList<>(List.of("--server.port=0","--spring.jpa.hibernate.ddl-auto=validate","--app.mail.enabled=false"));
        try {
            configure(arguments,created,schema,"spring.datasource","OPORTUNIDADES",env("DATABASE_URL","jdbc:postgresql://localhost:5432/trampolim_db"),env("DATABASE_USERNAME","root"),env("DATABASE_PASSWORD","root"));
            configure(arguments,created,schema,"trampolim.datasource.empresas","EMPRESAS","jdbc:postgresql://localhost:5433/trampolim_empresas_db","trampolim_empresas","dev_empresas_only");
            configure(arguments,created,schema,"trampolim.datasource.capacita","CAPACITA","jdbc:postgresql://localhost:5434/trampolim_capacita_db","trampolim_capacita","dev_capacita_only");
            arguments.add("--trampolim.datasource.empresas.enabled=true");
            arguments.add("--trampolim.datasource.capacita.enabled=true");
            try(var context=new SpringApplicationBuilder(BackendApplication.class).run(arguments.toArray(String[]::new))) {
                assertNotNull(context.getBean(com.trampolim.api.auth.AuthController.class));
                assertNotNull(context.getBean(com.trampolim.api.auth.IndependentTrailAuthController.class));
                assertNotNull(context.getBean(com.trampolim.api.company.CompanyController.class));
                assertNotNull(context.getBean(com.trampolim.api.education.CapacitaController.class));
                int port=context.getEnvironment().getRequiredProperty("local.server.port",Integer.class);
                var client=java.net.http.HttpClient.newHttpClient();
                for(String route:List.of("/profile","/company/dashboard","/capacita/progress")) {
                    var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api"+route))
                        .timeout(java.time.Duration.ofSeconds(20)).GET().build();
                    int status=client.send(request,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
                    assertTrue(status==401||status==403,"Private API must reject anonymous access: "+route);
                }
                verifyIndependentAccounts(port,client);
                String[] beans={"jdbcTemplate","empresasJdbcTemplate","capacitaJdbcTemplate"};
                String[] tables={"app_users","company_users","student_users"};
                for(int i=0;i<beans.length;i++) assertEquals(1,
                    context.getBean(beans[i],JdbcTemplate.class).queryForObject(
                        "SELECT count(*) FROM "+tables[i]+" WHERE email=?",Integer.class,"isolated@example.test"));
                verifyResetIsolation(port,client,context);
                verifyCorporateDeletion(port,client,context);
                verifyConcurrentLoginFailures(port,client,context);
                for(String bean:List.of("jdbcTemplate","empresasJdbcTemplate","capacitaJdbcTemplate")) {
                    var jdbc=context.getBean(bean,JdbcTemplate.class);
                    assertEquals(schema,jdbc.queryForObject("SELECT current_schema()",String.class));
                    assertTrue(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success",Long.class)>0);
                }
            }
        } finally { for(var jdbc:created) jdbc.execute("DROP SCHEMA "+schema+" CASCADE"); }
    }
    private void verifyIndependentAccounts(int port,java.net.http.HttpClient client) throws Exception {
        String base="http://127.0.0.1:"+port+"/api";
        String[] registrations={"/auth/cadastro","/empresas/auth/register","/capacita/auth/register"};
        String[] extra={"", """
            ,"legalName":"Fictitious Test Company","tradeName":"Test Fixture","cnpj":"11222333000181",
            "phone":"11999999999","industry":"Test","address":"Test address","city":"Test","state":"SP"
            """, ",\"educationStage\":\"HIGH_SCHOOL\",\"ageBand\":\"AGE_18_PLUS\""};
        String[] tokens=new String[3];
        for(int i=0;i<3;i++) {
            String body="{\"name\":\"HTTP Test Fixture\",\"email\":\"isolated@example.test\",\"password\":\"IndependentPassword"+i+"!\",\"termsAccepted\":true"+extra[i]+"}";
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+registrations[i]))
                .timeout(java.time.Duration.ofSeconds(20)).header("Content-Type","application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();
            var response=client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200,response.statusCode(),"Independent registration: "+registrations[i]);
            var matcher=java.util.regex.Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
            assertTrue(matcher.find(),"Registration must return a session token");
            tokens[i]=matcher.group(1);
        }
        String[] privateRoutes={"/profile","/company/dashboard","/capacita/progress"};
        for(int account=0;account<3;account++) for(int area=0;area<3;area++) {
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+privateRoutes[area]))
                .timeout(java.time.Duration.ofSeconds(20)).header("Authorization","Bearer "+tokens[account]).GET().build();
            int status=client.send(request,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
            if(account==area) assertEquals(200,status,"Own token accepted: "+privateRoutes[area]);
            else assertTrue(status==401||status==403,"Foreign token rejected: "+privateRoutes[area]);
        }
        String[] logins={"/auth/login","/empresas/auth/login","/capacita/auth/login"};
        for(int area=0;area<3;area++) for(int password=0;password<3;password++) {
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+logins[area]))
                .timeout(java.time.Duration.ofSeconds(20)).header("Content-Type","application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                    "{\"email\":\"isolated@example.test\",\"password\":\"IndependentPassword"+password+"!\"}")).build();
            int status=client.send(request,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
            if(area==password) assertEquals(200,status,"Own password accepted: "+logins[area]);
            else assertEquals(401,status,"Other account password rejected: "+logins[area]);
        }
        var logout=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/capacita/session/logout"))
            .timeout(java.time.Duration.ofSeconds(20)).header("Authorization","Bearer "+tokens[2])
            .POST(java.net.http.HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(204,client.send(logout,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode());
        for(int area=0;area<3;area++) {
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+privateRoutes[area]))
                .timeout(java.time.Duration.ofSeconds(20)).header("Authorization","Bearer "+tokens[area]).GET().build();
            int status=client.send(request,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
            if(area==2) assertTrue(status==401||status==403,"Logged out student token must be revoked");
            else assertEquals(200,status,"Student logout must preserve other trail sessions");
        }
    }
    private void verifyResetIsolation(int port,java.net.http.HttpClient client,
            org.springframework.context.ConfigurableApplicationContext context) throws Exception {
        String[] beans={"jdbcTemplate","empresasJdbcTemplate","capacitaJdbcTemplate"};
        String[] tables={"app_users","company_users","student_users"};
        String[] routes={"/auth/password/reset","/empresas/auth/password/reset","/capacita/auth/password/reset"};
        for(int origin=0;origin<3;origin++) {
            String token=UUID.randomUUID().toString();
            String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            context.getBean(beans[origin],JdbcTemplate.class).update("UPDATE "+tables[origin]+
                " SET password_reset_token_hash=?,password_reset_expires_at=CURRENT_TIMESTAMP+INTERVAL '5 minutes' WHERE email=?",
                hash,"isolated@example.test");
            // Reject the link in both foreign trails before consuming it in its own trail.
            for(int step=1;step<=4;step++) {
                int target=(origin+(step<=2?step:0))%3;
                var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api"+routes[target]))
                    .timeout(java.time.Duration.ofSeconds(20)).header("Content-Type","application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"token\":\""+token+"\",\"password\":\"NewIndependentPassword!\"}")).build();
                if(step==3) {
                    var first=client.sendAsync(request,java.net.http.HttpResponse.BodyHandlers.discarding());
                    var second=client.sendAsync(request,java.net.http.HttpResponse.BodyHandlers.discarding());
                    var statuses=new ArrayList<>(List.of(first.get(25,java.util.concurrent.TimeUnit.SECONDS).statusCode(),
                        second.get(25,java.util.concurrent.TimeUnit.SECONDS).statusCode()));
                    statuses.sort(Integer::compareTo);
                    assertEquals(List.of(200,400),statuses,"Concurrent requests must consume a reset link exactly once: "+routes[origin]);
                } else {
                    int status=client.send(request,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
                    assertEquals(400,status,"Reset link must remain trail-specific and single-use");
                }
            }
        }
    }
    private void verifyCorporateDeletion(int port,java.net.http.HttpClient client,
            org.springframework.context.ConfigurableApplicationContext context) throws Exception {
        var jdbc=context.getBean("empresasJdbcTemplate",JdbcTemplate.class);
        var encoder=context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class);
        Long id=jdbc.queryForObject("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at) VALUES ('deletion@example.test',?,'HTTP deletion fixture',TRUE,'ACTIVE',CURRENT_TIMESTAMP) RETURNING id",Long.class,encoder.encode("DeletionPassword!"));
        var tokens=context.getBean(com.trampolim.api.security.TrailTokenService.class);
        String token=tokens.issue(com.trampolim.api.security.TrailTokenService.Trail.EMPRESAS,"deletion@example.test",id,0);
        for(String password:List.of("wrong-password","DeletionPassword!")) {
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/empresas/account"))
                .timeout(java.time.Duration.ofSeconds(20)).header("Content-Type","application/json").header("Authorization","Bearer "+token)
                .method("DELETE",java.net.http.HttpRequest.BodyPublishers.ofString("{\"password\":\""+password+"\"}")).build();
            assertEquals(password.equals("DeletionPassword!")?204:400,client.send(request,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode());
        }
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM company_users WHERE id=?",Integer.class,id));
        var revoked=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/empresas/account/export"))
            .timeout(java.time.Duration.ofSeconds(20)).header("Authorization","Bearer "+token).GET().build();
        int status=client.send(revoked,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
        assertTrue(status==401||status==403,"Deleted account token must be rejected");
        assertEquals(1,context.getBean("capacitaJdbcTemplate",JdbcTemplate.class).queryForObject("SELECT count(*) FROM student_users WHERE email='isolated@example.test'",Integer.class));
    }
    private void verifyConcurrentLoginFailures(int port,java.net.http.HttpClient client,
            org.springframework.context.ConfigurableApplicationContext context) throws Exception {
        String[] beans={"jdbcTemplate","empresasJdbcTemplate","capacitaJdbcTemplate"};
        String[] tables={"app_users","company_users","student_users"};
        String[] routes={"/auth/login","/empresas/auth/login","/capacita/auth/login"};
        for(int area=0;area<3;area++) {
            var jdbc=context.getBean(beans[area],JdbcTemplate.class);
            jdbc.update("UPDATE "+tables[area]+" SET failed_login_attempts=3,login_locked_until=NULL WHERE email='isolated@example.test'");
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api"+routes[area]))
                .timeout(java.time.Duration.ofSeconds(20)).header("Content-Type","application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"email\":\"isolated@example.test\",\"password\":\"InvalidPassword!\"}")).build();
            var first=client.sendAsync(request,java.net.http.HttpResponse.BodyHandlers.discarding());
            var second=client.sendAsync(request,java.net.http.HttpResponse.BodyHandlers.discarding());
            assertEquals(401,first.get(25,java.util.concurrent.TimeUnit.SECONDS).statusCode());
            assertEquals(401,second.get(25,java.util.concurrent.TimeUnit.SECONDS).statusCode());
            assertTrue(jdbc.queryForObject("SELECT login_locked_until>CURRENT_TIMESTAMP FROM "+tables[area]+" WHERE email='isolated@example.test'",Boolean.class),"Concurrent failures must trigger account lock: "+routes[area]);
            var correctPassword=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api"+routes[area]))
                .timeout(java.time.Duration.ofSeconds(20)).header("Content-Type","application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"email\":\"isolated@example.test\",\"password\":\"NewIndependentPassword!\"}")).build();
            assertEquals(429,client.send(correctPassword,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode(),"Correct password must not bypass an active lock");
        }
    }
    private void configure(List<String> args,List<JdbcTemplate> created,String schema,String property,String prefix,String fallbackUrl,String fallbackUser,String fallbackPassword) {
        String url=env(prefix+"_DB_URL",fallbackUrl),user=env(prefix+"_DB_USERNAME",fallbackUser),password=env(prefix+"_DB_PASSWORD",fallbackPassword);
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(url,user,password));
        jdbc.execute("CREATE SCHEMA "+schema); created.add(jdbc);
        args.add("--"+property+".url="+url+(url.contains("?")?"&":"?")+"currentSchema="+schema);
        args.add("--"+property+".username="+user); args.add("--"+property+".password="+password);
    }
    private String env(String key,String fallback) {String value=System.getenv(key);return value==null||value.isBlank()?fallback:value;}
}

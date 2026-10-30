package com.trampolim.api.education;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;import java.net.http.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.io.*;import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.trampolim.api.BackendApplication;

class LibraryHttpPostgresTest {
    private final HttpClient client=HttpClient.newHttpClient();private final ObjectMapper json=new ObjectMapper();private String base;
    @Test void realPdfLifecycleProtectedDownloadsAndIndependentAuthentication()throws Exception{
        String schema="library_http_"+UUID.randomUUID().toString().replace("-","");String url=env("CAPACITA_DB_URL","jdbc:postgresql://localhost:5434/trampolim_capacita_db");
        var admin=new JdbcTemplate(new DriverManagerDataSource(url,env("CAPACITA_DB_USERNAME","trampolim_capacita"),env("CAPACITA_DB_PASSWORD","dev_capacita_only")));admin.execute("CREATE SCHEMA "+schema);Path media=Files.createTempDirectory("library-http-private-");
        try(var context=new SpringApplicationBuilder(BackendApplication.class).run("--server.port=0","--spring.jpa.hibernate.ddl-auto=validate","--app.mail.enabled=false","--capacita.admin-emails=library-teacher@example.test","--capacita.media.root="+media,"--trampolim.datasource.capacita.url="+url+(url.contains("?")?"&":"?")+"currentSchema="+schema)){
            base="http://127.0.0.1:"+context.getEnvironment().getRequiredProperty("local.server.port")+"/api/capacita";
            String teacher=register("library-teacher@example.test"),student=register("library-student@example.test"),other=register("library-other@example.test");
            long course=id(send("POST","/learning/courses","{\"title\":\"Library test course\",\"category\":\"Math\",\"status\":\"PUBLISHED\"}",teacher));
            String metadata="{\"courseId\":"+course+",\"title\":\"Real PDF fixture\",\"topic\":\"Fractions\",\"category\":\"Guide\",\"alternativeText\":\"Teacher alternative text\",\"accessScope\":\"STUDENTS\",\"allowDownload\":false,\"status\":\"DRAFT\"}";
            assertEquals(403,send("POST","/learning/library/materials",metadata,student).statusCode());long material=id(send("POST","/learning/library/materials",metadata,teacher));
            byte[] pdf=LibraryPostgresTest.pdf("Real PDF selectable text",false);assertEquals(403,upload(material,0,pdf,other).statusCode());var uploaded=upload(material,0,pdf,teacher);assertEquals(200,uploaded.statusCode(),uploaded.body());
            assertEquals(403,send("GET","/learning/library/materials/"+material+"/file",null,student).statusCode());
            String publish=metadata.substring(0,metadata.length()-1)+",\"revision\":1}";publish=publish.replace("\"DRAFT\"","\"PUBLISHED\"");assertEquals(204,send("PUT","/learning/library/materials/"+material,publish,teacher).statusCode());
            var listed=send("GET","/learning/library/materials?course="+course+"&search=Fractions",null,student);assertEquals(1,json.readTree(listed.body()).size());
            String path="/learning/library/materials/"+material;var file=client.send(request("GET",path+"/file",null,student).header("Range","bytes=0-127").build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(206,file.statusCode());assertEquals(128,file.body().length);assertTrue(file.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
            assertEquals(403,send("GET",path+"/file?download=true",null,student).statusCode());assertEquals(200,send("GET",path+"/file?download=true",null,teacher).statusCode());assertEquals(403,send("GET",path+"/file?version="+json.readTree(uploaded.body()).path("id").asLong(),null,student).statusCode());
            var text=json.readTree(send("GET",path+"/text",null,student).body());assertTrue(text.path("extractedText").asText().contains("selectable"));assertEquals("Teacher alternative text",text.path("alternativeText").asText());assertFalse(text.path("extractionVerified").asBoolean());
            assertEquals(403,send("GET",path+"/file",null,null).statusCode());String corporate=context.getBean(com.trampolim.api.security.TrailTokenService.class).issue(com.trampolim.api.security.TrailTokenService.Trail.EMPRESAS,"library-student@example.test",1L,0);assertEquals(403,send("GET",path+"/file",null,corporate).statusCode());
            assertEquals(400,upload(material,2,"not-pdf".getBytes(),teacher).statusCode());assertEquals(400,upload(material,2,LibraryPostgresTest.pdf("Active",true),teacher).statusCode());
            assertEquals(204,send("DELETE",path,null,teacher).statusCode());assertEquals(404,send("GET",path+"/file",null,student).statusCode());
        }finally{admin.execute("DROP SCHEMA "+schema+" CASCADE");try(var paths=Files.walk(media)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
    private HttpResponse<String> upload(long id,int revision,byte[] bytes,String token)throws Exception{String boundary="pdf-"+UUID.randomUUID();var body=new ByteArrayOutputStream();body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.pdf\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));body.write(bytes);body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));return client.send(HttpRequest.newBuilder(URI.create(base+"/learning/library/materials/"+id+"/versions?revision="+revision)).timeout(java.time.Duration.ofSeconds(60)).header("Authorization","Bearer "+token).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());}
    private String register(String email)throws Exception{var response=send("POST","/auth/register","{\"name\":\"Library fixture\",\"email\":\""+email+"\",\"password\":\"LibraryFixturePassword!\",\"educationStage\":\"HIGH_SCHOOL\",\"ageBand\":\"AGE_18_PLUS\",\"termsAccepted\":true}",null);assertEquals(200,response.statusCode());return json.readTree(response.body()).path("token").asText();}
    private HttpRequest.Builder request(String method,String path,String body,String token){var request=HttpRequest.newBuilder(URI.create(base+path)).timeout(java.time.Duration.ofSeconds(60)).header("Content-Type","application/json");if(token!=null)request.header("Authorization","Bearer "+token);return request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));}
    private HttpResponse<String> send(String method,String path,String body,String token)throws Exception{return client.send(request(method,path,body,token).build(),HttpResponse.BodyHandlers.ofString());}
    private long id(HttpResponse<String> response)throws Exception{assertEquals(200,response.statusCode(),response.body());return json.readTree(response.body()).path("id").asLong();}
    private static String env(String name,String fallback){String value=System.getenv(name);return value==null||value.isBlank()?fallback:value;}
}

package com.trampolim.api.education;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.trampolim.api.BackendApplication;

class LearningHttpPostgresTest {
    private final HttpClient client=HttpClient.newHttpClient();private final ObjectMapper json=new ObjectMapper();private String base;
    @Test void realUploadStreamingPermissionsAndLogout() throws Exception {
        Assumptions.assumeTrue(System.getenv("CAPACITA_FFMPEG")!=null&&System.getenv("CAPACITA_FFPROBE")!=null,"Configure as ferramentas de vídeo para esta integração real.");
        String schema="learning_http_"+UUID.randomUUID().toString().replace("-","");
        String url=env("CAPACITA_DB_URL","jdbc:postgresql://localhost:5434/trampolim_capacita_db");
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(url,env("CAPACITA_DB_USERNAME","trampolim_capacita"),env("CAPACITA_DB_PASSWORD","dev_capacita_only")));
        Path media=Files.createTempDirectory("learning-http-media-");jdbc.execute("CREATE SCHEMA "+schema);
        try(var context=new SpringApplicationBuilder(BackendApplication.class).run("--server.port=0","--spring.jpa.hibernate.ddl-auto=validate","--app.mail.enabled=false",
                "--trampolim.datasource.capacita.url="+url+(url.contains("?")?"&":"?")+"currentSchema="+schema,
                "--capacita.media.root="+media,"--capacita.admin-emails=learning-admin@example.test")) {
            base="http://127.0.0.1:"+context.getEnvironment().getRequiredProperty("local.server.port")+"/api/capacita/learning";
            String admin=register("learning-admin@example.test"),teacher=register("learning-teacher@example.test"),student=register("learning-student@example.test");
            assertEquals(204,send("PUT","/admin/roles","{\"email\":\"learning-teacher@example.test\",\"role\":\"TEACHER\",\"granted\":true}",admin).statusCode());
            long course=id(send("POST","/courses","{\"title\":\"HTTP test course\",\"category\":\"Math\",\"status\":\"PUBLISHED\"}",teacher));
            long subject=id(send("POST","/courses/"+course+"/subjects","{\"title\":\"Subject\",\"sortOrder\":0}",teacher));
            long module=id(send("POST","/subjects/"+subject+"/modules","{\"title\":\"Module\",\"sortOrder\":0}",teacher));
            long lesson=id(send("POST","/modules/"+module+"/lessons","{\"title\":\"Lesson\",\"status\":\"DRAFT\",\"sortOrder\":0}",teacher));
            String published="{\"title\":\"Lesson\",\"status\":\"PUBLISHED\",\"sortOrder\":0}";
            assertEquals(400,send("PUT","/lessons/"+lesson,published,teacher).statusCode());
            assertEquals(403,send("PUT","/lessons/"+lesson,published,student).statusCode());
            assertEquals(404,send("GET","/lessons/"+lesson,null,student).statusCode());
            byte[] video=Files.readAllBytes(Path.of("../frontend/e2e/fixtures/lesson-180s.mp4"));
            String boundary="test-"+UUID.randomUUID();var bytes=new java.io.ByteArrayOutputStream();
            bytes.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"lesson.mp4\"\r\nContent-Type: video/mp4\r\n\r\n").getBytes(StandardCharsets.UTF_8));bytes.write(video);bytes.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            var upload=HttpRequest.newBuilder(URI.create(base+"/lessons/"+lesson+"/video")).timeout(java.time.Duration.ofSeconds(60)).header("Authorization","Bearer "+teacher).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build();
            var uploaded=client.send(upload,HttpResponse.BodyHandlers.ofString());assertEquals(200,uploaded.statusCode());assertEquals("READY",json.readTree(uploaded.body()).path("validationStatus").asText());
            assertEquals(204,send("PUT","/lessons/"+lesson,published,teacher).statusCode());
            assertEquals(200,send("GET","/lessons/"+lesson,null,student).statusCode());
            var playback=send("POST","/lessons/"+lesson+"/playback",null,student);assertEquals(200,playback.statusCode());
            JsonNode session=json.readTree(playback.body());String videoUrl=base.substring(0,base.length()-"/capacita/learning".length())+session.path("videoPath").asText();
            var range=HttpRequest.newBuilder(URI.create(videoUrl)).timeout(java.time.Duration.ofSeconds(10)).header("Range","bytes=0-127").header("Origin","http://localhost:5173").GET().build();
            var chunk=client.send(range,HttpResponse.BodyHandlers.ofByteArray());assertEquals(206,chunk.statusCode());assertEquals(128,chunk.body().length);assertTrue(chunk.headers().firstValue("content-range").orElse("").startsWith("bytes 0-127/"));assertEquals("http://localhost:5173",chunk.headers().firstValue("access-control-allow-origin").orElse(""));
            String sessionId=session.path("sessionId").asText();
            assertEquals(200,send("POST","/lessons/"+lesson+"/progress","{\"sessionId\":\""+sessionId+"\",\"positionSeconds\":180,\"playing\":true}",student).statusCode());
            assertEquals(403,send("GET","/lessons/"+lesson+"/video",null,null).statusCode());
            assertEquals(204,client.send(HttpRequest.newBuilder(URI.create(base.replace("/learning","/session/logout"))).header("Authorization","Bearer "+student).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.discarding()).statusCode());
            assertEquals(404,client.send(range,HttpResponse.BodyHandlers.discarding()).statusCode());
            assertEquals(204,send("PUT","/admin/roles","{\"email\":\"learning-teacher@example.test\",\"role\":\"TEACHER\",\"granted\":false}",admin).statusCode());
            assertEquals(403,send("PUT","/lessons/"+lesson,published,teacher).statusCode());
        } finally {
            jdbc.execute("DROP SCHEMA "+schema+" CASCADE");
            try(var paths=Files.walk(media)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);}
        }
    }
    private String register(String email) throws Exception {
        String endpoint=base.replace("/learning","/auth/register");
        var request=HttpRequest.newBuilder(URI.create(endpoint)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Learning fixture\",\"email\":\""+email+"\",\"password\":\"LearningFixturePassword!\",\"educationStage\":\"HIGH_SCHOOL\",\"ageBand\":\"AGE_18_PLUS\",\"termsAccepted\":true}")).build();
        var response=client.send(request,HttpResponse.BodyHandlers.ofString());assertEquals(200,response.statusCode());return json.readTree(response.body()).path("token").asText();
    }
    private HttpResponse<String> send(String method,String path,String body,String token) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(base+path)).timeout(java.time.Duration.ofSeconds(20)).header("Content-Type","application/json");if(token!=null) request.header("Authorization","Bearer "+token);
        return client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private long id(HttpResponse<String> response) throws Exception {assertEquals(200,response.statusCode());return json.readTree(response.body()).path("id").asLong();}
    private static String env(String name,String fallback) {String value=System.getenv(name);return value==null||value.isBlank()?fallback:value;}
}

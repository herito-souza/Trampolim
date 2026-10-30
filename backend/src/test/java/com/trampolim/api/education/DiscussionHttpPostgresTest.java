package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import java.net.*;import java.net.http.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.io.*;import java.util.*;import java.util.concurrent.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.trampolim.api.BackendApplication;

class DiscussionHttpPostgresTest {
    private final HttpClient client=HttpClient.newHttpClient();private final ObjectMapper json=new ObjectMapper();private String base;private final List<InputStream> streams=new ArrayList<>();
    private record Event(long id,JsonNode data){}
    @Test void privateSseRecoveryBatchAndMediaRevocation() throws Exception {
        Assumptions.assumeTrue(System.getenv("CAPACITA_FFMPEG")!=null&&System.getenv("CAPACITA_FFPROBE")!=null,"Configure FFmpeg/FFprobe para esta integração real.");
        String schema="discussion_http_"+UUID.randomUUID().toString().replace("-","");String url=env("CAPACITA_DB_URL","jdbc:postgresql://localhost:5434/trampolim_capacita_db");
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(url,env("CAPACITA_DB_USERNAME","trampolim_capacita"),env("CAPACITA_DB_PASSWORD","dev_capacita_only")));jdbc.execute("CREATE SCHEMA "+schema);Path media=Files.createTempDirectory("discussion-http-media-");
        try(var context=new SpringApplicationBuilder(BackendApplication.class).run("--server.port=0","--spring.jpa.hibernate.ddl-auto=validate","--app.mail.enabled=false","--capacita.questions.sse-poll-ms=200","--capacita.admin-emails=discussion-teacher@example.test","--capacita.media.root="+media,"--trampolim.datasource.capacita.url="+url+(url.contains("?")?"&":"?")+"currentSchema="+schema)) {
            base="http://127.0.0.1:"+context.getEnvironment().getRequiredProperty("local.server.port")+"/api/capacita";
            String teacher=register("discussion-teacher@example.test"),student=register("discussion-first@example.test"),second=register("discussion-second@example.test"),outsider=register("discussion-outsider@example.test");
            assertEquals(204,send("PUT","/learning/admin/roles","{\"email\":\"discussion-outsider@example.test\",\"role\":\"TEACHER\",\"granted\":true}",teacher).statusCode());
            long course=id(send("POST","/learning/courses","{\"title\":\"Discussion course\",\"category\":\"Math\",\"status\":\"PUBLISHED\"}",teacher));
            var liveTeacher=stream(teacher,0);var teacherReader=new BufferedReader(new InputStreamReader(liveTeacher,StandardCharsets.UTF_8));
            long first=create(student,course,"First private question",null);var firstEvent=event(teacherReader);assertEquals(first,firstEvent.data().path("items").get(0).path("question_id").asLong());liveTeacher.close();
            long next=create(second,course,"Offline question",null);var history=send("GET","/learning/notifications",null,teacher);assertEquals(2,json.readTree(history.body()).path("unreadCount").asInt());
            var reconnected=stream(teacher,firstEvent.id());var recovered=event(new BufferedReader(new InputStreamReader(reconnected,StandardCharsets.UTF_8)));assertTrue(recovered.id()>firstEvent.id());assertEquals(next,recovered.data().path("items").get(0).path("question_id").asLong());reconnected.close();
            assertEquals(403,send("GET","/learning/discussion/questions/"+first,null,second).statusCode());assertEquals(403,send("GET","/learning/discussion/questions/"+first,null,outsider).statusCode());
            var studentStream=stream(student,0);var studentReader=new BufferedReader(new InputStreamReader(studentStream,StandardCharsets.UTF_8));
            UUID key=UUID.randomUUID();String batch="{\"clientKey\":\""+key+"\",\"questionIds\":["+first+","+next+"],\"body\":\"General private answer\",\"confirmed\":true}";
            assertEquals(400,send("POST","/learning/discussion/batches",batch.replace("true","false"),teacher).statusCode());
            var sent=send("POST","/learning/discussion/batches",batch,teacher);assertEquals(200,sent.statusCode());var repeated=send("POST","/learning/discussion/batches",batch,teacher);assertEquals(json.readTree(sent.body()).path("id"),json.readTree(repeated.body()).path("id"));
            var reply=event(studentReader);for(JsonNode notice:reply.data().path("items"))assertEquals(first,notice.path("question_id").asLong());assertEquals("REPLY",reply.data().path("items").get(0).path("kind").asText());
            long notification=reply.data().path("items").get(0).path("id").asLong();assertEquals(404,send("PUT","/learning/notifications/"+notification+"/read",null,second).statusCode());assertEquals(204,send("PUT","/learning/notifications/"+notification+"/read",null,student).statusCode());
            assertEquals(0,json.readTree(send("GET","/learning/notifications",null,student).body()).path("unreadCount").asInt());
            byte[] audio=Files.readAllBytes(Path.of("../frontend/e2e/fixtures/question-audio.wav"));String boundary="question-"+UUID.randomUUID();var body=new ByteArrayOutputStream();body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8));body.write(audio);body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            var upload=client.send(HttpRequest.newBuilder(URI.create(base+"/learning/discussion/media?kind=AUDIO")).timeout(java.time.Duration.ofSeconds(60)).header("Authorization","Bearer "+student).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,upload.statusCode());UUID asset=UUID.fromString(json.readTree(upload.body()).path("id").asText());
            create(student,course,"Audio private question",asset);assertEquals(403,send("POST","/learning/discussion/media/"+asset+"/playback",null,second).statusCode());
            var playback=send("POST","/learning/discussion/media/"+asset+"/playback",null,student);assertEquals(200,playback.statusCode());String path=json.readTree(playback.body()).path("path").asText();
            String videoUrl=base.substring(0,base.length()-"/capacita".length())+path;var range=HttpRequest.newBuilder(URI.create(videoUrl)).timeout(java.time.Duration.ofSeconds(10)).header("Range","bytes=0-127").GET().build();var chunk=client.send(range,HttpResponse.BodyHandlers.ofByteArray());assertEquals(206,chunk.statusCode());assertEquals(128,chunk.body().length);
            assertEquals(204,send("POST","/session/logout",null,student).statusCode());assertEquals(404,client.send(range,HttpResponse.BodyHandlers.discarding()).statusCode());
            assertTrue(CompletableFuture.supplyAsync(()->{try{while(studentReader.readLine()!=null){}return true;}catch(IOException error){return true;}}).get(10,TimeUnit.SECONDS));
            assertEquals(403,send("GET","/learning/notifications",null,student).statusCode());
        } finally {for(var stream:streams)try{stream.close();}catch(IOException ignored){}jdbc.execute("DROP SCHEMA "+schema+" CASCADE");try(var paths=Files.walk(media)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
    private InputStream stream(String token,long cursor)throws Exception{var response=client.send(HttpRequest.newBuilder(URI.create(base+"/learning/notifications/stream?after="+cursor)).header("Authorization","Bearer "+token).header("Accept","text/event-stream").GET().build(),HttpResponse.BodyHandlers.ofInputStream());assertEquals(200,response.statusCode());streams.add(response.body());return response.body();}
    private Event event(BufferedReader reader)throws Exception{return CompletableFuture.supplyAsync(()->{try{long id=0;String kind="";StringBuilder data=new StringBuilder();String line;while((line=reader.readLine())!=null){if(line.startsWith("id:"))id=Long.parseLong(line.substring(3).trim());else if(line.startsWith("event:"))kind=line.substring(6).trim();else if(line.startsWith("data:"))data.append(line.substring(5).trim());else if(line.isBlank()){if(kind.equals("notifications")&&!data.isEmpty())return new Event(id,json.readTree(data.toString()));kind="";data.setLength(0);}}throw new IllegalStateException("SSE ended before notification");}catch(IOException error){throw new RuntimeException(error);}}).get(15,TimeUnit.SECONDS);}
    private long create(String token,long course,String title,UUID media)throws Exception{return id(send("POST","/learning/discussion/questions","{\"clientKey\":\""+UUID.randomUUID()+"\",\"courseId\":"+course+",\"title\":\""+title+"\",\"body\":\"Private question text\""+(media==null?"":",\"mediaId\":\""+media+"\"")+"}",token));}
    private String register(String email)throws Exception{var response=send("POST","/auth/register","{\"name\":\"Discussion fixture\",\"email\":\""+email+"\",\"password\":\"DiscussionFixturePassword!\",\"educationStage\":\"HIGH_SCHOOL\",\"ageBand\":\"AGE_18_PLUS\",\"termsAccepted\":true}",null);assertEquals(200,response.statusCode());return json.readTree(response.body()).path("token").asText();}
    private HttpResponse<String> send(String method,String path,String body,String token)throws Exception{var request=HttpRequest.newBuilder(URI.create(base+path)).timeout(java.time.Duration.ofSeconds(20)).header("Content-Type","application/json");if(token!=null)request.header("Authorization","Bearer "+token);return client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
    private long id(HttpResponse<String> response)throws Exception{assertEquals(200,response.statusCode());return json.readTree(response.body()).path("id").asLong();}
    private static String env(String name,String fallback){String value=System.getenv(name);return value==null||value.isBlank()?fallback:value;}
}

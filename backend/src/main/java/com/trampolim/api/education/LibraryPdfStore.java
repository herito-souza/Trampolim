package com.trampolim.api.education;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class LibraryPdfStore {
    @Value("${capacita.media.root:storage/capacita/videos}") private String root;
    @Value("${capacita.library.max-bytes:20971520}") private long maxBytes=20971520;
    @Value("${capacita.library.max-pages:300}") private int maxPages=300;
    @Value("${capacita.library.processing-seconds:45}") private int processingSeconds=45;
    private final Semaphore capacity=new Semaphore(2);
    public record Stored(UUID key,String sha256,long bytes,int pages,boolean hasText,boolean tagged) {}
    public Map<String,Object> limits(){return Map.of("maxBytes",maxBytes,"maxPages",maxPages);}
    Path directory()throws IOException{Path dir=Path.of(root).toAbsolutePath().normalize().resolve("library");Files.createDirectories(dir);return dir;}
    Path file(UUID key)throws IOException{return directory().resolve(key+".pdf");}
    String text(UUID key)throws IOException{return Files.readString(Path.of(file(key)+".txt"));}
    void delete(UUID key){try{Path file=file(key);for(String suffix:List.of("",".txt",".properties"))Files.deleteIfExists(Path.of(file+suffix));}catch(IOException ignored){/* Janitor retries orphan cleanup. */}}
    public Stored upload(MultipartFile file){
        if(file==null||file.isEmpty()||file.getSize()>maxBytes||!Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT).endsWith(".pdf"))bad("Envie um arquivo PDF dentro do limite permitido.");
        return process(file,null);
    }
    public Stored compose(String title,String content){
        if(content==null||content.isBlank()||content.length()>50000)bad("Escreva de 1 a 50.000 caracteres para gerar o PDF.");
        return process(null,title+"\n\n"+content);
    }
    private Stored process(MultipartFile upload,String content){
        if(!capacity.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Há documentos em validação. Tente novamente em instantes.");
        UUID key=UUID.randomUUID();Path source=null,generated=null;boolean success=false;
        try{
            Path dir=directory();source=Files.createTempFile(dir,"pdf-input-",".tmp");
            if(content!=null){Files.writeString(source,content);generated=Files.createTempFile(dir,"pdf-generated-",".pdf");worker("generate",source,generated);Files.delete(source);source=generated;generated=null;}
            else try(var input=upload.getInputStream();var output=Files.newOutputStream(source)){byte[] buffer=new byte[65536];long bytes=0;int read;while((read=input.read(buffer))!=-1){bytes+=read;if(bytes>maxBytes)bad("PDF acima do limite permitido.");output.write(buffer,0,read);}}
            if(Files.size(source)>maxBytes)bad("PDF acima do limite permitido.");
            try(var stream=Files.newInputStream(source)){if(!Arrays.equals(stream.readNBytes(5),"%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))bad("O arquivo não é um PDF válido.");}
            String sha;try(var input=Files.newInputStream(source)){var digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[65536];int read;while((read=input.read(buffer))!=-1)digest.update(buffer,0,read);sha=HexFormat.of().formatHex(digest.digest());}
            Path target=file(key);worker("inspect",source,target);if(Files.size(target)>maxBytes)bad("PDF normalizado acima do limite permitido.");
            var info=new Properties();try(var stream=Files.newInputStream(Path.of(target+".properties"))){info.load(stream);}
            var result=new Stored(key,sha,Files.size(target),Integer.parseInt(info.getProperty("pages")),Boolean.parseBoolean(info.getProperty("hasText")),Boolean.parseBoolean(info.getProperty("tagged")));success=true;return result;
        }catch(ResponseStatusException error){throw error;}catch(Exception error){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"PDF não aceito. Verifique o formato, a quantidade de páginas e remova senhas, scripts, formulários, anexos e ações externas. No editor, use caracteres latinos compatíveis.");}
        finally{capacity.release();if(!success)delete(key);for(Path path:new Path[]{source,generated})if(path!=null)try{Files.deleteIfExists(path);}catch(IOException ignored){}}
    }
    private void worker(String mode,Path input,Path output)throws Exception{
        var command=new ArrayList<String>();command.add(Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString());command.add("-Xmx192m");command.add("-Djava.awt.headless=true");
        File application=new ApplicationHome(PdfDocumentWorker.class).getSource();
        if(application!=null&&application.isFile()&&application.getName().endsWith(".jar")){
            command.add("-Dloader.main="+PdfDocumentWorker.class.getName());command.add("-cp");command.add(application.getAbsolutePath());command.add("org.springframework.boot.loader.launch.PropertiesLauncher");
        }else{command.add("-cp");command.add(System.getProperty("java.class.path"));command.add(PdfDocumentWorker.class.getName());}
        command.addAll(List.of(mode,input.toString(),output.toString(),String.valueOf(maxPages)));
        var process=new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try{if(!process.waitFor(processingSeconds,TimeUnit.SECONDS)){process.destroyForcibly().waitFor();bad("A validação excedeu o tempo permitido.");}if(process.exitValue()!=0)bad("PDF inválido, protegido ou com recursos ativos não permitidos. O editor aceita texto latino simples.");}
        finally{if(process.isAlive())process.destroyForcibly();}
    }
    private static void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}

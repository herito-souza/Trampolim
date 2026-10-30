package com.trampolim.api.education;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Private filesystem adapter. Only validated, normalized MP4 files are served by authorized endpoints. */
@Service
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class LessonMediaStore {
    @Value("${capacita.media.root:storage/capacita/videos}") private String root;
    @Value("${capacita.media.ffprobe:ffprobe}") private String ffprobe;
    @Value("${capacita.media.ffmpeg:ffmpeg}") private String ffmpeg;
    @Value("${capacita.media.max-bytes:134217728}") private long maxBytes;
    private final Semaphore processing=new Semaphore(2);
    public record Stored(UUID key,double duration,long bytes) {}
    public Stored store(MultipartFile file) {
        if(file==null||file.isEmpty()||file.getSize()>maxBytes) reject("Envie um vídeo de até 128 MB.");
        String name=Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if(!name.matches(".*\\.(mp4|mov|webm)$")) reject("Formatos permitidos: MP4, MOV e WebM.");
        if(!processing.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Há vídeos em processamento. Tente novamente em instantes.");
        Path input=null,output=null;boolean stored=false;
        try {
            Path directory=directory();input=Files.createTempFile(directory,"upload-",".bin");
            try(var stream=file.getInputStream();var target=Files.newOutputStream(input)) {
                byte[] buffer=new byte[65536];long copied=0;int read;
                while((read=stream.read(buffer))!=-1) {copied+=read;if(copied>maxBytes) reject("Vídeo acima do limite permitido.");target.write(buffer,0,read);}
            }
            var source=probe(input);
            double duration=source.path("format").path("duration").asDouble(-1);
            // Some browser-recorded WebM files have no duration index; normalize before deciding.
            if(Double.isFinite(duration)&&duration>0) validateDuration(duration);
            var streams=source.path("streams");boolean video=false;
            for(var stream:streams) if("video".equals(stream.path("codec_type").asText())) {
                if(video||stream.path("width").asInt()>3840||stream.path("height").asInt()>2160||stream.path("width").asInt()<1||stream.path("height").asInt()<1) reject("O vídeo deve ter uma única faixa de imagem, com resolução de até 3840 × 2160.");
                video=true;
            }
            if(!video) reject("O arquivo não contém uma faixa de vídeo válida.");
            UUID key=UUID.randomUUID();output=directory.resolve(key+".mp4");
            // Decode and re-encode the actual stream; remove attachments, metadata and unsupported browser codecs.
            run(List.of(ffmpeg,"-nostdin","-v","error","-xerror","-protocol_whitelist","file,pipe","-format_whitelist","mov,matroska,webm","-i",input.toString(),"-map","0:v:0","-map","0:a:0?","-map_metadata","-1","-map_chapters","-1","-t","301","-vf","scale=trunc(min(1920\\,iw)/2)*2:-2","-c:v","libx264","-preset","veryfast","-crf","26","-threads","2","-pix_fmt","yuv420p","-c:a","aac","-movflags","+faststart","-y",output.toString()),240);
            double actual=probe(output).path("format").path("duration").asDouble(-1);
            validateDuration(actual);
            long bytes=Files.size(output);if(bytes<1||bytes>maxBytes) reject("Vídeo processado acima do limite permitido.");
            stored=true;return new Stored(key,actual,bytes);
        } catch(ResponseStatusException exception) {throw exception;}
        catch(java.io.IOException exception) {throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"O processamento de vídeo não está disponível. Verifique o armazenamento e a instalação de FFmpeg/FFprobe.");}
        finally {remove(input);if(!stored) remove(output);processing.release();}
    }
    public static void validateDuration(double seconds) {
        if(!Double.isFinite(seconds)||seconds<180||seconds>300) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A videoaula deve ter entre 180 e 300 segundos (3 a 5 minutos).");
    }
    public Path file(UUID key) {try {return directory().resolve(key+".mp4");}catch(java.io.IOException error){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Armazenamento indisponível.");}}
    public void delete(UUID key) {if(key!=null) remove(file(key));}
    private Path directory() throws java.io.IOException {Path path=Path.of(root).toAbsolutePath().normalize();Files.createDirectories(path);return path;}
    private com.fasterxml.jackson.databind.JsonNode probe(Path path) throws java.io.IOException {
        return inspect(path,"mov,matroska,webm");
    }
    com.fasterxml.jackson.databind.JsonNode inspect(Path path,String formats) throws java.io.IOException {
        String json=run(List.of(ffprobe,"-v","error","-protocol_whitelist","file,pipe","-format_whitelist",formats,"-show_entries","format=duration:stream=codec_type,width,height","-of","json",path.toString()),20);
        return new ObjectMapper().readTree(json);
    }
    String run(List<String> arguments,int timeout) throws java.io.IOException {
        Path log=Files.createTempFile(directory(),"process-",".log");Process process=null;
        try {
            process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if(!process.waitFor(timeout,TimeUnit.SECONDS)) {process.destroyForcibly();reject("O processamento excedeu o tempo permitido. Reduza a resolução do vídeo.");}
            if(process.exitValue()!=0) reject("O arquivo não pôde ser validado ou convertido em vídeo seguro. Envie outro MP4, MOV ou WebM.");
            if(Files.size(log)>65536) reject("Arquivo de vídeo inválido.");
            return Files.readString(log);
        } catch(InterruptedException error) {Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Processamento interrompido.");}
        finally {if(process!=null&&process.isAlive()) process.destroyForcibly();remove(log);}
    }
    private void remove(Path path) {if(path!=null) try{Files.deleteIfExists(path);}catch(java.io.IOException ignored){/* Private orphan can be reconciled against media_key; never expose filesystem details. */}}
    private void reject(String message) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}

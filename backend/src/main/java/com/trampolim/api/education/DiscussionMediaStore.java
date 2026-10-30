package com.trampolim.api.education;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
@Service
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class DiscussionMediaStore {
    private final LessonMediaStore engine;private final Semaphore processing=new Semaphore(2);
    @Value("${capacita.media.root:storage/capacita/videos}") private String root;
    @Value("${capacita.media.ffmpeg:ffmpeg}") private String ffmpeg;
    @Value("${capacita.questions.media-max-bytes:26214400}") private long maximumBytes=26214400;
    @Value("${capacita.questions.audio-max-seconds:120}") private int audioSeconds=120;
    @Value("${capacita.questions.video-max-seconds:120}") private int videoSeconds=120;
    public record Stored(UUID key,String kind,double duration,long bytes,String mediaType){}
    public DiscussionMediaStore(LessonMediaStore engine){this.engine=engine;}
    public Map<String,Object> limits(){return Map.of("maxBytes",maximumBytes,"audioMaxSeconds",audioSeconds,"videoMaxSeconds",videoSeconds);}
    public Stored store(MultipartFile file,String kind) {
        if(kind==null||!Set.of("AUDIO","VIDEO").contains(kind))bad("Escolha áudio ou vídeo.");
        if(file==null||file.isEmpty()||file.getSize()>maximumBytes)bad("Mídia vazia ou acima do limite de tamanho.");
        String name=Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if(!name.matches(".*\\.(mp4|mov|webm|m4a|mp3|wav|ogg|aac|flac)$"))bad("Formato não permitido. Envie MP4, MOV, WebM, M4A, MP3, WAV, OGG, AAC ou FLAC.");
        if(!processing.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Há mídias em processamento. Tente novamente em instantes.");
        Path input=null,output=null;boolean success=false;
        try {
            Files.createDirectories(directory());input=Files.createTempFile(directory(),"incoming-",".bin");
            try(var source=file.getInputStream();var target=Files.newOutputStream(input)){byte[] buffer=new byte[65536];int n;long size=0;while((n=source.read(buffer))!=-1){size+=n;if(size>maximumBytes)bad("Mídia acima do limite de tamanho.");target.write(buffer,0,n);}}
            String formats="mov,matroska,webm,mp3,wav,ogg,flac,aac";var info=engine.inspect(input,formats);boolean video=false,audio=false;
            for(var stream:info.path("streams")){if("video".equals(stream.path("codec_type").asText())){if(stream.path("width").asInt()>3840||stream.path("height").asInt()>2160)bad("Resolução acima de 3840 × 2160.");video=true;}if("audio".equals(stream.path("codec_type").asText()))audio=true;}
            if("VIDEO".equals(kind)&&!video||"AUDIO".equals(kind)&&(!audio||video))bad("O arquivo não corresponde ao tipo de comunicação selecionado.");
            int max="AUDIO".equals(kind)?audioSeconds:videoSeconds;double advertised=info.path("format").path("duration").asDouble(-1);if(advertised>max)bad("A gravação deve ter no máximo "+max+" segundos.");
            UUID key=UUID.randomUUID();output=file(key,kind);
            var args=new ArrayList<>(List.of(ffmpeg,"-nostdin","-v","error","-xerror","-protocol_whitelist","file,pipe","-format_whitelist",formats,"-i",input.toString(),"-map_metadata","-1","-map_chapters","-1","-t",Integer.toString(max+1)));
            if("VIDEO".equals(kind))args.addAll(List.of("-map","0:v:0","-map","0:a:0?","-vf","scale=trunc(min(1280\\,iw)/2)*2:-2","-c:v","libx264","-preset","veryfast","-crf","28","-threads","2","-pix_fmt","yuv420p","-c:a","aac"));
            else args.addAll(List.of("-map","0:a:0","-vn","-c:a","aac","-b:a","96k"));
            args.addAll(List.of("-movflags","+faststart","-y",output.toString()));engine.run(args,180);
            double actual=engine.inspect(output,"mov").path("format").path("duration").asDouble(-1);long bytes=Files.size(output);
            if(!Double.isFinite(actual)||actual<=0||actual>max||bytes<=0||bytes>maximumBytes)bad("Mídia inválida ou acima dos limites de duração/tamanho.");
            success=true;return new Stored(key,kind,actual,bytes,"AUDIO".equals(kind)?"audio/mp4":"video/mp4");
        }catch(ResponseStatusException error){throw error;}catch(java.io.IOException error){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Não foi possível processar a mídia. Verifique o armazenamento e FFmpeg/FFprobe.");}
        finally{remove(input);if(!success)remove(output);processing.release();}
    }
    Path directory(){return Path.of(root).toAbsolutePath().normalize().resolve("discussion");}
    public Path file(UUID key,String kind){return directory().resolve(key+("AUDIO".equals(kind)?".m4a":".mp4"));}
    public void delete(UUID key,String kind){remove(file(key,kind));}
    private void remove(Path path){if(path!=null)try{Files.deleteIfExists(path);}catch(java.io.IOException ignored){/* Private orphan reconciled by maintenance. */}}
    private void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}

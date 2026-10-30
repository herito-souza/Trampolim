package com.trampolim.api.education;

import java.io.IOException;
import java.util.*;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/capacita/learning/library")
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class LibraryController {
    private final LibraryService service;private final LibraryPdfStore store;private final LearningAccess access;private final JdbcTemplate jdbc;private final TransactionTemplate tx;
    @Value("${capacita.library.deleted-retention-days:30}")private int retentionDays=30;
    public LibraryController(LibraryService service,LibraryPdfStore store,LearningAccess access,@Qualifier("capacitaJdbcTemplate")JdbcTemplate jdbc,@Qualifier("capacitaTransactionManager")PlatformTransactionManager manager){this.service=service;this.store=store;this.access=access;this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);}
    public record Composition(String content,int revision){}
    @GetMapping("/config")public Map<String,Object> config(@AuthenticationPrincipal String email){access.account(email);return store.limits();}
    @GetMapping("/materials")public List<Map<String,Object>> list(@AuthenticationPrincipal String email,@RequestParam(defaultValue="false")boolean manage,@RequestParam(required=false)Long course,@RequestParam(required=false)Long subject,@RequestParam(required=false)Long module,@RequestParam(required=false)Long lesson,@RequestParam(required=false)String search,@RequestParam(required=false)String category,@RequestParam(required=false)Long teacher,@RequestParam(required=false)Long before){return service.list(email,manage,course,subject,module,lesson,search,category,teacher,before);}
    @GetMapping("/materials/{id}")public Map<String,Object> detail(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(defaultValue="false")boolean manage){return service.detail(email,id,manage);}
    @PostMapping("/materials")public Map<String,Object> create(@AuthenticationPrincipal String email,@RequestBody LibraryService.MaterialInput input){return service.create(email,input);}
    @PutMapping("/materials/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void edit(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody LibraryService.MaterialInput input){service.edit(email,id,input);}
    @DeleteMapping("/materials/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@AuthenticationPrincipal String email,@PathVariable long id){service.delete(email,id);}
    @PostMapping(value="/materials/{id}/versions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)public Map<String,Object> upload(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam int revision,@RequestParam MultipartFile file){service.reserve(email,id);return install(email,id,revision,store.upload(file));}
    @PostMapping("/materials/{id}/compose")public Map<String,Object> compose(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody Composition input){service.reserve(email,id);String title=(String)service.detail(email,id,true).get("title");return install(email,id,input.revision(),store.compose(title,input.content()));}
    private Map<String,Object> install(String email,long id,int revision,LibraryPdfStore.Stored stored){boolean installed=false;try{var result=service.install(email,id,stored,revision);installed=!Boolean.TRUE.equals(result.get("duplicate"));return result;}finally{if(!installed)store.delete(stored.key());}}
    @GetMapping("/materials/{id}/file")public ResponseEntity<Resource> file(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(required=false)Long version,@RequestParam(defaultValue="false")boolean download)throws IOException{
        var row=service.version(email,id,version,download);Path path=store.file((UUID)row.get("storage_key"));if(!Files.isRegularFile(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"PDF indisponível no armazenamento.");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'").header(HttpHeaders.CONTENT_DISPOSITION,(download?ContentDisposition.attachment():ContentDisposition.inline()).filename("material-"+id+"-v"+row.get("version_number")+".pdf").build().toString()).body(new FileSystemResource(path));
    }
    @GetMapping("/materials/{id}/text")public Map<String,Object> text(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(required=false)Long version)throws IOException{var row=service.version(email,id,version,false);return Map.of("alternativeText",row.get("alternative_text"),"extractedText",store.text((UUID)row.get("storage_key")),"extractionVerified",false);}
    @Scheduled(fixedDelayString="${capacita.library.cleanup-ms:3600000}")public void cleanup(){try{
        if(retentionDays>0)tx.executeWithoutResult(status->{var rows=jdbc.queryForList("SELECT id FROM learning_materials WHERE status='DELETED' AND deleted_at<CURRENT_TIMESTAMP-(? * INTERVAL '1 day') LIMIT 100 FOR UPDATE SKIP LOCKED",retentionDays);for(var row:rows){jdbc.update("UPDATE learning_materials SET current_version_id=NULL WHERE id=?",row.get("id"));jdbc.update("DELETE FROM learning_materials WHERE id=?",row.get("id"));}});
        Path dir=store.directory();try(var paths=Files.list(dir)){for(Path path:paths.limit(2000).toList()){
            if(!Files.isRegularFile(path)||Files.getLastModifiedTime(path).toInstant().plusSeconds(86400).isAfter(java.time.Instant.now()))continue;String name=path.getFileName().toString();
            if(name.matches("[0-9a-f-]{36}\\.pdf(\\.txt|\\.properties)?")){UUID key=UUID.fromString(name.substring(0,36));if(jdbc.queryForObject("SELECT count(*) FROM learning_material_versions WHERE storage_key=?",Integer.class,key)==0)Files.deleteIfExists(path);}
            else if(name.startsWith("pdf-input-")||name.startsWith("pdf-generated-"))Files.deleteIfExists(path);
        }}
    }catch(IOException|org.springframework.dao.DataAccessException ignored){/* Preserve private files and retry maintenance later. */}}
}

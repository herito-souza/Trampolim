package com.trampolim.api.hub;
import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController
@RequestMapping("/api/public/hub")
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class HubPublicController {
 private final HubService service;public HubPublicController(HubService service){this.service=service;}
 @GetMapping("/organizations")public Map<String,Object> list(@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit,@RequestParam(defaultValue="")String q){return service.publicOrganizations(q,before,limit);}
 @GetMapping("/organizations/{id}")public Map<String,Object> detail(@PathVariable long id){return service.publicOrganization(id);}
 @GetMapping("/pages/{slug}")public Map<String,Object> page(@PathVariable String slug){return service.publicPage(slug);}
 @GetMapping("/organizations/{id}/logo")public ResponseEntity<byte[]> logo(@PathVariable long id){service.publicOrganization(id);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").body(service.logo(id));}
 @GetMapping("/programs")public Map<String,Object> programs(@RequestParam(defaultValue="9223372036854775807")long before,@RequestParam(defaultValue="20")int limit){return service.publicPrograms(before,limit);}
 @GetMapping("/programs/{id}")public Map<String,Object> program(@PathVariable long id){return service.publicProgram(id);}
}

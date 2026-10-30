package com.trampolim.api.radar;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController
public class SkillCatalogController {
 private final SkillCatalog skills;
 public SkillCatalogController(SkillCatalog skills){this.skills=skills;}
 @GetMapping("/api/public/skills")public Map<String,Object> list(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String after,@RequestParam(defaultValue="40")int limit){return Map.of("items",skills.list(q,after,limit),"levels",List.of("1 — Inicial","2 — Básico aplicado","3 — Intermediário/autônomo","4 — Avançado"));}
 @GetMapping("/api/public/skills/resolve")public List<Map<String,Object>> resolve(@RequestParam String codes){return skills.resolve(Arrays.asList(codes.split(",",-1)));}
 @PutMapping("/api/radar/admin/skills")@ResponseStatus(HttpStatus.NO_CONTENT)public void save(@AuthenticationPrincipal String email,@RequestBody SkillCatalog.Input input){skills.save(email,input);}
}

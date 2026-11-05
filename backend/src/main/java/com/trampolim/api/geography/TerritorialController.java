package com.trampolim.api.geography;
import java.util.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/public/geography")
public class TerritorialController {
 private final TerritorialCatalog catalog;public TerritorialController(TerritorialCatalog catalog){this.catalog=catalog;}
 @GetMapping("/settings")public Map<String,Object> settings(){return catalog.settings();}
 @GetMapping("/municipalities")public List<TerritorialCatalog.Municipality> municipalities(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String state,@RequestParam(defaultValue="30")int limit){return catalog.search(q,state,limit);}
}

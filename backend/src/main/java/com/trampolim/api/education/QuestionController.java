package com.trampolim.api.education;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/capacita/learning/discussion")
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class QuestionController {
    private final QuestionService service;
    public QuestionController(QuestionService service){this.service=service;}
    @GetMapping("/questions") public List<Map<String,Object>> list(@AuthenticationPrincipal String email,@RequestParam(defaultValue="false")boolean inbox,@RequestParam(required=false)Long course,@RequestParam(required=false)Long module,@RequestParam(required=false)Long lesson,@RequestParam(required=false)String status,@RequestParam(defaultValue="NEWEST")String order,@RequestParam(defaultValue="0")int offset){return service.list(email,inbox,course,module,lesson,status,order,offset);}
    @PostMapping("/questions") public Map<String,Object> create(@AuthenticationPrincipal String email,@RequestBody QuestionService.QuestionInput input){return service.create(email,input);}
    @GetMapping("/questions/{id}") public Map<String,Object> detail(@AuthenticationPrincipal String email,@PathVariable long id){return service.detail(email,id);}
    @GetMapping("/questions/{id}/messages") public List<Map<String,Object>> messages(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(required=false)Long before){return service.messages(email,id,before);}
    @PostMapping("/questions/{id}/messages") public Map<String,Object> followUp(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody QuestionService.MessageInput input){return service.send(email,id,input,false);}
    @PostMapping("/questions/{id}/reply") public Map<String,Object> reply(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody QuestionService.MessageInput input){return service.send(email,id,input,true);}
    public record Status(String status){}
    @PutMapping("/questions/{id}/status") @ResponseStatus(HttpStatus.NO_CONTENT) public void status(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody Status input){service.status(email,id,input==null?"":input.status());}
    @DeleteMapping("/questions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal String email,@PathVariable long id){service.delete(email,id);}
    @PostMapping("/batches") public QuestionService.BatchResult batch(@AuthenticationPrincipal String email,@RequestBody QuestionService.BatchInput input){return service.batch(email,input);}
    @GetMapping("/batches") public List<QuestionService.BatchResult> batches(@AuthenticationPrincipal String email){return service.batches(email);}
}

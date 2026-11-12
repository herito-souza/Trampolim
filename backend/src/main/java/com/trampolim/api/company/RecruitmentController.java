package com.trampolim.api.company;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company/recruitment")
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class RecruitmentController {
    private final RecruitmentService service;
    public RecruitmentController(RecruitmentService service){this.service=service;}
    @GetMapping("/stages")
    public java.util.List<RecruitmentService.Stage> stages(@AuthenticationPrincipal String email){return service.stages(email);}
    @GetMapping("/applications")
    public RecruitmentService.Page<RecruitmentService.Application> applications(@AuthenticationPrincipal String email,
        @RequestParam(required=false) Long listingId,@RequestParam(required=false) String status,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return service.applications(email,listingId,status,page,size);}
    @GetMapping("/applications/{id}")
    public RecruitmentService.Application application(@AuthenticationPrincipal String email,@PathVariable long id){return service.application(email,id);}
    @GetMapping("/indicators")
    public RecruitmentService.Indicators indicators(@AuthenticationPrincipal String email,@RequestParam(required=false) Long listingId,@RequestParam(required=false) String status){return service.indicators(email,listingId,status);}
    @PutMapping("/applications/{id}/stage")
    public RecruitmentService.Mutation stage(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody RecruitmentService.Change input){return service.change(email,id,input);}
    @GetMapping("/applications/{id}/history")
    public RecruitmentService.Page<RecruitmentService.Event> history(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return service.history(email,id,page,size);}
    @GetMapping("/applications/{id}/notes")
    public RecruitmentService.Page<RecruitmentService.InternalNote> notes(@AuthenticationPrincipal String email,@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return service.notes(email,id,page,size);}
    @PostMapping("/applications/{id}/notes")
    public RecruitmentService.Mutation note(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody RecruitmentService.Note input){return service.addNote(email,id,input);}
    @DeleteMapping("/applications/{id}/notes/{note}")
    public RecruitmentService.Mutation remove(@AuthenticationPrincipal String email,@PathVariable long id,@PathVariable UUID note,@RequestParam long expectedRevision,@RequestParam UUID requestKey){return service.removeNote(email,id,note,new RecruitmentService.Remove(expectedRevision,requestKey));}
}

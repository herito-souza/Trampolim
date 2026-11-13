package com.trampolim.api.career;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Authenticated operations for the Oportunidades mentoring context. */
@RestController
@RequestMapping("/api/career/mentoring")
public class CareerMentorshipController {
    private final CareerMentorshipService service;

    public CareerMentorshipController(CareerMentorshipService service) { this.service = service; }

    @GetMapping("/me") public Map<String,Object> me(@AuthenticationPrincipal String email) { return service.me(email); }
    @PutMapping("/mentor-profile") public Map<String,Object> saveMentor(@AuthenticationPrincipal String email, @RequestBody CareerMentorshipService.MentorInput input) { return service.saveMentor(email,input); }
    @DeleteMapping("/mentor-profile") @ResponseStatus(HttpStatus.NO_CONTENT) public void withdrawMentor(@AuthenticationPrincipal String email) { service.withdrawMentor(email); }
    @PostMapping("/availability") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> addSlot(@AuthenticationPrincipal String email, @RequestBody CareerMentorshipService.SlotInput input) { return service.addSlot(email,input); }
    @GetMapping("/availability/mine") public List<Map<String,Object>> mySlots(@AuthenticationPrincipal String email) { return service.mySlots(email); }
    @DeleteMapping("/availability/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void closeSlot(@AuthenticationPrincipal String email,@PathVariable long id) { service.closeSlot(email,id); }
    @PostMapping("/sessions") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> book(@AuthenticationPrincipal String email,@RequestBody CareerMentorshipService.BookingInput input) { return service.book(email,input); }
    @GetMapping("/sessions") public List<Map<String,Object>> sessions(@AuthenticationPrincipal String email) { return service.sessions(email); }
    @GetMapping("/sessions/{id}/history") public List<Map<String,Object>> history(@AuthenticationPrincipal String email,@PathVariable UUID id) { return service.history(email,id); }
    @PostMapping("/sessions/{id}/confirm") public Map<String,Object> confirm(@AuthenticationPrincipal String email,@PathVariable UUID id) { return service.confirm(email,id,true); }
    @PostMapping("/sessions/{id}/decline") public Map<String,Object> decline(@AuthenticationPrincipal String email,@PathVariable UUID id) { return service.confirm(email,id,false); }
    @PostMapping("/sessions/{id}/cancel") public Map<String,Object> cancel(@AuthenticationPrincipal String email,@PathVariable UUID id) { return service.cancel(email,id); }
    @PostMapping("/sessions/{id}/complete") public Map<String,Object> complete(@AuthenticationPrincipal String email,@PathVariable UUID id) { return service.complete(email,id); }
    @PutMapping("/sessions/{id}/details") public Map<String,Object> meetingDetails(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody CareerMentorshipService.SessionDetails input) { return service.meetingDetails(email,id,input); }
    @PutMapping("/sessions/{id}/rating") public Map<String,Object> rate(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody CareerMentorshipService.RatingInput input) { return service.rate(email,id,input); }
    @PostMapping("/reports") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> report(@AuthenticationPrincipal String email,@RequestBody CareerMentorshipService.ReportInput input) { return service.report(email,input); }

    @GetMapping("/admin/mentors") public List<Map<String,Object>> mentors(@AuthenticationPrincipal String email) { return service.adminMentors(email); }
    @PutMapping("/admin/mentors/{id}/review") public Map<String,Object> reviewMentor(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody CareerMentorshipService.MentorDecision input) { return service.reviewMentor(email,id,input); }
    @GetMapping("/admin/reports") public List<Map<String,Object>> reports(@AuthenticationPrincipal String email) { return service.adminReports(email); }
    @PutMapping("/admin/reports/{id}") public Map<String,Object> reviewReport(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody CareerMentorshipService.ReportDecision input) { return service.reviewReport(email,id,input); }
    @GetMapping("/admin/metrics") public Map<String,Object> metrics(@AuthenticationPrincipal String email) { return service.metrics(email); }
}

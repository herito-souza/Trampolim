package com.trampolim.api.career;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public catalog exposes approved mentor profiles only; scheduling still requires an Oportunidades account. */
@RestController
@RequestMapping("/api/public/career/mentoring")
public class CareerMentorshipPublicController {
    private final CareerMentorshipService service;

    public CareerMentorshipPublicController(CareerMentorshipService service) { this.service=service; }

    @GetMapping("/mentors")
    public ResponseEntity<Map<String,Object>> mentors(@RequestParam(defaultValue="") String specialty,
            @RequestParam(defaultValue="") String before,@RequestParam(defaultValue="20") int limit) {
        UUID cursor=null;
        if(!before.isBlank()) try { cursor=UUID.fromString(before); }
        catch(IllegalArgumentException invalid) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Cursor inválido."); }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.mentors(specialty,cursor,limit));
    }

    @GetMapping("/mentors/{id}/availability")
    public ResponseEntity<?> slots(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publicSlots(id));
    }
}

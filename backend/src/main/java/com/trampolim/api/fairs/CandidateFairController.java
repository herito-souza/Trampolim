package com.trampolim.api.fairs;
import org.springframework.web.bind.annotation.*;import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/job-fairs")
public class CandidateFairController extends FairRegistrationApi {public CandidateFairController(@Qualifier("candidateFairRegistrations")FairRegistrationStore store){super(store);}}

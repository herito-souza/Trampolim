package com.trampolim.api.fairs;
import org.springframework.web.bind.annotation.*;import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/capacita/job-fairs") @ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class StudentFairController extends FairRegistrationApi {public StudentFairController(@Qualifier("studentFairRegistrations")FairRegistrationStore store){super(store);}}

package com.trampolim.api.trust;
import org.springframework.web.bind.annotation.*;import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/opportunities/company-trust")
public class CandidateReportController extends ReportApi {public CandidateReportController(@Qualifier("candidateListingReports")ListingReportGateway service){super(service);}}

package com.trampolim.api.trust;
import org.springframework.web.bind.annotation.*;import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/capacita/opportunities/company-trust") @ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class StudentReportController extends ReportApi {public StudentReportController(@Qualifier("studentListingReports")ListingReportGateway service){super(service);}}

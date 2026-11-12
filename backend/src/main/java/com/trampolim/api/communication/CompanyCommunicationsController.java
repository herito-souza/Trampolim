package com.trampolim.api.communication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/empresas/communications")
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class CompanyCommunicationsController extends CommunicationApi {public CompanyCommunicationsController(@Qualifier("companyCommunications")CommunicationStore store){super(store);}}

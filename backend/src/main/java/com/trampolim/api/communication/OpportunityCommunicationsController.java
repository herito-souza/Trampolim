package com.trampolim.api.communication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/communications")

public class OpportunityCommunicationsController extends CommunicationApi {public OpportunityCommunicationsController(@Qualifier("opportunitiesCommunications")CommunicationStore store){super(store);}}

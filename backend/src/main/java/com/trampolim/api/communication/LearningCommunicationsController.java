package com.trampolim.api.communication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/capacita/communications")
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class LearningCommunicationsController extends CommunicationApi {public LearningCommunicationsController(@Qualifier("learningCommunications")CommunicationStore store){super(store);}}

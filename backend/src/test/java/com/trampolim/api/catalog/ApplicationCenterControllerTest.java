package com.trampolim.api.catalog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.trampolim.api.radar.RadarBridge;
import com.trampolim.api.user.AppUser;
import com.trampolim.api.user.AppUserRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ApplicationCenterControllerTest {
    @Test void onlyVerifiedPersonalAccountCanRequestLookupForItsOwnEmail(){
        var users=mock(AppUserRepository.class);var bridge=mock(RadarBridge.class);var user=new AppUser();user.setEmail("verified@example.test");user.setEmailVerified(true);user.setAccountType("PERSON");when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var json=new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("items",0);when(bridge.post(eq("/integrations/application-center/query"),any())).thenReturn(json);
        var controller=new ApplicationCenterController(users,bridge);var result=controller.list(user.getEmail(),new ApplicationCenterController.Filter(null,null,null,null,null,0,10,true));assertSame(json,result);
        verify(bridge).configured();verify(bridge).post(eq("/integrations/application-center/query"),argThat(body->body instanceof Map<?,?> map&&user.getEmail().equals(map.get("verifiedEmail"))&&!map.containsKey("email")));
    }
    @Test void consentAndVerifiedIdentityAreRequiredBeforeCrossTrailCall(){
        var users=mock(AppUserRepository.class);var bridge=mock(RadarBridge.class);var user=new AppUser();user.setEmail("candidate@example.test");user.setEmailVerified(true);user.setAccountType("PERSON");when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));var controller=new ApplicationCenterController(users,bridge);
        assertEquals(400,assertThrows(ResponseStatusException.class,()->controller.list(user.getEmail(),new ApplicationCenterController.Filter(null,null,null,null,null,0,10,false))).getStatusCode().value());
        user.setEmailVerified(false);assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.list(user.getEmail(),new ApplicationCenterController.Filter(null,null,null,null,null,0,10,true))).getStatusCode().value());
        user.setEmailVerified(true);user.setAccountType("COMPANY");assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.list(user.getEmail(),new ApplicationCenterController.Filter(null,null,null,null,null,0,10,true))).getStatusCode().value());
        verifyNoInteractions(bridge);
    }
    @Test void existingCandidateApplicationNoticeOpensTheCentral(){
        assertEquals("#oportunidades/applications",com.trampolim.api.communication.CommunicationTemplates.render("OPORTUNIDADES","APPLICATION_UPDATED").target());
    }
}

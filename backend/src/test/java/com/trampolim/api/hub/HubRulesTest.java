package com.trampolim.api.hub;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.*;
class HubRulesTest {
 HubRules.Profile profile(String color){return new HubRules.Profile("Institution","Legal ID","hub@example.test","000","Responsible","association-test","Learning","Text",color,true,0,"Institution");}
 @Test void brandRequiresReadableContrast(){assertTrue(HubRules.contrast("#9bc3e5","#0b1421")>=4.5);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->HubRules.profile(profile("#101010")));}
 @Test void identifiersAndContactsAreValidated(){assertDoesNotThrow(()->HubRules.profile(profile("#9bc3e5")));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->HubRules.profile(new HubRules.Profile("","","bad","","","../file","","","#9bc3e5",true,0,"")));}
 @Test void periodsAndScopesAreBounded(){assertThrows(org.springframework.web.server.ResponseStatusException.class,()->HubRules.dates("2026-01-01","2030-01-01"));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->HubRules.courses(List.of(1L,1L)));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->HubRules.page(1,100));}
}

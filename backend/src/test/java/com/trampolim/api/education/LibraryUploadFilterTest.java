package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;

class LibraryUploadFilterTest {
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
    @SuppressWarnings("unchecked")@Test void anonymousIsRefusedBeforeParsing()throws Exception{var provider=(ObjectProvider<LibraryService>)mock(ObjectProvider.class);var filter=new LibraryUploadFilter(provider,20);var request=new MockHttpServletRequest("POST","/api/capacita/learning/library/materials/42/versions");var response=new MockHttpServletResponse();filter.doFilter(request,response,(req,res)->fail("Must not parse an anonymous body"));assertEquals(401,response.getStatus());verifyNoInteractions(provider);}
    @SuppressWarnings("unchecked")@Test void lengthAndParallelLimitsApplyBeforeParsing()throws Exception{var provider=(ObjectProvider<LibraryService>)mock(ObjectProvider.class);var service=mock(LibraryService.class);when(provider.getIfAvailable()).thenReturn(service);when(service.detail("teacher@example.test",42,true)).thenReturn(Map.of());SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("teacher@example.test",null,List.of()));var filter=new LibraryUploadFilter(provider,20);var request=new MockHttpServletRequest("POST","/api/capacita/learning/library/materials/42/versions");var response=new MockHttpServletResponse();filter.doFilter(request,response,(req,res)->fail("Unknown length must not be parsed"));assertEquals(411,response.getStatus());request.setContent(new byte[1048597]);response=new MockHttpServletResponse();filter.doFilter(request,response,(req,res)->fail("Oversize body must not be parsed"));assertEquals(413,response.getStatus());request.setContent(new byte[10]);filter.doFilter(request,new MockHttpServletResponse(),(req,res)->{var nested=new MockHttpServletResponse();var parallel=new MockHttpServletRequest("POST","/api/capacita/learning/library/materials/42/versions");parallel.setContent(new byte[10]);filter.doFilter(parallel,nested,(nextReq,nextRes)->fail("Parallel request must not be parsed"));assertEquals(429,nested.getStatus());});var next=new MockHttpServletResponse();filter.doFilter(request,next,(req,res)->((jakarta.servlet.http.HttpServletResponse)res).setStatus(204));assertEquals(204,next.getStatus());verify(service,atLeastOnce()).detail("teacher@example.test",42,true);}
}

package com.trampolim.api.education;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class LearningUploadFilterTest {
    @AfterEach void cleanup() {SecurityContextHolder.clearContext();}
    @SuppressWarnings("unchecked")
    @Test void refusesAnonymousBeforeReadingMultipartBody() throws Exception {
        var access=(ObjectProvider<LearningAccess>)mock(ObjectProvider.class);
        var filter=new LearningUploadFilter(access);var request=new MockHttpServletRequest("POST","/api/capacita/learning/lessons/1/video");var response=new MockHttpServletResponse();
        filter.doFilter(request,response,(req,res)->fail("An anonymous upload must not reach multipart parsing"));
        assertEquals(401,response.getStatus());verifyNoInteractions(access);
    }
    @SuppressWarnings("unchecked")
    @Test void onlyOneUploadPerAccountCanReachMultipartParsing() throws Exception {
        var provider=(ObjectProvider<LearningAccess>)mock(ObjectProvider.class);var access=mock(LearningAccess.class);when(provider.getIfAvailable()).thenReturn(access);
        when(access.teacher("teacher@example.test")).thenReturn(new LearningAccess.Account(7,"Teacher","teacher@example.test",true,false));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("teacher@example.test",null,java.util.List.of()));
        var filter=new LearningUploadFilter(provider);var first=new MockHttpServletRequest("POST","/api/capacita/learning/lessons/1/video");var firstResponse=new MockHttpServletResponse();
        filter.doFilter(first,firstResponse,(req,res)->{
            var second=new MockHttpServletRequest("POST","/api/capacita/learning/lessons/1/video");var response=new MockHttpServletResponse();
            filter.doFilter(second,response,(nestedReq,nestedRes)->fail("Parallel body must not be parsed"));assertEquals(429,response.getStatus());
        });
        var next=new MockHttpServletRequest("POST","/api/capacita/learning/lessons/1/video");var response=new MockHttpServletResponse();
        filter.doFilter(next,response,(req,res)->((jakarta.servlet.http.HttpServletResponse)res).setStatus(204));assertEquals(204,response.getStatus());
    }
}

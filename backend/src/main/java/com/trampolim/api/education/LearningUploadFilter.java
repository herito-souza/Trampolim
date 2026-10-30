package com.trampolim.api.education;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

/** Reject unauthorized/excess parallel uploads before Spring parses and spools multipart bodies. */
public class LearningUploadFilter extends OncePerRequestFilter {
    private final ObjectProvider<LearningAccess> access;
    private final long discussionLimit;
    private final Set<Long> active=ConcurrentHashMap.newKeySet();private final Semaphore capacity=new Semaphore(2);
    public LearningUploadFilter(ObjectProvider<LearningAccess> access) {this(access,26214400);}
    public LearningUploadFilter(ObjectProvider<LearningAccess> access,long discussionLimit) {this.access=access;this.discussionLimit=discussionLimit;}
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {return !"POST".equals(request.getMethod())||!(request.getRequestURI().matches("/api/capacita/learning/lessons/[0-9]+/video")||request.getRequestURI().equals("/api/capacita/learning/discussion/media"));}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        Long user=null;boolean acquired=false;
        try {
            var authentication=SecurityContextHolder.getContext().getAuthentication();
            if(authentication==null||!authentication.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            var service=access.getIfAvailable();if(service==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            boolean discussion=request.getRequestURI().equals("/api/capacita/learning/discussion/media");
            var teacher=discussion?service.account(authentication.getName()):service.teacher(authentication.getName());
            if(!discussion){long lesson;try {lesson=Long.parseLong(request.getRequestURI().split("/")[5]);}catch(NumberFormatException invalid) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST);}service.ownLesson(teacher,lesson);}
            if(discussion&&request.getContentLengthLong()<0) throw new ResponseStatusException(HttpStatus.LENGTH_REQUIRED);
            if(request.getContentLengthLong()>(discussion?discussionLimit+1048576:130L*1024*1024)) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
            if(!active.add(teacher.id())) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
            user=teacher.id();
            if(!capacity.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
            acquired=true;
        } catch(ResponseStatusException error) {
            if(user!=null) active.remove(user);if(acquired) capacity.release();
            response.setStatus(error.getStatusCode().value());response.setContentType("application/json");response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Envio indisponível. Verifique sua permissão, o limite de tamanho e se há outro vídeo em processamento.\"}");return;
        } catch(org.springframework.dao.DataAccessException error) {
            response.setStatus(503);response.setContentType("application/json");response.getWriter().write("{\"error\":\"Serviço educacional temporariamente indisponível.\"}");return;
        }
        try {chain.doFilter(request,response);} finally {if(user!=null) active.remove(user);if(acquired) capacity.release();}
    }
}

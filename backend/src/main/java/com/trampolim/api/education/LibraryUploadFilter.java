package com.trampolim.api.education;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

/** Authenticate/authorize and bound uploads before multipart data can be spooled by MVC. */
public class LibraryUploadFilter extends OncePerRequestFilter {
    private final ObjectProvider<LibraryService> services;private final long maxBytes;private final Semaphore capacity=new Semaphore(2);private final Set<String> active=ConcurrentHashMap.newKeySet();
    public LibraryUploadFilter(ObjectProvider<LibraryService> services,long maxBytes){this.services=services;this.maxBytes=maxBytes;}
    @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getMethod().equals("POST")||!request.getRequestURI().matches("/api/capacita/learning/library/materials/[0-9]+/(versions|compose)");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String user=null;boolean acquired=false;int error=0;
        try{var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null||!auth.isAuthenticated())error=401;
            else{var service=services.getIfAvailable();if(service==null)error=503;else{long id=Long.parseLong(request.getRequestURI().split("/")[6]);service.detail(auth.getName(),id,true);long size=request.getContentLengthLong();long limit=request.getRequestURI().endsWith("/compose")?310000:maxBytes+1048576;if(size<0)error=411;else if(size>limit)error=413;else if(!active.add(auth.getName()))error=429;else{user=auth.getName();if(!capacity.tryAcquire())error=429;else acquired=true;}}}
        }catch(ResponseStatusException denied){error=denied.getStatusCode().value();}catch(org.springframework.dao.DataAccessException unavailable){error=503;}catch(NumberFormatException invalid){error=400;}
        try{if(error!=0){response.setStatus(error);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");response.getWriter().write("{\"error\":\"Envio de PDF indisponível. Verifique sua permissão, o tamanho e se há outro envio em andamento.\"}");return;}chain.doFilter(request,response);}finally{if(acquired)capacity.release();if(user!=null)active.remove(user);}
    }
}

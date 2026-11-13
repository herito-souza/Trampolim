package com.trampolim.api.privacy;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
@Configuration public class PrivacyWebConfig implements WebMvcConfigurer {
 private final Map<String,PrivacyStore> stores=new HashMap<>();
 public PrivacyWebConfig(List<PrivacyStore> stores){stores.forEach(s->this.stores.put(s.trail,s));}
 @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new HandlerInterceptor(){
  @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler){String path=req.getRequestURI();res.setHeader("Cache-Control","private, no-store, max-age=0");res.setHeader("Pragma","no-cache");res.setHeader("X-Content-Type-Options","nosniff");
   if(req.getMethod().equals("GET")&&(path.equals("/api/profile/export")||path.startsWith("/api/capacita/account/export")||path.equals("/api/empresas/account/export"))){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth!=null&&auth.isAuthenticated()&&!auth.getName().equals("anonymousUser")){String trail=path.startsWith("/api/capacita/")?"capacita":path.startsWith("/api/empresas/")?"empresas":"oportunidades";if(stores.containsKey(trail))stores.get(trail).recordExport(auth.getName());}}
   return true;
  }
 }).addPathPatterns("/api/profile/**","/api/empresas/account/**","/api/capacita/account/**","/api/admin/privacy/**");}
}

package com.trampolim.api.config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
/** Single-hop proxy trust for rate-limit addresses. Does not trust forwarded scheme or host. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TrustedProxyFilter extends OncePerRequestFilter {
 private final List<IpAddressMatcher> peers;
 public TrustedProxyFilter(@Value("${app.security.trusted-proxies:}") String configured) {
  var entries=new ArrayList<IpAddressMatcher>();
  for(String entry:configured.split(",")){entry=entry.trim();if(entry.isEmpty())continue;
   try{String[] parts=entry.split("/",-1);if(parts.length>2||!numeric(parts[0]))throw new IllegalArgumentException();int bits=parts[0].contains(":")?128:32;int mask=parts.length==2?Integer.parseInt(parts[1]):bits;if(mask<1||mask>bits)throw new IllegalArgumentException();entries.add(new IpAddressMatcher(entry));}
   catch(IllegalArgumentException invalid){throw new IllegalStateException("Configure somente IPs/CIDRs explícitos e restritos em app.security.trusted-proxies.");}
  }
  peers=List.copyOf(entries);
 }
 private static boolean numeric(String value){if(value==null||value.length()>64)return false;if(!(value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")||value.contains(":")&&value.matches("[0-9a-fA-F:.]+")))return false;try{InetAddress.getByName(value);return true;}catch(java.net.UnknownHostException invalid){return false;}}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String forwarded=request.getHeader("X-Forwarded-For");
  if(!peers.isEmpty()&&numeric(forwarded)&&peers.stream().anyMatch(peer->peer.matches(request.getRemoteAddr()))){
   chain.doFilter(new HttpServletRequestWrapper(request){@Override public String getRemoteAddr(){return forwarded;}@Override public String getRemoteHost(){return forwarded;}},response);
  }else chain.doFilter(request,response);
 }
}

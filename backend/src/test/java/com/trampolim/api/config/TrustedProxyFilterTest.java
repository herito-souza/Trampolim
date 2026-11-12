package com.trampolim.api.config;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
class TrustedProxyFilterTest {
 String address(String trusted,String peer,String header)throws Exception{var request=new MockHttpServletRequest("POST","/api/auth/login");request.setRemoteAddr(peer);if(header!=null)request.addHeader("X-Forwarded-For",header);var captured=new String[1];new TrustedProxyFilter(trusted).doFilter(request,new MockHttpServletResponse(),(r,s)->captured[0]=r.getRemoteAddr());return captured[0];}
 @Test void noProxyIsTrustedByDefault()throws Exception{assertEquals("192.0.2.10",address("","192.0.2.10","203.0.113.8"));}
 @Test void untrustedPeerCannotForgeTheRateLimitAddress()throws Exception{assertEquals("192.0.2.10",address("10.0.0.2/32","192.0.2.10","203.0.113.8"));}
 @Test void trustedSingleHopPreservesDistinctClientLimits()throws Exception{assertEquals("203.0.113.8",address("10.0.0.2/32","10.0.0.2","203.0.113.8"));assertEquals("203.0.113.9",address("10.0.0.2/32","10.0.0.2","203.0.113.9"));}
 @Test void ignoresChainsHostnamesAndInvalidAddresses()throws Exception{for(String input:new String[]{"1.2.3.4,5.6.7.8","attacker.example.test","999.999.999.999","abcd","::1%eth0",""})assertEquals("10.0.0.2",address("10.0.0.2/32","10.0.0.2",input));}
 @Test void supportsExplicitIpv6Peers()throws Exception{assertEquals("2001:db8:1::8",address("2001:db8::/64","2001:db8::2","2001:db8:1::8"));}
 @Test void rejectsUnrestrictedOrMalformedTrust(){for(String value:new String[]{"0.0.0.0/0","::/0","*","proxy.example.test","10.0.0.2/99"})assertThrows(IllegalStateException.class,()->new TrustedProxyFilter(value));}
}

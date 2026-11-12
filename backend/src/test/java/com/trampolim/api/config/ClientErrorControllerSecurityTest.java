package com.trampolim.api.config;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.security.BoundedRateLimiter;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
class ClientErrorControllerSecurityTest {
 MockHttpServletRequest request(String ip){var r=new MockHttpServletRequest();r.setRemoteAddr(ip);return r;}
 @Test void saturationFailsClosedWithoutGrowingTheMap(){var limiter=new BoundedRateLimiter(2,Clock.systemUTC());var controller=new ClientErrorController(limiter);var input=new ClientErrorController.ClientErrorInput("runtime","/");controller.report(input,request("192.0.2.1"));controller.report(input,request("192.0.2.2"));for(int i=3;i<100;i++){String ip="192.0.2."+i;assertEquals(429,assertThrows(ResponseStatusException.class,()->controller.report(input,request(ip))).getStatusCode().value());}assertEquals(2,limiter.size());}
 @Test void allowsSixtyReportsAndRejectsTheNext(){var controller=new ClientErrorController();var input=new ClientErrorController.ClientErrorInput("runtime","/");for(int i=0;i<60;i++)controller.report(input,request("192.0.2.1"));assertEquals(429,assertThrows(ResponseStatusException.class,()->controller.report(input,request("192.0.2.1"))).getStatusCode().value());}
 @Test void neverLogsAClientControlledPrivatePath(){Logger logger=(Logger)LoggerFactory.getLogger(ClientErrorController.class);var appender=new ListAppender<ILoggingEvent>();appender.start();logger.addAppender(appender);try{new ClientErrorController().report(new ClientErrorController.ClientErrorInput("runtime","/private/opaque-secret-ticket"),request("192.0.2.1"));assertEquals(1,appender.list.size());assertFalse(appender.list.getFirst().getFormattedMessage().contains("opaque-secret-ticket"));}finally{logger.detachAppender(appender);appender.stop();}}
}

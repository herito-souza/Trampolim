package com.trampolim.api.config;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
class BoundedRestClientsTest {
    @Test void slowExternalServiceCannotHoldRequestIndefinitely() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var release=new CountDownLatch(1);
        server.createContext("/slow",exchange->{try{release.await(2,TimeUnit.SECONDS);exchange.sendResponseHeaders(200,-1);}catch(InterruptedException error){Thread.currentThread().interrupt();}finally{exchange.close();}});server.start();
        try {var client=BoundedRestClients.create("http://127.0.0.1:"+server.getAddress().getPort(),Duration.ofMillis(100));assertThrows(ResourceAccessException.class,()->client.get().uri("/slow").retrieve().toBodilessEntity());}
        finally {release.countDown();server.stop(0);}
    }
}

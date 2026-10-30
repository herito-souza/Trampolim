package com.trampolim.api.radar;

import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.config.BoundedRestClients;

/** Explicit HTTP contracts; no repository or JDBC references to another trail. */
@Component
public class RadarBridge {
    @Value("${radar.internal-api-url:http://127.0.0.1:8080/api}") private String base;
    @Value("${radar.bridge-secret:}") private String secret;
    private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    private final java.util.concurrent.Semaphore inFlight=new java.util.concurrent.Semaphore(16);
    private String clientBase;private RestClient client;
    private synchronized RestClient client(){if(client==null||!Objects.equals(clientBase,base)){clientBase=base;client=BoundedRestClients.create(base,java.time.Duration.ofSeconds(5));}return client;}
    public JsonNode get(String path){if(!inFlight.tryAcquire())throw unavailable();try{JsonNode value=json.readTree(client().get().uri(path).retrieve().body(String.class));if(value==null)throw new IllegalStateException();return value;}catch(org.springframework.web.client.RestClientResponseException error){if(error.getStatusCode().is4xxClientError())throw new ResponseStatusException(error.getStatusCode(),"A fonte recusou a operação. Confira a disponibilidade, os dados e a autorização.");throw unavailable();}catch(Exception error){throw unavailable();}finally{inFlight.release();}}
    public JsonNode post(String path,Object body){configured();if(!inFlight.tryAcquire())throw unavailable();try{JsonNode value=json.readTree(client().post().uri(path).header("X-Trampolim-Bridge",secret).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(encode(body)).retrieve().body(String.class));if(value==null)throw new IllegalStateException();return value;}catch(org.springframework.web.client.RestClientResponseException error){if(error.getStatusCode().is4xxClientError())throw new ResponseStatusException(error.getStatusCode(),"A fonte recusou a operação. Confira a disponibilidade, os dados e a autorização.");throw unavailable();}catch(Exception error){throw unavailable();}finally{inFlight.release();}}
    public void configured(){if(secret==null||secret.length()<32)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"A integração segura do Radar ainda não está configurada.");}
    public void authorize(String supplied){configured();if(supplied==null||supplied.length()>256||!MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Integração não autorizada.");}
    public void validateSkills(Collection<String> codes){if(codes==null||codes.size()>40||codes.stream().anyMatch(code->code==null||!code.matches("[a-z][a-z0-9-]{1,63}")))RadarModel.bad("Competências inválidas.");if(codes.isEmpty())return;var rows=get("/public/skills/resolve?codes="+String.join(",",codes));Set<String> found=new HashSet<>();for(var row:rows)if(row.path("active").asBoolean())found.add(row.path("code").asText());if(!found.containsAll(codes))RadarModel.bad("Selecione competências ativas do catálogo padronizado.");}
    public <T>T convert(Object object,Class<T> type){return json.convertValue(object,type);}
    public String encode(Object object){try{return json.writeValueAsString(object);}catch(Exception error){throw new IllegalArgumentException("Dados do Radar inválidos.");}}
    public JsonNode read(Object object){try{return json.readTree(object.toString());}catch(Exception error){throw new IllegalStateException("Dados armazenados do Radar inválidos.");}}
    public static String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception error){throw new IllegalStateException(error);}}
    private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Uma fonte do Radar está temporariamente indisponível. Nenhum resultado foi inventado.");}
}

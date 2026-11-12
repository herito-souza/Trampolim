package com.trampolim.api.planning;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trampolim.api.radar.RadarBridge;
/** Stable across JVMs and replicas; map insertion order is not a recommendation criterion. */
public final class PlanHashes {
 private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
 private PlanHashes(){}
 public static String hash(Object value){try{return RadarBridge.hash(JSON.writeValueAsString(value));}catch(Exception invalid){throw new IllegalArgumentException("Não foi possível registrar os critérios do plano.");}}
}

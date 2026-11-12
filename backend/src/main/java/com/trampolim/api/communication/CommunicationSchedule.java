package com.trampolim.api.communication;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
/** Same platform calendar as the existing alert digest. No location is inferred. */
public final class CommunicationSchedule {
 private CommunicationSchedule(){}
 public static final ZoneId ZONE=ZoneId.of("America/Sao_Paulo");
 public static Instant digestAfter(Instant event,String frequency){if("immediate".equals(frequency))return null;var created=event.atZone(ZONE);var ready=created.withHour(9).withMinute(0).withSecond(0).withNano(0);if("daily".equals(frequency)){if(!ready.toInstant().isAfter(event))ready=ready.plusDays(1);}else{ready=ready.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));if(!ready.toInstant().isAfter(event))ready=ready.plusWeeks(1);}return ready.toInstant();}
}

package com.trampolim.api.communication;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
class CommunicationScheduleTest {
 @Test void immediateDoesNotInventAnExternalDelivery(){assertNull(CommunicationSchedule.digestAfter(Instant.parse("2026-11-07T12:00:00Z"),"immediate"));}
 @Test void dailyRespectsNineAMAndDoesNotMissTheBoundary(){assertEquals(Instant.parse("2026-11-07T12:00:00Z"),CommunicationSchedule.digestAfter(Instant.parse("2026-11-07T11:59:00Z"),"daily"));assertEquals(Instant.parse("2026-11-08T12:00:00Z"),CommunicationSchedule.digestAfter(Instant.parse("2026-11-07T12:00:00Z"),"daily"));}
 @Test void weeklyUsesMondayInThePlatformTimeZone(){assertEquals(Instant.parse("2026-11-09T12:00:00Z"),CommunicationSchedule.digestAfter(Instant.parse("2026-11-07T23:00:00Z"),"weekly"));assertEquals(Instant.parse("2026-11-16T12:00:00Z"),CommunicationSchedule.digestAfter(Instant.parse("2026-11-09T12:00:00Z"),"weekly"));}
}

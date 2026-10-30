package com.trampolim.api.future;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static com.trampolim.api.future.FutureModel.*;
class FutureModelTest {
 Input input(String status,int hires,String end){return new Input("Analista","Descrição","Tecnologia",hires,"Ensino médio","Inicial","Brasil","Remoto",LocalDate.now().toString(),end,status,0);}
 @Test void draftAndFutureForecastAreDistinctFromVacancy(){assertEquals("DRAFT",validate(input("DRAFT",2,LocalDate.now().plusMonths(3).toString())).status());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->validate(input("OPEN_JOB",2,LocalDate.now().plusMonths(3).toString())));}
 @Test void datesAndEstimatesAreValidated(){assertThrows(org.springframework.web.server.ResponseStatusException.class,()->validate(input("PUBLISHED",0,LocalDate.now().plusMonths(3).toString())));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->validate(input("PUBLISHED",2,LocalDate.now().minusDays(1).toString())));}
 @Test void smallCohortsNeverExposeExactCounts(){for(long i=0;i<5;i++){assertEquals(false,band(i).get("available"));assertFalse(band(i).containsKey("lower"));assertFalse(band(i).containsKey("count"));}}
 @Test void largerCountsAreBandedNotExact(){assertEquals(band(5),band(9));assertEquals(5L,band(7).get("lower"));assertEquals(9L,band(7).get("upper"));assertEquals(10L,band(10).get("lower"));}
}

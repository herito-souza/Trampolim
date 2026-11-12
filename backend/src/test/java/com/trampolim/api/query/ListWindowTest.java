package com.trampolim.api.query;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class ListWindowTest {
 @Test void legacyAndBoundedPagesKeepAnArrayContract(){assertTrue(new ListWindow(null,0).pageable().isUnpaged());assertEquals("",new ListWindow(null,0).sql());assertEquals(40,new ListWindow(20,2).pageable().getOffset());assertEquals(" LIMIT 20 OFFSET 40",new ListWindow(20,2).sql());}
 @Test void rejectsUnboundedAndInvalidClientPages(){for(int limit:new int[]{-1,0,201,Integer.MAX_VALUE})assertThrows(ResponseStatusException.class,()->new ListWindow(limit,0));assertThrows(ResponseStatusException.class,()->new ListWindow(10,-1));assertThrows(ResponseStatusException.class,()->new ListWindow(10,1001));assertThrows(ResponseStatusException.class,()->new ListWindow(null,1));}
}

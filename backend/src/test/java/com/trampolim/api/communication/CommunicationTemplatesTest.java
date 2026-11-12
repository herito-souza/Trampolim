package com.trampolim.api.communication;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CommunicationTemplatesTest {
 @Test void templatesAreVersionedPlainMessagesWithoutContactDataOrDeliveryPromises(){for(String trail:new String[]{"OPORTUNIDADES","EMPRESAS","CAPACITA"})for(String kind:CommunicationTemplates.kinds(trail)){var t=CommunicationTemplates.render(trail,kind);assertEquals(1,t.version());assertFalse(t.title().isBlank());assertTrue(t.body().length()<1000);assertTrue(t.target().startsWith("#"));assertFalse(t.body().contains("@"));assertFalse(t.body().contains("<script"));}assertTrue(CommunicationTemplates.render("EMPRESAS","INTERVIEW_INVITATION").body().contains("não comprova entrega"));}
 @Test void unknownEventsCannotChooseTheirOwnTemplateOrTarget(){assertThrows(IllegalArgumentException.class,()->CommunicationTemplates.render("CAPACITA","ARBITRARY_MESSAGE"));assertThrows(IllegalArgumentException.class,()->CommunicationTemplates.render("OPORTUNIDADES","COURSE_CHANGED"));assertThrows(IllegalArgumentException.class,()->CommunicationTemplates.render("UNKNOWN","NEW_MATCH"));}
}

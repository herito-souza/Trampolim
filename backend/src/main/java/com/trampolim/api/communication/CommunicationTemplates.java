package com.trampolim.api.communication;
import java.util.*;
public final class CommunicationTemplates {
 private CommunicationTemplates(){}
 public static final Set<String> KINDS=Set.of("TRUST_REVIEW","APPLICATION_UPDATED","INTERVIEW_INVITATION","NEW_MATCH","COURSE_CHANGED","ASSESSMENT_AVAILABLE","QUESTION_REPLY","QUESTION_ACTIVITY");
 public static Set<String> kinds(String trail){return switch(trail){case "OPORTUNIDADES"->Set.of("APPLICATION_UPDATED","NEW_MATCH");case "EMPRESAS"->Set.of("TRUST_REVIEW","APPLICATION_UPDATED","INTERVIEW_INVITATION");case "CAPACITA"->Set.of("COURSE_CHANGED","ASSESSMENT_AVAILABLE","QUESTION_REPLY","QUESTION_ACTIVITY");default->throw new IllegalArgumentException("Unsupported trail");};}
 public record Template(String title,String body,String target,int version){}
 public static Template render(String trail,String kind){
  if(!kinds(trail).contains(kind))throw new IllegalArgumentException("Unsupported event for trail");
  String prefix=trail.equals("CAPACITA")?"#capacita/learning/":trail.equals("EMPRESAS")?"#empresas/":"#oportunidades/";
  return switch(kind){
   case "TRUST_REVIEW"->new Template("Atualização de revisão de cadastro ou anúncio","Há uma atualização na análise da sua empresa ou de um anúncio. Consulte seus registros de revisão. Sinalização não constitui confirmação de fraude.","#empresas",1);
   case "APPLICATION_UPDATED"->new Template("Atualização no acompanhamento de candidatura",trail.equals("EMPRESAS")?"Uma etapa mudou em um processo da sua empresa. Consulte o recrutamento. Nenhum convite ao candidato é confirmado por este aviso.":"Seu acompanhamento pessoal foi atualizado. Consulte os registros na Central; isto não confirma uma decisão da empresa.",trail.equals("EMPRESAS")?"#selecao":"#oportunidades/applications",2);
   case "INTERVIEW_INVITATION"->new Template("Entrevista agendada no recrutamento","Há uma atualização de agendamento na sua empresa. Confirme o contato com o candidato; este aviso interno não comprova entrega de convite.","#selecao",1);
   case "NEW_MATCH"->new Template("Nova vaga nas suas preferências","Uma vaga publicada corresponde aos filtros de alerta que você escolheu. Confira requisitos e disponibilidade; isso não garante compatibilidade ou contratação.","#oportunidades/catalog-jobs",1);
   case "COURSE_CHANGED"->new Template("Atualização em curso acompanhado","Um curso com atividade sua teve informações alteradas. Consulte sua disponibilidade e os materiais atuais.",prefix+"courses",1);
   case "ASSESSMENT_AVAILABLE"->new Template("Avaliação publicada","Uma avaliação foi publicada em um curso com atividade sua. Confira os requisitos antes de iniciar.",prefix+"assessments",1);
   case "QUESTION_REPLY"->new Template("Resposta no mural de dúvidas","Há uma resposta em uma dúvida da qual você participa. Acesse o mural autenticado para consultar.",prefix+"questions",1);
   case "QUESTION_ACTIVITY"->new Template("Atualização no mural de dúvidas","Há atividade em uma dúvida da qual você participa. Acesse seu ambiente para consultar.",prefix+"questions",1);
   default->throw new IllegalArgumentException("Unsupported communication kind");
  };
 }
}

package com.trampolim.api.education;

/** Transparent pedagogical rule version. No sensitive attributes or inferred disabilities. */
public final class AdaptiveRules {
 public static final int VERSION=1;
 private AdaptiveRules(){}
 public static String action(boolean mastered,boolean skipMastered,boolean prerequisitesComplete,
                             boolean lessonsComplete,boolean latestFailed,boolean assessmentPassed,boolean canAttempt){
  if(mastered&&skipMastered)return "MASTERED";
  if(assessmentPassed&&lessonsComplete)return "COMPLETED";
  if(!prerequisitesComplete)return "PREREQUISITE";
  if(latestFailed)return canAttempt?"REVIEW":"TEACHER_SUPPORT";
  if(!lessonsComplete)return "STUDY";
  return canAttempt?"ASSESS":"TEACHER_SUPPORT";
 }
 public static String explanation(String action){return switch(action){
  case "MASTERED"->"Competências deste conteúdo já comprovadas no nível exigido; repetição dispensada pela regra do módulo.";
  case "COMPLETED"->"Aulas e avaliação deste contexto já concluídas.";
  case "PREREQUISITE"->"Conclua os módulos prévios definidos pelo professor antes de seguir esta recomendação.";
  case "REVIEW"->"A última tentativa não atingiu o critério. Revise as aulas antes de tentar novamente.";
  case "TEACHER_SUPPORT"->"Não há tentativa disponível ou o prazo terminou. Solicite orientação ao professor.";
  case "ASSESS"->"Aulas estudadas; a avaliação pode comprovar a competência. A correção ocorre no servidor.";
  default->"Estude as videoaulas válidas e acompanhe o progresso registrado pelo servidor.";
 };}
}

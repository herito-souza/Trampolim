package com.trampolim.api.career;

import java.util.List;
import java.util.Set;

/** Question templates adapt the interview without inferring personal traits. */
public final class CareerInterviewQuestions {
    public static final Set<String> LEVELS = Set.of("ENTRY", "JUNIOR", "MID", "SENIOR");
    public static final Set<String> TYPES = Set.of("GENERAL", "FIRST_JOB", "BEHAVIORAL", "TECHNICAL");

    private CareerInterviewQuestions() {}

    public static List<String> create(String occupation, String level, String type) {
        String role = occupation.trim();
        String scope = switch (level) {
            case "ENTRY" -> "em uma situação de início de carreira";
            case "JUNIOR" -> "em uma tarefa de nível júnior";
            case "MID" -> "em uma tarefa de nível pleno";
            case "SENIOR" -> "em uma situação de maior autonomia ou orientação técnica";
            default -> throw new IllegalArgumentException("Nível de experiência inválido.");
        };
        return switch (type) {
            case "FIRST_JOB" -> List.of(
                    "Que atividade de estudo, projeto ou vida cotidiana ajudou você a desenvolver algo útil para atuar como " + role + "?",
                    "Se recebesse uma tarefa nova como " + role + " e ainda não soubesse executá-la, como buscaria orientação e aprenderia?",
                    "Conte como você organizaria seus estudos e compromissos para cumprir uma tarefa profissional de " + role + ".");
            case "BEHAVIORAL" -> List.of(
                    "Conte uma situação real " + scope + " em que precisou colaborar com outras pessoas. Qual foi sua ação e o resultado observado?",
                    "Descreva um problema relacionado a " + role + ", como você analisou as opções e o que faria diferente hoje.",
                    "Como você organiza prioridades quando recebe demandas concorrentes? Dê um exemplo concreto.");
            case "TECHNICAL" -> List.of(
                    "Ao receber uma tarefa típica de " + role + ", quais etapas você seguiria para entender o problema antes de propor uma solução?",
                    "Descreva uma ferramenta, método ou conhecimento que utilizaria em " + role + " e explique como verificaria o resultado.",
                    "Como investigaria uma falha ou resultado inesperado durante uma tarefa de " + role + "? Explique seu raciocínio.");
            case "GENERAL" -> List.of(
                    "Apresente sua trajetória e explique o que motiva seu interesse por " + role + ".",
                    "Conte uma situação de estudo, projeto ou trabalho relacionada a " + role + " e explique sua contribuição.",
                    "Qual competência você gostaria de desenvolver para atuar como " + role + " e como pretende praticá-la?");
            default -> throw new IllegalArgumentException("Tipo de entrevista inválido.");
        };
    }
}

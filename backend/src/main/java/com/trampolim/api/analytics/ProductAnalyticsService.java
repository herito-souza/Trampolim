package com.trampolim.api.analytics;

import java.sql.Date;
import java.time.YearMonth;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Product indicators built from first-party operational records, without returning identities. */
@Service
public class ProductAnalyticsService {
    private final JdbcTemplate opportunities;
    private final ObjectProvider<JdbcTemplate> companiesProvider, learningProvider;

    public ProductAnalyticsService(@Qualifier("jdbcTemplate") JdbcTemplate opportunities,
            @Qualifier("empresasJdbcTemplate") ObjectProvider<JdbcTemplate> companies,
            @Qualifier("capacitaJdbcTemplate") ObjectProvider<JdbcTemplate> learning) {
        this.opportunities = opportunities;
        this.companiesProvider = companies;
        this.learningProvider = learning;
    }

    public Map<String,Object> report(String trail, String fromMonth, String toMonth, String state, String municipalityCode) {
        YearMonth from = month(fromMonth), to = month(toMonth);
        if (from.isAfter(to) || from.plusMonths(35).isBefore(to)) bad("Selecione um período de até 36 meses.");
        String normalizedState = state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
        String normalizedMunicipality = municipalityCode == null ? "" : municipalityCode.trim();
        if (!normalizedState.isEmpty() && !normalizedState.matches("[A-Z]{2}")) bad("UF inválida.");
        if (!normalizedMunicipality.isEmpty() && !normalizedMunicipality.matches("[0-9]{7}")) bad("Código IBGE inválido.");
        if (!normalizedState.isEmpty() && !normalizedMunicipality.isEmpty()) bad("Use UF ou município no filtro, não ambos.");
        String normalizedTrail=trail.toUpperCase(Locale.ROOT);
        if(!normalizedTrail.equals("EMPRESAS")&&(!normalizedState.isEmpty()||!normalizedMunicipality.isEmpty()))bad("O filtro geográfico está disponível apenas para métricas de vagas e empresas; os perfis não possuem município IBGE validado.");
        var metrics = new ArrayList<Map<String,Object>>();
        var unavailable = new ArrayList<Map<String,String>>();
        String coverage = null;
        switch (normalizedTrail) {
            case "OPORTUNIDADES" -> {
                addAccountMetrics(metrics, opportunities, "app_users", "account_type='PERSON'", from, to);
                addRetentionMetrics(metrics, opportunities, "app_users", "account_type='PERSON'", from, to);
                JdbcTemplate companies = companiesProvider.getIfAvailable();
                if (companies == null) unavailable.add(unavailable("first_application", "Banco Empresas indisponível nesta instância."));
                else addFirstApplicationMetric(metrics, companies, from, to, normalizedState, normalizedMunicipality);
                coverage = usageCoverage(opportunities);
                unavailable.add(unavailable("journey_abandonment", "Não há eventos persistidos de início e abandono do diagnóstico ou da busca; não inferimos abandono pela ausência de candidatura."));
            }
            case "EMPRESAS" -> {
                JdbcTemplate db = required(companiesProvider);
                addAccountMetrics(metrics, db, "company_users", "1=1", from, to);
                addRetentionMetrics(metrics, db, "company_users", "1=1", from, to);
                addCompanyMetrics(metrics, db, from, to, normalizedState, normalizedMunicipality);
                coverage = usageCoverage(db);
                unavailable.add(unavailable("journey_abandonment", "Não há eventos confiáveis de início e abandono do fluxo de publicação; métricas de candidaturas não são tratadas como abandono."));
            }
            case "CAPACITA" -> {
                JdbcTemplate db = required(learningProvider);
                addAccountMetrics(metrics, db, "student_users", "1=1", from, to);
                addRetentionMetrics(metrics, db, "student_users", "1=1", from, to);
                addLearningMetrics(metrics, db, from, to);
                coverage = usageCoverage(db);
                unavailable.add(unavailable("journey_abandonment", "A plataforma não registra matrícula/início explícito da trilha de estudo; progresso parcial não é suficiente para declarar abandono."));
            }
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trilha não encontrada.");
        }
        var result = new LinkedHashMap<String,Object>();
        result.put("trail", normalizedTrail);
        result.put("fromMonth", from.toString()); result.put("toMonth", to.toString());
        result.put("minimumCohort", ProductMetricPrivacy.MIN_COHORT);
        result.put("usageTrackingFrom", coverage);
        result.put("scope", Map.of("state", normalizedState, "municipalityCode", normalizedMunicipality,
                "geographicFilterAppliedTo", normalizedTrail.equals("EMPRESAS") ? "vagas, candidaturas, entrevistas, contratações e empresas" : "nenhuma métrica de usuário; localização de perfil não é normalizada por código IBGE"));
        result.put("metrics", metrics); result.put("unavailable", unavailable);
        result.put("notice", "Indicadores descrevem registros da Trampolim. Não representam todo o mercado regional nem garantem contratação.");
        return result;
    }

    private void addAccountMetrics(List<Map<String,Object>> out, JdbcTemplate db, String table, String condition, YearMonth from, YearMonth to) {
        String dateClause = "created_at >= ? AND created_at < ? AND " + condition;
        var args = instantBounds(from,to);
        add(out, "new_accounts", "Novas contas", "Contas criadas no mês; conta por trilha e sem unir identidades entre bases.", "contas",
                db.query("SELECT to_char(date_trunc('month',created_at AT TIME ZONE 'America/Sao_Paulo'),'YYYY-MM') period,count(*) value,count(*) contributors FROM "+table+" WHERE "+dateClause+" GROUP BY 1 ORDER BY 1", (rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")), args.toArray()));
        args = instantBounds(from,to);
        add(out, "registration_completion", "Cadastros confirmados", "E-mail verificado e aceite dos termos registrados entre contas criadas no mês.", "contas",
                db.query("SELECT to_char(date_trunc('month',created_at AT TIME ZONE 'America/Sao_Paulo'),'YYYY-MM') period,count(*) FILTER(WHERE email_verified=TRUE AND terms_accepted_at IS NOT NULL) value,count(*) FILTER(WHERE email_verified=TRUE AND terms_accepted_at IS NOT NULL) contributors FROM "+table+" WHERE "+dateClause+" GROUP BY 1 ORDER BY 1", (rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")), args.toArray()));
        args = instantBounds(from,to);
        add(out, "activated_accounts", "Ativação em até 7 dias", "Conta criada e com primeiro acesso autenticado até sete dias depois; coortes recentes podem ainda não ter completado a janela.", "contas",
                db.query("SELECT to_char(date_trunc('month',u.created_at AT TIME ZONE 'America/Sao_Paulo'),'YYYY-MM') period,count(*) FILTER(WHERE u.email_verified=TRUE AND u.created_at<CURRENT_TIMESTAMP-INTERVAL '7 days' AND EXISTS(SELECT 1 FROM product_usage_days d WHERE d.user_id=u.id AND d.usage_day>=(u.created_at AT TIME ZONE 'America/Sao_Paulo')::date AND d.usage_day<=((u.created_at AT TIME ZONE 'America/Sao_Paulo')::date+7))) value,count(*) FILTER(WHERE u.created_at<CURRENT_TIMESTAMP-INTERVAL '7 days') contributors FROM "+table+" u WHERE u.created_at >= ? AND u.created_at < ? AND "+condition+" GROUP BY 1 ORDER BY 1", (rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")), args.toArray()));
    }

    private void addRetentionMetrics(List<Map<String,Object>> out, JdbcTemplate db, String table, String condition, YearMonth from, YearMonth to) {
        String sql = "WITH calendar AS (SELECT (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date today), first_use AS (SELECT user_id,min(usage_day) first_day FROM product_usage_days GROUP BY user_id), cohorts AS (SELECT u.id,f.first_day FROM first_use f JOIN "+table+" u ON u.id=f.user_id WHERE f.first_day>=? AND f.first_day<? AND "+condition+") SELECT to_char(date_trunc('month',c.first_day),'YYYY-MM') period, count(*) FILTER(WHERE c.first_day+13<=cal.today AND EXISTS(SELECT 1 FROM product_usage_days d WHERE d.user_id=c.id AND d.usage_day BETWEEN c.first_day+7 AND c.first_day+13)) d7, count(*) FILTER(WHERE c.first_day+13<=cal.today) d7base, count(*) FILTER(WHERE c.first_day+36<=cal.today AND EXISTS(SELECT 1 FROM product_usage_days d WHERE d.user_id=c.id AND d.usage_day BETWEEN c.first_day+30 AND c.first_day+36)) d30, count(*) FILTER(WHERE c.first_day+36<=cal.today) d30base FROM cohorts c CROSS JOIN calendar cal GROUP BY 1 ORDER BY 1";
        var args = bounds(from,to);
        var rows = db.query(sql,(rs,n)->Map.<String,Object>of("period",rs.getString("period"),"d7",rs.getLong("d7"),"d7base",rs.getLong("d7base"),"d30",rs.getLong("d30"),"d30base",rs.getLong("d30base")),args.toArray());
        var d7 = new ArrayList<Map<String,Object>>(); var d30 = new ArrayList<Map<String,Object>>();
        for(var row:rows){long n7=((Number)row.get("d7")).longValue(),b7=((Number)row.get("d7base")).longValue(),n30=((Number)row.get("d30")).longValue(),b30=((Number)row.get("d30base")).longValue();d7.add(rateRow((String)row.get("period"),n7,b7));d30.add(rateRow((String)row.get("period"),n30,b30));}
        out.add(metric("retention_d7","Retenção D7", "Percentual da coorte de primeiro acesso que teve outro login entre os dias 7 e 13; considera apenas coortes cuja janela já amadureceu.","percentual",d7));
        out.add(metric("retention_d30","Retenção D30", "Percentual da coorte de primeiro acesso que teve outro login entre os dias 30 e 36; considera apenas coortes cuja janela já amadureceu.","percentual",d30));
    }

    private void addFirstApplicationMetric(List<Map<String,Object>> out, JdbcTemplate db, YearMonth from, YearMonth to, String state, String municipality) {
        String sql = "WITH first_apps AS (SELECT DISTINCT ON(lower(btrim(a.candidate_email))) lower(btrim(a.candidate_email)) candidate,a.created_at first_at,c.state,l.municipality_code FROM company_applications a JOIN company_listings l ON l.id=a.listing_id JOIN companies c ON c.id=l.company_id WHERE l.kind='job' AND a.consent_at IS NOT NULL AND a.candidate_email IS NOT NULL AND btrim(a.candidate_email)<>'' ORDER BY lower(btrim(a.candidate_email)),a.created_at,a.id) SELECT to_char(date_trunc('month',first_at AT TIME ZONE 'America/Sao_Paulo'),'YYYY-MM') period,count(*) value,count(*) contributors FROM first_apps WHERE first_at>=? AND first_at<? AND (?='' OR state=?) AND (?='' OR municipality_code=?) GROUP BY 1 ORDER BY 1";
        var queryArgs = append(instantBounds(from,to),state,state,municipality,municipality);
        add(out,"first_application", "Primeira candidatura registrada", "Candidatos distintos por e-mail cuja primeira candidatura conhecida na base Empresas ocorreu no mês; sem expor e-mail.","candidatos",db.query(sql,(rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")),queryArgs.toArray()));
    }

    private void addCompanyMetrics(List<Map<String,Object>> out, JdbcTemplate db, YearMonth from, YearMonth to, String state, String municipality) {
        String geo = " AND (?='' OR c.state=?) AND (?='' OR l.municipality_code=?)";
        String base = " FROM company_applications a JOIN company_listings l ON l.id=a.listing_id JOIN companies c ON c.id=l.company_id WHERE l.kind='job' AND a.consent_at IS NOT NULL"+geo;
        add(out,"applications", "Candidaturas recebidas", "Candidaturas consentidas registradas para vagas de emprego, agrupadas pela data de envio.","candidaturas",
                db.query("SELECT to_char(date_trunc('month',a.created_at AT TIME ZONE 'America/Sao_Paulo'),'YYYY-MM') period,count(*) value,count(DISTINCT lower(btrim(a.candidate_email))) contributors"+base+" AND a.created_at>=? AND a.created_at<? GROUP BY 1 ORDER BY 1",(rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")),append(geoArgs(state,municipality),instantBounds(from,to).toArray())));

        add(out,"applications_per_vacancy", "Média de candidaturas por vaga", "Candidaturas consentidas divididas pelas vagas que tiveram publicação registrada no mês; valores só aparecem quando o grupo tem pelo menos cinco candidatos.","candidaturas/vaga",
                db.query("WITH published AS (SELECT l.id,l.company_id,p.published_at FROM career_listing_publications p JOIN company_listings l ON l.id=p.listing_id JOIN companies c ON c.id=l.company_id WHERE l.kind='job' AND p.published_at>=? AND p.published_at<? AND (?='' OR c.state=?) AND (?='' OR l.municipality_code=?)), per_listing AS (SELECT p.id,date_trunc('month',p.published_at AT TIME ZONE 'America/Sao_Paulo') month,count(a.id) applications FROM published p LEFT JOIN company_applications a ON a.listing_id=p.id AND a.consent_at IS NOT NULL GROUP BY p.id,p.published_at), per_month AS (SELECT month,avg(applications) value FROM per_listing GROUP BY month), candidates AS (SELECT date_trunc('month',p.published_at AT TIME ZONE 'America/Sao_Paulo') month,count(DISTINCT lower(btrim(a.candidate_email))) contributors FROM published p JOIN company_applications a ON a.listing_id=p.id AND a.consent_at IS NOT NULL GROUP BY 1) SELECT to_char(m.month,'YYYY-MM') period,m.value,COALESCE(c.contributors,0) contributors FROM per_month m LEFT JOIN candidates c ON c.month=m.month ORDER BY 1",(rs,n)->row(rs.getString("period"),rs.getDouble("value"),rs.getLong("contributors")),append(instantBounds(from,to),state,state,municipality,municipality)));

        var interviewArgs = append(geoArgs(state,municipality),instantBounds(from,to).toArray());
        String outcomeBase=" FROM company_selection_outcomes o JOIN company_applications a ON a.id=o.application_id JOIN company_listings l ON l.id=a.listing_id JOIN companies c ON c.id=l.company_id WHERE l.kind='job' AND a.consent_at IS NOT NULL"+geo;
        add(out,"interviews_held", "Entrevistas realizadas", "Entrevistas marcadas como realizadas, agrupadas pela data do registro.","entrevistas",
                db.query("SELECT to_char(date_trunc('month',o.interview_at AT TIME ZONE 'America/Sao_Paulo'),'YYYY-MM') period,count(DISTINCT o.application_id) value,count(DISTINCT lower(btrim(a.candidate_email))) contributors"+outcomeBase+" AND o.interview_held=TRUE AND o.interview_at>=? AND o.interview_at<? GROUP BY 1 ORDER BY 1",(rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")),interviewArgs.toArray()));

        var hireArgs = append(geoArgs(state,municipality),Date.valueOf(from.atDay(1)),Date.valueOf(to.plusMonths(1).atDay(1)));
        String hireBase=" FROM company_selection_outcomes o JOIN company_applications a ON a.id=o.application_id JOIN company_listings l ON l.id=a.listing_id JOIN companies c ON c.id=l.company_id WHERE l.kind='job' AND a.consent_at IS NOT NULL AND o.candidate_confirmed_at IS NOT NULL AND o.hire_date IS NOT NULL AND o.public_metrics_consent=TRUE"+geo;
        add(out,"confirmed_hires", "Contratações confirmadas", "Somente contratação com data e confirmação do candidato, em registros com consentimento para métricas públicas.","contratações",
                db.query("SELECT to_char(date_trunc('month',o.hire_date::timestamp),'YYYY-MM') period,count(DISTINCT o.application_id) value,count(DISTINCT lower(btrim(a.candidate_email))) contributors"+hireBase+" AND o.hire_date>=? AND o.hire_date<? GROUP BY 1 ORDER BY 1",(rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")),hireArgs.toArray()));

        var timeArgs = append(geoArgs(state,municipality),Date.valueOf(from.atDay(1)),Date.valueOf(to.plusMonths(1).atDay(1)));
        add(out,"median_days_to_hire", "Mediana de dias até contratação", "Mediana entre envio da candidatura e data de contratação confirmada; mesma base consentida da métrica de contratações.","dias",
                db.query("SELECT to_char(date_trunc('month',o.hire_date::timestamp),'YYYY-MM') period,percentile_cont(0.5) WITHIN GROUP(ORDER BY (o.hire_date-(a.created_at AT TIME ZONE 'America/Sao_Paulo')::date)) value,count(DISTINCT lower(btrim(a.candidate_email))) contributors"+hireBase+" AND o.hire_date>=? AND o.hire_date<? AND o.hire_date>=(a.created_at AT TIME ZONE 'America/Sao_Paulo')::date GROUP BY 1 ORDER BY 1",(rs,n)->row(rs.getString("period"),rs.getDouble("value"),rs.getLong("contributors")),timeArgs.toArray()));

        var companyArgs = bounds(from,to); companyArgs.addAll(List.of(state,state,municipality,municipality));
        add(out,"active_companies", "Empresas ativas", "Empresas aprovadas com ao menos um usuário autorizado que acessou a trilha no mês.","empresas",
                db.query("SELECT to_char(date_trunc('month',d.usage_day::timestamp),'YYYY-MM') period,count(DISTINCT c.id) value,count(DISTINCT c.id) contributors FROM product_usage_days d JOIN company_users u ON u.id=d.user_id JOIN companies c ON (c.owner_user_id=u.id OR EXISTS(SELECT 1 FROM company_members m WHERE m.company_id=c.id AND m.user_id=u.id)) WHERE c.verification_status='APPROVED' AND d.usage_day>=? AND d.usage_day<? AND (?='' OR c.state=?) AND (?='' OR EXISTS(SELECT 1 FROM company_listings gl WHERE gl.company_id=c.id AND gl.kind='job' AND gl.publication_status='PUBLISHED' AND gl.municipality_code=?)) GROUP BY 1 ORDER BY 1",(rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")),companyArgs.toArray()));

        var filledArgs = append(geoArgs(state,municipality),Date.valueOf(from.atDay(1)),Date.valueOf(to.plusMonths(1).atDay(1)));
        add(out,"vacancies_with_confirmed_hire", "Vagas com contratação confirmada", "Vagas distintas associadas a contratação confirmada; não infere preenchimento a partir de candidatura ou encerramento.","vagas",db.query("SELECT to_char(date_trunc('month',o.hire_date::timestamp),'YYYY-MM') period,count(DISTINCT l.id) value,count(DISTINCT c.id) contributors"+hireBase+" AND o.hire_date>=? AND o.hire_date<? GROUP BY 1 ORDER BY 1",(rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")),filledArgs.toArray()));
    }

    private void addLearningMetrics(List<Map<String,Object>> out, JdbcTemplate db, YearMonth from, YearMonth to) {
        var args=instantBounds(from,to);
        add(out,"course_completions","Cursos concluídos","Eventos idempotentes course.completed.v1, emitidos após os critérios existentes de conclusão de aulas e avaliações.","conclusões",db.query("SELECT to_char(date_trunc('month',occurred_at AT TIME ZONE 'America/Sao_Paulo'),'YYYY-MM') period,count(DISTINCT aggregate_id) value,count(DISTINCT payload->>'studentId') contributors FROM domain_event_outbox WHERE event_name='course.completed.v1' AND occurred_at>=? AND occurred_at<? GROUP BY 1 ORDER BY 1",(rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")),args.toArray()));
        args=instantBounds(from,to);
        add(out,"verified_competencies","Competências verificadas","Conquistas não revogadas emitidas por avaliações aprovadas; cursos concluídos não são contados como competência automaticamente.","competências",db.query("SELECT to_char(date_trunc('month',awarded_at AT TIME ZONE 'America/Sao_Paulo'),'YYYY-MM') period,count(*) value,count(DISTINCT student_id) contributors FROM learning_competency_awards WHERE revoked_at IS NULL AND awarded_at>=? AND awarded_at<? GROUP BY 1 ORDER BY 1",(rs,n)->row(rs.getString("period"),rs.getLong("value"),rs.getLong("contributors")),args.toArray()));
    }

    private String usageCoverage(JdbcTemplate db) {
        return db.queryForObject("SELECT min(usage_day)::text FROM product_usage_days",String.class);
    }
    private static JdbcTemplate required(ObjectProvider<JdbcTemplate> provider) {
        JdbcTemplate db=provider.getIfAvailable(); if(db==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Banco da trilha indisponível."); return db;
    }
    private static List<Object> bounds(YearMonth from,YearMonth to){return new ArrayList<>(List.of(Date.valueOf(from.atDay(1)),Date.valueOf(to.plusMonths(1).atDay(1))));}
    private static List<Object> instantBounds(YearMonth from,YearMonth to){var zone=java.time.ZoneId.of("America/Sao_Paulo");return new ArrayList<>(List.of(java.sql.Timestamp.from(from.atDay(1).atStartOfDay(zone).toInstant()),java.sql.Timestamp.from(to.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant())));}
    private static List<Object> append(List<Object> input,Object... values){var out=new ArrayList<>(input);Collections.addAll(out,values);return out;}
    private static void addGeo(List<Object> args,String state,String municipality){Collections.addAll(args,state,state,municipality,municipality);}
    private static List<Object> geoArgs(String state,String municipality){var args=new ArrayList<Object>();addGeo(args,state,municipality);return args;}
    private static Map<String,Object> row(String period,Object value,long contributors){var map=new LinkedHashMap<String,Object>();map.put("period",period);map.put("value",value);map.put("contributors",contributors);return map;}
    private static Map<String,Object> rateRow(String period,long numerator,long denominator){var map=new LinkedHashMap<String,Object>();map.put("period",period);map.put("numerator",numerator);map.put("denominator",denominator);map.put("contributors",denominator);return map;}
    private static void add(List<Map<String,Object>> out,String key,String label,String formula,String unit,List<Map<String,Object>> rows){out.add(metric(key,label,formula,unit,rows));}
    private static Map<String,Object> metric(String key,String label,String formula,String unit,List<Map<String,Object>> rows){return ProductMetricPrivacy.metric(key,label,formula,unit,rows);}
    private static Map<String,String> unavailable(String key,String reason){return Map.of("key",key,"reason",reason);}
    private static YearMonth month(String value){try{return YearMonth.parse(value);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use meses no formato AAAA-MM.");}}
    private static void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}

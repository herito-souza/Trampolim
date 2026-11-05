package com.trampolim.api.geography;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Optional, additive filters; unknown private facts never count as a match. */
public final class SearchRefinements {
    private SearchRefinements() {}
    private static void bad() { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revise os filtros de pesquisa."); }
    public static RegionalQuery.Sql apply(TerritorialCatalog geo, RegionalQuery.Sql base, Map<String,String> p, boolean company) {
        var sql = new StringBuilder(base.where()); var args = new ArrayList<Object>(base.args());
        String state = text(p,"state",2), contract = text(p,"contractType",80), experience = text(p,"experienceLevel",40);
        if (!state.isEmpty()) {
            var codes = geo.stateCodes(state); var aliases = geo.legacyAliases(codes);
            sql.append(" AND (l.municipality_code IN (").append(RegionalQuery.placeholders(codes.size())).append(") OR (l.municipality_code IS NULL AND geo_normalize(l.location) IN (").append(RegionalQuery.placeholders(aliases.size())).append("))");
            args.addAll(codes); args.addAll(aliases);
            if (Boolean.parseBoolean(p.getOrDefault("includeRemote","true"))) sql.append(" OR geo_normalize(l.modality) IN ('remoto','online')");
            sql.append(")");
        }
        if (!contract.isEmpty()) {
            String normalized = TerritorialCatalog.normalize(contract);
            if (company && normalized.equals("estagio")) sql.append(" AND l.is_internship=TRUE");
            else { sql.append(" AND geo_normalize(l.contract_type) IN (?,?)"); args.add(normalized); args.add(normalized.equals("aprendiz")?"jovem aprendiz":normalized); }
        }
        if (!experience.isEmpty()) {
            if (company) {
                if (!TerritorialCatalog.normalize(experience).equals("sem experiencia")) sql.append(" AND FALSE");
                else sql.append(" AND EXISTS (SELECT 1 FROM company_radar_requirements sr WHERE sr.listing_id=l.id AND sr.definition->>'experienceMonths'='0')");
            } else { sql.append(" AND geo_normalize(l.experience_level)=?"); args.add(TerritorialCatalog.normalize(experience)); }
        }
        Integer education = integer(p,"educationLevel",1,5), months = integer(p,"maxExperienceMonths",0,600), days = integer(p,"publishedDays",1,365);
        if (education != null || months != null) {
            if (!company) sql.append(" AND FALSE");
            else {
                sql.append(" AND EXISTS (SELECT 1 FROM company_radar_requirements sr WHERE sr.listing_id=l.id");
                if (education != null) { sql.append(" AND CASE WHEN sr.definition->>'educationLevel' ~ '^[1-5]$' THEN (sr.definition->>'educationLevel')::integer <= ? ELSE FALSE END"); args.add(education); }
                if (months != null) { sql.append(" AND CASE WHEN sr.definition->>'experienceMonths' ~ '^[0-9]{1,3}$' THEN (sr.definition->>'experienceMonths')::integer <= ? ELSE FALSE END"); args.add(months); }
                sql.append(")");
            }
        }
        if (days != null) {
            sql.append(company ? " AND (SELECT published_at FROM career_listing_publications sp WHERE sp.listing_id=l.id) >= CURRENT_TIMESTAMP - (? * INTERVAL '1 day')" : " AND l.published_at >= CURRENT_TIMESTAMP - (? * INTERVAL '1 day')"); args.add(days);
        }
        BigDecimal minimum = money(p,"minimumSalary"), maximum = money(p,"maximumSalary");
        if(minimum!=null&&maximum!=null&&minimum.compareTo(maximum)>0) bad();
        if(minimum!=null||maximum!=null)sql.append(" AND ").append(salary(1)).append("<=").append(salary(2));
        if(minimum!=null){sql.append(" AND ").append(salary(2)).append(">=?");args.add(minimum);}
        if(maximum!=null){sql.append(" AND ").append(salary(1)).append("<=?");args.add(maximum);}
        String radius=text(p,"radiusKm",10),lat=text(p,"latitude",16),lon=text(p,"longitude",16);
        if(!radius.isEmpty()||!lat.isEmpty()||!lon.isEmpty()){
            if(company)bad();
            double km=coordinate(radius,1,500),latitude=coordinate(lat,-90,90),longitude=coordinate(lon,-180,180);
            double delta=km/110.0;
            sql.append(" AND l.latitude BETWEEN ? AND ? AND l.longitude IS NOT NULL AND 6371*acos(LEAST(1.0,GREATEST(-1.0,sin(radians(?))*sin(radians(l.latitude))+cos(radians(?))*cos(radians(l.latitude))*cos(radians(l.longitude-?)))))<=?");
            args.add(latitude-delta);args.add(latitude+delta);args.add(latitude);args.add(latitude);args.add(longitude);args.add(km);
        }
        if(Boolean.parseBoolean(p.getOrDefault("accessibleOnly","false")))sql.append(company?" AND FALSE":" AND NULLIF(trim(l.accessibility),'') IS NOT NULL");
        String provider=text(p,"provider",180);if(!provider.isEmpty()){sql.append(company?" AND geo_normalize(c.trade_name)=?":" AND geo_normalize(l.provider)=?");args.add(TerritorialCatalog.normalize(provider));}
        String schedule=text(p,"workSchedule",100);if(!schedule.isEmpty()){sql.append(" AND geo_normalize(l.work_schedule) LIKE ?");args.add("%"+TerritorialCatalog.normalize(schedule)+"%");}
        return new RegionalQuery.Sql(sql.toString(),args);
    }
    static String salary(int group) {
        String number="(?:[0-9]{1,3}(?:[.][0-9]{3})+|[0-9]+)(?:,[0-9]{1,2})?";
        String match="regexp_match(l.salary,'^[[:space:]]*(?:R[$][[:space:]]*)?("+number+")(?:[[:space:]]*(?:-|a|até)[[:space:]]*(?:R[$][[:space:]]*)?("+number+"))?[[:space:]]*$','i')";
        String first="("+match+")[1]", value=group==1?first:"COALESCE(("+match+")[2],"+first+")";
        return "NULLIF(replace(replace("+value+",'.',''),',','.'),'')::numeric";
    }
    static double coordinate(String value,double min,double max){try{double n=Double.parseDouble(value);if(!Double.isFinite(n)||n<min||n>max)bad();return n;}catch(NumberFormatException e){bad();return 0;}}
    static String text(Map<String,String> p,String key,int max){String v=p.getOrDefault(key,"").trim();if(v.length()>max)bad();return v;}
    static Integer integer(Map<String,String> p,String key,int min,int max){String v=text(p,key,10);if(v.isEmpty())return null;try{int n=Integer.parseInt(v);if(n<min||n>max)bad();return n;}catch(NumberFormatException e){bad();return null;}}
    static BigDecimal money(Map<String,String> p,String key){String v=text(p,key,16);if(v.isEmpty())return null;try{BigDecimal n=new BigDecimal(v);if(n.signum()<0||n.compareTo(new BigDecimal("100000000"))>0||n.scale()>2)bad();return n;}catch(NumberFormatException e){bad();return null;}}
}

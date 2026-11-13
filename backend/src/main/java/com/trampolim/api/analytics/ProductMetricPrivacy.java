package com.trampolim.api.analytics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies minimum-cohort suppression to every metric series before it leaves the backend. */
final class ProductMetricPrivacy {
    static final int MIN_COHORT=5;
    private ProductMetricPrivacy() {}

    static Map<String,Object> metric(String key,String label,String formula,String unit,List<Map<String,Object>> rows) {
        var series=new ArrayList<Map<String,Object>>();
        for(var row:rows){
            var item=new LinkedHashMap<String,Object>(); item.put("period",row.get("period"));
            long contributors=((Number)row.get("contributors")).longValue();
            boolean suppressed=contributors>0&&contributors<MIN_COHORT;
            item.put("suppressed",suppressed); item.put("contributors",suppressed?null:contributors);
            if(row.containsKey("numerator")){
                long base=((Number)row.get("denominator")).longValue();
                item.put("denominator",base<MIN_COHORT?null:base);
                item.put("value",base<MIN_COHORT?null:Math.round(((Number)row.get("numerator")).longValue()*10000.0/base)/100.0);
                item.put("suppressed",base>0&&base<MIN_COHORT);
            }else item.put("value",suppressed?null:row.get("value"));
            series.add(item);
        }
        var result=new LinkedHashMap<String,Object>(); result.put("key",key); result.put("label",label);
        result.put("formula",formula); result.put("unit",unit); result.put("series",series); return result;
    }
}

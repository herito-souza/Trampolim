package com.trampolim.api.config.features;

import java.util.Set;

/** Targeting narrows feature release availability; it never grants resource authorization. */
public final class FeatureFlagTargeting {
    private FeatureFlagTargeting() {}
    public static final Set<String> PROFILES=Set.of("CANDIDATE","STUDENT","TEACHER","COMPANY_OWNER","RECRUITER","ADMIN","ALL_AUTHENTICATED");
    public static boolean matches(FeatureFlag f,String environment,String profile,Long companyId,String municipality){
        if(f==null||!f.enabled()||environment==null||profile==null)return false;
        if(!f.environments().contains(environment.toLowerCase(java.util.Locale.ROOT))
                ||!f.profiles().contains(profile.toUpperCase(java.util.Locale.ROOT))&&!f.profiles().contains("ALL_AUTHENTICATED"))return false;
        if(!f.companyIds().isEmpty()&&(companyId==null||!f.companyIds().contains(companyId)))return false;
        return f.municipalityCodes().isEmpty()||municipality!=null&&f.municipalityCodes().contains(municipality);
    }
}

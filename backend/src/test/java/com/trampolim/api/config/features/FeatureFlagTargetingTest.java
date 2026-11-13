package com.trampolim.api.config.features;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class FeatureFlagTargetingTest {
    private FeatureFlag flag(boolean enabled,List<String> env,List<String> profiles,List<Long> companies,List<String> cities){return new FeatureFlag("observatory.v2",enabled,1,"now","admin@example.test",env,profiles,companies,cities);}
    @Test void requiresExplicitEnablementEnvironmentAndProfile(){var f=flag(true,List.of("staging"),List.of("ADMIN"),List.of(),List.of());assertTrue(FeatureFlagTargeting.matches(f,"staging","admin",null,null));assertFalse(FeatureFlagTargeting.matches(f,"prod","ADMIN",null,null));assertFalse(FeatureFlagTargeting.matches(f,"staging","CANDIDATE",null,null));assertFalse(FeatureFlagTargeting.matches(flag(false,List.of("staging"),List.of("ADMIN"),List.of(),List.of()),"staging","ADMIN",null,null));}
    @Test void companyAndMunicipalityTargetsOnlyNarrowRelease(){var f=flag(true,List.of("prod"),List.of("COMPANY_OWNER"),List.of(17L),List.of("2914605"));assertTrue(FeatureFlagTargeting.matches(f,"prod","COMPANY_OWNER",17L,"2914605"));assertFalse(FeatureFlagTargeting.matches(f,"prod","COMPANY_OWNER",18L,"2914605"));assertFalse(FeatureFlagTargeting.matches(f,"prod","RECRUITER",17L,"2914605"));assertFalse(FeatureFlagTargeting.matches(f,"prod","COMPANY_OWNER",17L,null));}
    @Test void unknownFlagsFailClosed(){assertFalse(FeatureFlagTargeting.matches(null,"prod","ADMIN",1L,"2914605"));}
    @Test void authenticatedAudienceIsExplicit(){var f=flag(true,List.of("prod"),List.of("ALL_AUTHENTICATED"),List.of(),List.of());assertTrue(FeatureFlagTargeting.matches(f,"prod","STUDENT",null,null));assertFalse(FeatureFlagTargeting.matches(flag(true,List.of("prod"),List.of(),List.of(),List.of()),"prod","STUDENT",null,null));}
}

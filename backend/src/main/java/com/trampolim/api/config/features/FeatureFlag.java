package com.trampolim.api.config.features;

import java.util.List;

public record FeatureFlag(String key, boolean enabled, int revision, String updatedAt, String updatedBy,
        List<String> environments, List<String> profiles, List<Long> companyIds, List<String> municipalityCodes) {
    public FeatureFlag { environments=List.copyOf(environments); profiles=List.copyOf(profiles); companyIds=List.copyOf(companyIds); municipalityCodes=List.copyOf(municipalityCodes); }
}

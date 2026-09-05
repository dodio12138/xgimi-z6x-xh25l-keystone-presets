package com.xgimirom.presets;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class ProjectionPreset {
    final String fullCoordinates;
    final Map<String, String> environments;

    ProjectionPreset(String fullCoordinates, Map<String, String> environments) {
        this.fullCoordinates = fullCoordinates;
        this.environments = Collections.unmodifiableMap(
                new LinkedHashMap<String, String>(environments));
    }
}

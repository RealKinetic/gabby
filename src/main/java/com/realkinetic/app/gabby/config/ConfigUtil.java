package com.realkinetic.app.gabby.config;

import java.util.Collections;
import java.util.List;

public class ConfigUtil {
    public static List<String> validateParameterGreaterThanZero(int value, String name) {
        if (value <= 0) {
            return Collections.singletonList(name + " must be greater than zero");
        }

        return Collections.emptyList();
    }

    public static List<String> validateStringNotEmpty(String value, String name) {
        if (value == null || value.isEmpty()) {
            return Collections.singletonList(name + " must not be empty");
        }

        return Collections.emptyList();
    }
}



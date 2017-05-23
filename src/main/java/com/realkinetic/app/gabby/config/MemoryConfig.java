package com.realkinetic.app.gabby.config;

import java.util.ArrayList;
import java.util.List;

public class MemoryConfig {
    public int getMaxAccesses() {
        return maxAccesses;
    }

    public void setMaxAccesses(int maxAccesses) {
        this.maxAccesses = maxAccesses;
    }

    private int maxAccesses;

    public List<String> validate() {
        final List<String> errors = new ArrayList<>();
        errors.addAll(ConfigUtil.validateParameterGreaterThanZero(this.maxAccesses, "maxAccesses"));
        return errors;
    }
}

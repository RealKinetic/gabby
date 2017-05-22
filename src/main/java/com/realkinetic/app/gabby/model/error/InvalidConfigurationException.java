package com.realkinetic.app.gabby.model.error;

import java.util.List;

public class InvalidConfigurationException extends Exception {
    public InvalidConfigurationException(List<String> message) {
        super(String.join("\n", message));
    }
}

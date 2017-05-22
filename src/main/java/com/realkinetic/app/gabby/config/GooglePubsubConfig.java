package com.realkinetic.app.gabby.config;

import java.util.ArrayList;
import java.util.List;

public class GooglePubsubConfig {
    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        errors.addAll(ConfigUtil.validateStringNotEmpty(this.project, "project"));
        errors.addAll(ConfigUtil.validateStringNotEmpty(this.appName, "appName"));
        return errors;
    }

    private String project;
    private String appName;
    private int readTimeout = 30;
}

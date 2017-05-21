package com.realkinetic.app.gabby.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class YamlConfig {
    private static final String FILENAME = "config.yaml";

    public static Config load() throws FileNotFoundException {
        return load(FILENAME);
    }

    public static Config load(String filename) throws FileNotFoundException {
        return load(new FileInputStream(filename));
    }

    public static Config load(InputStream stream) {
        Yaml yaml = new Yaml(new Constructor(BaseConfig.class));
        return (BaseConfig) yaml.load(stream);
    }
}

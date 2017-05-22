/*
Copyright 2017 Real Kinetic LLC

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
*/
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

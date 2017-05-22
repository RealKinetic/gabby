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

import java.util.ArrayList;
import java.util.List;

public class RedisConfig {
    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public int getConnectionPoolSize() {
        return this.connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (this.hosts == null || this.hosts.size() == 0) {
            errors.add("redis config must contain a list of hosts");
        }

        if (connectionPoolSize <= 0) {
            errors.add("redis connection pool size must be greater than 0");
        }

        return errors;
    }

    private List<String> hosts;
    private int connectionPoolSize;
}

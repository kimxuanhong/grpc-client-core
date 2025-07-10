package com.xhk.grpc.utils;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

public class EnvUtils {

    public static Map<String, String> getClientProperties(String prefix, Environment environment) {
        Map<String, String> result = new HashMap<>();
        for (PropertySource<?> propertySource : ((AbstractEnvironment) environment).getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource) {
                for (String propertyName : ((EnumerablePropertySource<?>) propertySource).getPropertyNames()) {
                    if (propertyName.startsWith(prefix)) {
                        String key = propertyName.substring(prefix.length() + 1);
                        String value = environment.getProperty(propertyName);
                        result.put(key, value);
                    }
                }
            }
        }
        return result;
    }
}

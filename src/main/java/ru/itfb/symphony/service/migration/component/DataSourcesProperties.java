package ru.itfb.symphony.service.migration.component;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app")
public class DataSourcesProperties {

    private Map<String, DataSourceProperties> datasources;

    @Data
    public static class DataSourceProperties {
        private String driverClassName;
        private String url;
        private String username;
        private String password;
    }
}
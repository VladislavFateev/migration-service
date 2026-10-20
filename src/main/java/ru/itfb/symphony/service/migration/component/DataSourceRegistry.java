package ru.itfb.symphony.service.migration.component;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceRegistry {

    private final DataSourcesProperties properties;
    private final ConcurrentHashMap<String, DataSource> dataSources = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (CollectionUtils.isEmpty(properties.getDatasources()))
            return;

        properties
                .getDatasources()
                .forEach((key, props) -> {
                    HikariConfig config = new HikariConfig();
                    config.setDriverClassName(props.getDriverClassName());
                    config.setJdbcUrl(props.getUrl());
                    config.setUsername(props.getUsername());
                    config.setPassword(props.getPassword());

                    log.info(String.format("Initializing data source for [%s].", key));
                    dataSources.put(key, new HikariDataSource(config));
                    log.info(String.format("Data source [%s] initialized.", key));
                });
    }

    public JdbcTemplate getDataSource(String datasourceName) {
        if (!dataSources.containsKey(datasourceName))
            throw new RuntimeException("DataSource [" + datasourceName + "] not exists");
        return new JdbcTemplate(dataSources.get(datasourceName));
    }

    @PreDestroy
    public void destroy() {
        dataSources.forEach((key, ds) -> {
            try {
                log.info(String.format("Closing data source for [%s].", key));
                ((HikariDataSource) ds).close();
                log.info(String.format("Data source [%s] closed.", key));
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        });
    }

}

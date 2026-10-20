package ru.itfb.symphony.service.migration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import ru.itfb.symphony.service.migration.component.DataSourcesProperties;

@SpringBootApplication
@EnableConfigurationProperties(DataSourcesProperties.class)
//@EnableAsync
@OpenAPIDefinition(
        info = @Info(
                title = "Сервис миграции данных"
        )
)
public class MigrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MigrationServiceApplication.class, args);
    }

}

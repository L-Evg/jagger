package com.griddynamics.jagger.jaas;

import com.griddynamics.jagger.dbapi.DatabaseService;
import com.griddynamics.jagger.engine.e1.services.DataService;
import com.griddynamics.jagger.engine.e1.services.DefaultDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.*;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Spring Boot based starter.
 */
@PropertySources({@PropertySource("classpath:/basic/default.environment.properties")})
@ImportResource({"classpath:/common/storage.rdb.client.conf.xml", "classpath:/spring/dbapi.config.xml"})
@SpringBootApplication
@EnableSwagger2

public class JaasStarter {

    @Autowired
    DatabaseService databaseService;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(JaasStarter.class, args);
    }

    @Bean
    public DataService getDataService() {
        return new DefaultDataService(databaseService);
    }

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("NAME")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }
}

package pl.ankietor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnkietorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnkietorApplication.class, args);
    }
}

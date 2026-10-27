package ch.conciliations.mabecoju.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MabecojuApplication {

    public static void main(String[] args) {
        SpringApplication.run(MabecojuApplication.class, args);
    }
}

package br.com.gems.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
@SpringBootApplication(scanBasePackages = "br.com.gems.app")
@EntityScan(basePackages = "br.com.gems.app.model.entity")
public class AppApplication {
    public static void main( String[] args ) {
        SpringApplication.run( AppApplication.class, args );
    }
}

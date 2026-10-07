package br.com.gems.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * Ponto de entrada da API interna do sistema.
 *
 * <p>
 * O {@code scanBasePackages} abrange {@code br.com.gems} — e não apenas
 * {@code br.com.gems.app} — porque as autoconfigurações da GEMS SDK vivem sob a raiz comum.
 *
 * <p>
 * O {@code @EntityScan} aponta só para {@code br.com.gems.app}: todas as entidades JPA do
 * sistema estão no módulo {@code app-model}, sob {@code br.com.gems.app.model.entity}.
 */
@SpringBootApplication(scanBasePackages = "br.com.gems")
@EntityScan(basePackages = "br.com.gems.app")
public class AppApplication {

    public static void main( String[] args ) {
        SpringApplication.run( AppApplication.class, args );
    }

}

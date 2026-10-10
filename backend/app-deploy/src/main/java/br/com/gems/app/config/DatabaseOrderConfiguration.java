package br.com.gems.app.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Security JDBC and model validation must follow the explicit platform migrations. */
@Configuration
public class DatabaseOrderConfiguration {
    @Bean
    public static BeanFactoryPostProcessor migrationDependencies() {
        return factory -> {
            for ( String name : java.util.List.of( "firebaseSecurityCatalog", "validationSchemaInitializer" ) ) {
                if ( factory.containsBeanDefinition( name ) ) {
                    factory.getBeanDefinition( name ).setDependsOn( "liquibase" );
                }
            }
        };
    }
}

package br.com.gems.app.config;

import br.com.gems.firebase.auth.FirebaseAuthException;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** SDK administration refusals have the same public status as request resolution. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FirebaseFailureHandler {
    @ExceptionHandler(FirebaseAuthException.class)
    public ResponseEntity<Map<String, String>> firebase( FirebaseAuthException failure ) {
        return ResponseEntity.status( failure.reason().status() ).body( Map.of( "code", failure.reason().name() ) );
    }
}

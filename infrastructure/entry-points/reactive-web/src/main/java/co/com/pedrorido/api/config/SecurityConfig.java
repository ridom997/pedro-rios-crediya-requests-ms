package co.com.pedrorido.api.config;

import co.com.pedrorido.api.auth.JwtAuthConverter;
import co.com.pedrorido.api.dto.GeneralResponseDTO;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;

@Configuration
@EnableReactiveMethodSecurity // habilita @PreAuthorize en handlers reactivos
@Log4j2
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            JwtAuthConverter jwtAuthConverter,
            ServerAccessDeniedHandler accessDeniedHandler
    ) {
        log.info("Creating springSecurityFilterChain");
        return http
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/solicitud").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/v1/solicitud").hasRole("CLIENTE")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/solicitud").hasRole("ASESOR")

                        .anyExchange().authenticated()
                )

                // 401/403 como JSON
                .exceptionHandling(h -> h
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                        .accessDeniedHandler(insufficientPermissions())
                )
                // Resource Server con tu converter y tu entry point
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                )

                .build();
    }

    // 403 como JSON (sin body por defecto si no lo pones)
    @Bean
    public ServerAccessDeniedHandler insufficientPermissions() {
        return (exchange, ex) -> {
            var res = exchange.getResponse();
            if (res.isCommitted()) return Mono.empty();
            res.setStatusCode(HttpStatus.FORBIDDEN);
            res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            HashMap<String, String> data = new HashMap<>();
            data.put("reason", ex.getMessage());
            GeneralResponseDTO<String> forbiddenResponse = GeneralResponseDTO.<String>builder()
                    .success(false)
                    .message("Forbidden.")
                    .data(data)
                    .build();
            var buf = res.bufferFactory().wrap(new Gson().toJson(forbiddenResponse).getBytes(StandardCharsets.UTF_8));
            return res.writeWith(Mono.just(buf));
        };
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${security.jwt.secret}") String secret
    ) {
        var key = new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        var decoder = NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        var timestamp = new JwtTimestampValidator(java.time.Duration.ZERO);

        // Usa SOLO estos (no uses createDefaultWithIssuer, que trae skew ~60s)
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp));
        return decoder;
    }

    @Bean
    public ServerAuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (exchange, ex) -> {
            var res = exchange.getResponse();
            if (res.isCommitted()) return Mono.empty();

            // Header estándar OAuth2 (útil para clientes)
            res.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

            res.setStatusCode(HttpStatus.UNAUTHORIZED);
            res.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String desc = "Invalid or missing bearer token";
            if (ex instanceof OAuth2AuthenticationException oae) {
                desc = Optional.ofNullable(oae.getError()).map(err -> err.getDescription()).orElse(desc);
            } else if (ex.getCause() instanceof OAuth2AuthenticationException cause) {
                desc = Optional.ofNullable(cause.getError()).map(err -> err.getDescription()).orElse(desc);
            } else if (ex.getCause() != null) {
                desc = ex.getCause().getMessage();
            }
            HashMap<String, String> data = new HashMap<>();
            data.put("reason", desc);
            GeneralResponseDTO<String> unauthorizedResponse = GeneralResponseDTO.<String>builder()
                    .success(false)
                    .message("Unauthorized, token not valid.")
                    .data(data)
                    .build();

            var buf = res.bufferFactory().wrap(new Gson().toJson(unauthorizedResponse).getBytes(StandardCharsets.UTF_8));
            return res.writeWith(Mono.just(buf));
        };
    }
}

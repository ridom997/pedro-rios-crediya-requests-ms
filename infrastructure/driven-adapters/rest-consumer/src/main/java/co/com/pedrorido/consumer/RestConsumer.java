package co.com.pedrorido.consumer;

import co.com.pedrorido.model.external.User;
import co.com.pedrorido.model.external.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Log4j2
public class RestConsumer implements UserRepository {
    private final WebClient client;

    @Override
    public Mono<Boolean> userExistsByDocumentNumber(String documentNumber, String email) {
        log.info("WebClient - userExistsByDocumentNumber: {}", documentNumber);
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .switchIfEmpty(Mono.error(new IllegalStateException("missing_jwt_in_context")))
                .flatMap(jwtAuth -> {
                    Jwt jwt = jwtAuth.getToken();
                    String claimDoc = firstNonBlankClaim(jwt, "id");
                    String claimEmail = firstNonBlankClaim(jwt,  "sub");

                    if (claimDoc == null) {
                        return Mono.error(new IllegalStateException("missing_document_claim_in_jwt"));
                    }
                    if (!normalize(documentNumber).equals(normalize(claimDoc)) || !normalize(email).equals(normalize(claimEmail))) {
                        return Mono.error(new AccessDeniedException("Only can create requests for the user that issued the token"));
                    }

                    String token = jwt.getTokenValue();

                    return client.get()
                            .uri(uri -> uri.path("/api/v1/users/exist")
                                    .queryParam("documentNumber", documentNumber)
                                    .build())
                            .headers(h -> {
                                h.remove(HttpHeaders.AUTHORIZATION);
                                h.setBearerAuth(token);
                            })
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<GeneralResponseDTO<Boolean>>() {
                            })
                            .map(resp -> resp != null
                                    && resp.getData() != null
                                    && Boolean.TRUE.equals(resp.getData().get("userExists")));
                })
                .onErrorResume(ex -> (ex instanceof AccessDeniedException)
                        ? Mono.error(ex)
                        : Mono.just(false)
                );
    }

    @Override
    public Mono<User> getUserByEmail(String email) {
        log.info("WebClient - getUserByEmail: {}", email);
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .switchIfEmpty(Mono.error(new IllegalStateException("missing_jwt_in_context")))
                .flatMap(jwtAuth -> {
                    Jwt jwt = jwtAuth.getToken();
                    String token = jwt.getTokenValue();

                    return client.get()
                            .uri(uri -> uri.path("/api/v1/admin/users/" + email)
                                    .build())
                            .headers(h -> {
                                h.remove(HttpHeaders.AUTHORIZATION);
                                h.setBearerAuth(token);
                            })
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<GeneralResponseDTO<User>>() {
                            })
                            .map(resp -> {
                                return resp.getData().get("user");
                            });
                })
                .onErrorResume(ex ->
                        (ex instanceof AccessDeniedException)
                                ? Mono.error(ex)
                                : Mono.empty()

                );
    }

    private static String firstNonBlankClaim(Jwt jwt, String... names) {
        for (String n : names) {
            Object v = jwt.getClaim(n);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) return s;
            }
        }
        return null;
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", "").trim();
    }
}

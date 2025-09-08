package co.com.pedrorido.api.auth;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Component
public class JwtAuthConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private final ReactiveJwtAuthenticationConverter delegate;

    public JwtAuthConverter() {
        this.delegate = new ReactiveJwtAuthenticationConverter();
        this.delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<String> raws = new HashSet<>();

            Object rolesClaim = jwt.getClaims().get("roles");
            if (rolesClaim instanceof Collection<?> coll) {
                for (Object o : coll) {
                    var r = Objects.toString(o, "").trim();
                    if (!r.isEmpty()) raws.add(r.startsWith("ROLE_") ? r : "ROLE_" + r);
                }
            }

            String scope = Objects.toString(
                    jwt.getClaims().getOrDefault("scope", jwt.getClaims().getOrDefault("scp", "")),
                    ""
            ).trim();
            if (!scope.isEmpty()) {
                for (String s : scope.split("\\s+")) if (!s.isBlank()) raws.add("SCOPE_" + s);
            }

            List<GrantedAuthority> authorities = raws.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return Flux.fromIterable(authorities); // <- Publisher requerido por WebFlux
        });
    }

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        return delegate.convert(jwt);
    }
}
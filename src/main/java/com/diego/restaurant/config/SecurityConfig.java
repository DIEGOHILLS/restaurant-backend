package com.diego.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/photos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/**").permitAll()

                        // reviews → logged in users only
                        .requestMatchers(HttpMethod.POST, "/api/reviews/**").hasRole("User")

                        // restaurants → admin only
                        .requestMatchers(HttpMethod.POST, "/api/restaurants/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/restaurants/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/restaurants/**").hasRole("Admin")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();

    jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");

            authorities.addAll(
                roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .toList()
            );
        }

        return authorities;
    });

    return jwtConverter;
}

package pl.edu.agh.backend.security.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import pl.edu.agh.backend.security.handler.BffAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) {

        // session read when it exists (web), not created for mobile
        // CSRF enabled for web sessions, ignored for mobile

        http.securityMatcher("/api/**")
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(SecurityConfig::hasBearerToken)
                        .ignoringRequestMatchers("/api/public/auth/refresh", "/api/public/auth/logout"))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/public/**")
                        .permitAll()
                        .requestMatchers("/api/me")
                        .authenticated()
                        .requestMatchers("/api/alumni/**")
                        .hasRole("VERIFIED_ALUMN")
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .hasRole("USER"))
                // we as resource server validate JWT by Keycloak JWKS, if SecurityContext is filled
                // up from session,
                // that is skipped
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(401, "Unauthorized")));

        return http.build();
    }

    // Chain 2 — handles the rest of the traffic that Chain 1 didn't intercept /**  (Web
    // login/logout, Swagger, static paths),
    // direct access to discord authn flow via oauth2Login with DiscordByPassResolver
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizationRequestResolver discordBypassResolver,
            BffAuthenticationSuccessHandler bffHandler,
            JwtDecoder jwtDecoder) {

        OidcClientInitiatedLogoutSuccessHandler oidcLogout =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcLogout.setPostLogoutRedirectUri("{baseUrl}/api/public/auth/post-logout");

        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/",
                                "/login/**",
                                "/error",
                                "/oauth2/authorization/**",
                                "/login/oauth2/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2Login(oauth2 -> oauth2.authorizationEndpoint(
                                ep -> ep.authorizationRequestResolver(discordBypassResolver))
                        .userInfoEndpoint(ui -> ui.oidcUserService(oidcUserService(jwtDecoder)))
                        .successHandler(bffHandler))
                .logout(logout -> logout.logoutRequestMatcher(
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/logout"))
                        .logoutSuccessHandler(oidcLogout)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN"));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtRoleConverter());
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    /** Web (oauth2Login) — OidcUserService is an AT roles reader */
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(JwtDecoder atDecoder) {
        OidcUserService delegate = new OidcUserService();
        return request -> {
            OidcUser oidcUser = delegate.loadUser(request);
            Jwt at = atDecoder.decode(request.getAccessToken().getTokenValue());
            Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());
            authorities.addAll(extractRealmRoles(at.getClaims()));
            String nameAttr = request.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();

            return new DefaultOidcUser(
                    List.copyOf(authorities), oidcUser.getIdToken(), oidcUser.getUserInfo(), nameAttr);
        };
    }

    /**
     * Extracts roles from the {@code realm_access.roles} claim. Mapping: "verified-alumn" -> ROLE_VERIFIED_ALUMN (dash
     * -> underscore). Used by both converters (Web and Mobile).
     */
    public static Set<GrantedAuthority> extractRealmRoles(Map<String, Object> claims) {
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
        if (realmAccess == null) {
            return Set.of();
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority(
                        "ROLE_" + r.toUpperCase(Locale.ROOT).replace("-", "_")))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * CSRF predicate - does the request carry the Authorization: Bearer header? Bearer is CSRF-safe by nature (not
     * automatically sent by the browser).
     */
    static boolean hasBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ");
    }

    /**
     * Internal converter for the Resource Server - delegates to extractRealmRoles. Used by
     * jwtAuthenticationConverter().
     */
    static class KeycloakJwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            return extractRealmRoles(jwt.getClaims());
        }
    }
}

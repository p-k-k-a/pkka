package pl.edu.agh.backend.security.resolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Configuration
@RequiredArgsConstructor
public class DiscordBypassRequestResolver {
    @Bean
    public OAuth2AuthorizationRequestResolver discordBypassResolver(
            ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return applyIdpHint(request, resolver.resolve(request));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(
                    HttpServletRequest request, String clientRegistrationId) {
                return applyIdpHint(request, resolver.resolve(request, clientRegistrationId));
            }

            private OAuth2AuthorizationRequest applyIdpHint(
                    HttpServletRequest request, OAuth2AuthorizationRequest authReq) {
                if (authReq == null) {
                    return null;
                }

                String idpHint = request.getParameter("idp");

                if ("discord".equalsIgnoreCase(idpHint)) {
                    return OAuth2AuthorizationRequest.from(authReq)
                            .additionalParameters(params -> params.put("kc_idp_hint", "discord"))
                            .build();
                }

                return authReq;
            }
        };
    }
}

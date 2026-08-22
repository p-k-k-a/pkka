package pl.edu.agh.backend.security.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class DiscordBypassRequestResolverTest {

    private OAuth2AuthorizationRequestResolver resolver;

    @BeforeEach
    void setUp() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak-web")
                .clientId("pkka-web")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("http://localhost/auth")
                .tokenUri("http://localhost/token")
                .issuerUri("http://localhost/realms/pkka")
                .build();
        ClientRegistrationRepository repo = new InMemoryClientRegistrationRepository(registration);
        resolver = new DiscordBypassRequestResolver().discordBypassResolver(repo);
    }

    @Test
    void addsKeycloakIdpHintWhenDiscordRequested() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/keycloak-web");
        request.setParameter("idp", "discord");

        OAuth2AuthorizationRequest authRequest = resolver.resolve(request, "keycloak-web");

        assertThat(authRequest).isNotNull();
        assertThat(authRequest.getAdditionalParameters()).containsEntry("kc_idp_hint", "discord");
    }

    @Test
    void leavesRequestUnchangedWithoutDiscordHint() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/keycloak-web");

        OAuth2AuthorizationRequest authRequest = resolver.resolve(request, "keycloak-web");

        assertThat(authRequest).isNotNull();
        assertThat(authRequest.getAdditionalParameters()).doesNotContainKey("kc_idp_hint");
    }
}

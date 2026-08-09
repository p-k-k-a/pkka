package pl.edu.agh.backend.security.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;
import pl.edu.agh.backend.user.UserProvisioningService;

@ExtendWith(MockitoExtension.class)
class BffAuthenticationSuccessHandlerTest {

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private UserProvisioningService userProvisioningService;

    @InjectMocks
    private BffAuthenticationSuccessHandler handler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "mobileDeepLinkScheme", "myapp");
        ReflectionTestUtils.setField(handler, "webSuccessUrl", "http://localhost:3000/dashboard");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    void webLoginSyncsIdentityAndRedirectsToSuccessUrl() throws Exception {
        OidcUser oidcUser = oidcUser("kc-web-user");
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, List.of(), "keycloak-web");

        handler.onAuthenticationSuccess(request, response, auth);

        verify(userProvisioningService).syncIdentityFromClaims("kc-web-user", "Jan", "Kowalski", "jan@example.com");
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/dashboard");
    }

    @Test
    void mobileLoginRedirectsToDeepLinkWithTokensAndInvalidatesSession() throws Exception {
        OidcUser oidcUser = oidcUser("kc-mobile-user");
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, List.of(), "keycloak-mobile");

        HttpSession session = request.getSession(true);
        session.setAttribute("marker", "present");

        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        when(client.getAccessToken())
                .thenReturn(new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "access-token-value",
                        Instant.now(),
                        Instant.now().plusSeconds(3600)));
        when(client.getRefreshToken()).thenReturn(new OAuth2RefreshToken("refresh-token-value", Instant.now()));
        when(authorizedClientService.loadAuthorizedClient("keycloak-mobile", auth.getName()))
                .thenReturn(client);

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).startsWith("myapp:///login#at=");
        assertThat(response.getRedirectedUrl()).contains("rt=");
        verify(authorizedClientService).removeAuthorizedClient("keycloak-mobile", auth.getName());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void mobileLoginReturns500WhenAuthorizedClientMissing() throws Exception {
        OidcUser oidcUser = oidcUser("kc-mobile-user");
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, List.of(), "keycloak-mobile");
        when(authorizedClientService.loadAuthorizedClient("keycloak-mobile", auth.getName()))
                .thenReturn(null);

        handler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getStatus()).isEqualTo(500);
    }

    private static OidcUser oidcUser(String subject) {
        OidcIdToken idToken = new OidcIdToken(
                "id-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of(
                        "sub", subject,
                        "email", "jan@example.com",
                        "given_name", "Jan",
                        "family_name", "Kowalski"));
        return new DefaultOidcUser(List.of(), idToken);
    }
}

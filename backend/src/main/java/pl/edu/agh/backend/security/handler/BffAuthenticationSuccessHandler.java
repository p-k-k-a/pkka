package pl.edu.agh.backend.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pl.edu.agh.backend.user.UserPrincipalExtractor;
import pl.edu.agh.backend.user.UserProvisioningService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class BffAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserProvisioningService userProvisioningService;
    private final UserPrincipalExtractor principalExtractor;

    @Value("${app.mobile.deep-link-scheme}")
    private String mobileDeepLinkScheme;

    @Value("${app.web.success-url}")
    private String webSuccessUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res, Authentication auth) throws IOException, ServletException {

        principalExtractor.extract(auth).ifPresent(userProvisioningService::provisionIfAbsent);

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
        String registrationId = token.getAuthorizedClientRegistrationId();

        if ("keycloak-mobile".equals(registrationId)) {
            handleMobile(req, res, token);
        } else {
            new SimpleUrlAuthenticationSuccessHandler(webSuccessUrl)
                    .onAuthenticationSuccess(req, res, auth);
        }
    }

    private void handleMobile(HttpServletRequest req, HttpServletResponse res,
                              OAuth2AuthenticationToken token) throws IOException {

        String regId = token.getAuthorizedClientRegistrationId();

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(regId, token.getName());

        if (client == null || client.getAccessToken() == null) {
            res.sendError(500, "Failed to retrieve access token");
            return;
        }

        String at = client.getAccessToken().getTokenValue();
        String rt = client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : null;

        authorizedClientService.removeAuthorizedClient(regId, token.getName()); // mobile stateless approach

        SecurityContextHolder.clearContext();

        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        StringBuilder link = new StringBuilder(mobileDeepLinkScheme)
                .append("://auth-success#at=")
                .append(URLEncoder.encode(at, StandardCharsets.UTF_8));

        if (rt != null) link.append("&rt=")
                .append(URLEncoder.encode(rt, StandardCharsets.UTF_8));

        log.debug("Mobile auth success — redirecting to deep link for user: {}", token.getName());
        res.sendRedirect(link.toString());
    }
}

package pl.edu.agh.backend.security.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/auth")
public class WebLogoutRedirectController {

    private final String webLogoutRedirectUrl;

    public WebLogoutRedirectController(@Value("${app.web.logout-redirect-url}") String webLogoutRedirectUrl) {
        this.webLogoutRedirectUrl = webLogoutRedirectUrl;
    }

    @GetMapping("/post-logout")
    public void postLogout(HttpServletResponse response) throws IOException {
        response.sendRedirect(webLogoutRedirectUrl);
    }
}

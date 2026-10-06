package pl.edu.agh.backend.security.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

class CsrfCookieFilterTest {

    private final CsrfCookieFilter filter = new CsrfCookieFilter();

    @Test
    void rendersDeferredCsrfTokenBeforeContinuingChain() throws Exception {
        CsrfToken token = mock(CsrfToken.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CsrfToken.class.getName(), token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(token).getToken();
        verify(chain).doFilter(request, response);
    }

    @Test
    void continuesChainWhenNoCsrfTokenPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}

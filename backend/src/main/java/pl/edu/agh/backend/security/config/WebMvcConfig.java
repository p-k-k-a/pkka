package pl.edu.agh.backend.security.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.resolver.CallerArgumentResolver;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    static {
        // Caller comes from the security context, not from the request, so it is not an API parameter.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(Caller.class);
    }

    private final CallerArgumentResolver callerArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(callerArgumentResolver);
    }
}

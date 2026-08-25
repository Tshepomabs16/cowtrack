package com.cowtrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the bundled React application.
 *
 * <p>The client uses browser history routing, so a deep link such as
 * {@code /dashboard} arrives at the server as a real request for a file that does
 * not exist. Anything that is not an actual static asset and not an API path is
 * therefore answered with {@code index.html}, letting the router take over.
 *
 * <p>API paths are deliberately excluded so that an unknown {@code /api/...} URL
 * still returns a 404 instead of silently handing back the HTML shell.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String STATIC_ROOT = "classpath:/static/";
    private static final String INDEX = "/static/index.html";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(STATIC_ROOT)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location)
                            throws IOException {

                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        ClassPathResource index = new ClassPathResource(INDEX);
                        // When the frontend has not been bundled (mvn -DskipFrontend),
                        // fall through rather than pretending to serve a page.
                        return index.exists() ? index : null;
                    }
                });
    }
}

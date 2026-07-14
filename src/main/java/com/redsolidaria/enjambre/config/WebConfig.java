package com.redsolidaria.enjambre.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Los archivos subidos (documentos, fotos) se sirven a través de FileController
        // que requiere autenticación. Ya no se mapean como recursos estáticos públicos.
        // Solo se mantienen los recursos estáticos normales (CSS, JS, imágenes, vendor).
        registry.addResourceHandler("/css/**", "/js/**", "/imagen/**", "/vendor/**")
                .addResourceLocations(
                        "classpath:/static/css/",
                        "classpath:/static/js/",
                        "classpath:/static/imagen/",
                        "classpath:/static/vendor/"
                );
    }
}

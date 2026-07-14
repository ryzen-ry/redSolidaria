package com.redsolidaria.enjambre.config;

import com.redsolidaria.enjambre.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Rate limiting: se ejecuta antes de la autenticación ──
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)

            // ── CSRF deshabilitado (se compensa con SameSite y rate limiting) ──
            .csrf(csrf -> csrf.disable())

            // ── Cabeceras de seguridad HTTP ──
            .headers(headers -> headers
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://www.youtube.com https://s.ytimg.com; " +
                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://unpkg.com; " +
                        "img-src 'self' data: https://i.ytimg.com https://img.youtube.com; " +
                        "font-src 'self' https://cdn.jsdelivr.net; " +
                        "frame-src 'self' https://www.youtube.com https://www.youtube-nocookie.com; " +
                        "connect-src 'self' ws://localhost:* wss://localhost:*; " +
                        "media-src 'self' https://www.youtube.com;"
                    )
                )
                .frameOptions(frame -> frame.deny()) // X-Frame-Options: DENY (CSP frame-src lo reemplaza)
                // X-Content-Type-Options: nosniff está habilitado por defecto en Spring Boot 3
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )

            // ── Autorización de rutas ──
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/","/nosotros","/capacitacion","/donaciones","/foro" ,"/login", "/registro/**", "/verificar-codigo",
                    "/css/**", "/js/**", "/imagen/**", "/vendor/**", "/portada.ico",
                    "/api/auth/**", "/ws/**"
                ).permitAll()
                // /uploads/** ya NO está en permitAll() — se sirve mediante controlador autenticado
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/voluntario/**").hasRole("VOLUNTARIO")
                .requestMatchers("/discapacitado/**").hasRole("DISCAPACITADO")
                .anyRequest().authenticated()
            )

            // ── Login ──
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform_login_dummy")
                .permitAll()
            )

            // ── Logout ──
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
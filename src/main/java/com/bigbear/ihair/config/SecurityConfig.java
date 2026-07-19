package com.bigbear.ihair.config;

import com.bigbear.ihair.security.JwtAuthenticationFilter;
import com.bigbear.ihair.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers("/api/auth/register").hasRole("ADMIN")
                        .requestMatchers("/api/auth/change-password").authenticated()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/branding/logo").permitAll()
                        .requestMatchers("/api/branding/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/campaigns/validate").hasAnyRole("ADMIN", "SALON_OWNER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/api/salons/*/logo").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/salons/*/logo").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/salons/**")
                                .hasAnyRole("ADMIN", "SALON_OWNER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/api/salons/*/schedule")
                                .hasAnyRole("ADMIN", "SALON_OWNER")
                        .requestMatchers(HttpMethod.POST, "/api/salons/*/holidays")
                                .hasAnyRole("ADMIN", "SALON_OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/salons/*/holidays/*")
                                .hasAnyRole("ADMIN", "SALON_OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/salons/*/holidays/*")
                                .hasAnyRole("ADMIN", "SALON_OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/salons/*/settings/**")
                                .hasAnyRole("ADMIN", "SALON_OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/salons/*/settings/**")
                                .hasAnyRole("ADMIN", "SALON_OWNER")
                        .requestMatchers("/api/salons/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/employees/**", "/api/hair-services/**")
                                .hasAnyRole("ADMIN", "SALON_OWNER", "EMPLOYEE")
                        .requestMatchers("/api/employees/**", "/api/hair-services/**")
                                .hasAnyRole("ADMIN", "SALON_OWNER")
                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "SALON_OWNER", "EMPLOYEE")
                        .requestMatchers("/api/appointments/**").hasAnyRole("ADMIN", "SALON_OWNER", "EMPLOYEE")
                        .requestMatchers("/api/sales/**").hasAnyRole("ADMIN", "SALON_OWNER", "EMPLOYEE")
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "SALON_OWNER")
                        .requestMatchers("/api/campaigns/**").hasAnyRole("ADMIN", "SALON_OWNER")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\","
                                            + "\"code\":\"AUTHENTICATION_REQUIRED\","
                                            + "\"message\":\"Kimlik doğrulaması gereklidir.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"Forbidden\","
                                            + "\"code\":\"ACCESS_DENIED\","
                                            + "\"message\":\"Bu işlem için yetkiniz bulunmuyor.\"}");
                        })
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}

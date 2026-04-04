package com.bonchang.qerp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(OpsUserDetailsService opsUserDetailsService) {
        return opsUserDetailsService;
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorrelationIdFilter correlationIdFilter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg").permitAll()
                        .requestMatchers(
                                "/architecture",
                                "/discover",
                                "/stocks/*",
                                "/portfolio",
                                "/portfolio/orders/*",
                                "/orders",
                                "/quant",
                                "/quant/strategies/*",
                                "/profile",
                                "/research",
                                "/research/*",
                                "/console",
                                "/console/orders/*",
                                "/console/research-link"
                        ).permitAll()
                        .requestMatchers("/auth/token").permitAll()
                        .requestMatchers(HttpMethod.POST, "/app/auth/guest").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/prometheus").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/research/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/app/home", "/app/discover", "/app/stocks/**", "/app/quant/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/app/me", "/app/portfolio", "/app/orders", "/app/orders/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/app/orders", "/app/orders/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.GET, "/dashboard/**", "/orders/**", "/accounts/**", "/strategy-runs/**", "/market-data/**").hasAnyRole("ADMIN", "TRADER", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/orders", "/orders/*/cancel", "/strategy-runs").hasAnyRole("ADMIN", "TRADER")
                        .requestMatchers(HttpMethod.POST, "/orders/expire-working", "/dashboard/seed-demo", "/dashboard/portfolio-snapshots/refresh", "/market-data/ingest").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

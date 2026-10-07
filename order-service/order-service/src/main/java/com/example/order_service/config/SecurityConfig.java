package com.example.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/mock-saga/**").permitAll() // <-- Already there
                        .requestMatchers("/api/v1/orders/**").permitAll()    // <-- ADD THIS LINE
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    // Create a hardcoded service user for the API Gateway to use
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails serviceUser = User.builder()
                .username("gateway-service")
                .password("{noop}super-secret-password") // {noop} means plain text password for this demo
                .roles("SERVICE")
                .build();

        return new InMemoryUserDetailsManager(serviceUser);
    }
}
package com.lonelysoul.oauth2_client_sample.request_sender.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain (HttpSecurity security) throws Exception {

        // Disable Spring Security auto-configuration without disabling OAuth2 client auto-configuration
        security.csrf((csrf) -> csrf.disable());
        security.authorizeHttpRequests ((request) -> request.anyRequest ().permitAll ());

        return security.build ();
    }
}

package com.lonelysoul.oauth2_client_sample.request_sender.configuration;

import com.lonelysoul.oauth2_client_sample.request_sender.util.OAuth2ClientHttpRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

    private static final String CLIENT_REGISTRATION_ID = "oauth2-client-sample";

    @Value("${authorization-resource-server.base-url}")
    private String authorizationResourceServerBaseUrl;

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager (
            ClientRegistrationRepository clientRegistrationRepository
            , OAuth2AuthorizedClientService authorizedClientService
    ){
        OAuth2AuthorizedClientProvider authorizedClientProvider
                = OAuth2AuthorizedClientProviderBuilder.builder ()
                        .clientCredentials ()
                        .build ();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager
                = new AuthorizedClientServiceOAuth2AuthorizedClientManager (
                        clientRegistrationRepository, authorizedClientService
                );
        authorizedClientManager.setAuthorizedClientProvider (authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    public RestClient constructRestClient (
            OAuth2AuthorizedClientManager authorizedClientManager
            , OAuth2AuthorizedClientRepository authorizedClientRepository
    ){
        ClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory ();
        OAuth2ClientHttpRequestInterceptor requestInterceptor
                = new OAuth2ClientHttpRequestInterceptor (authorizedClientManager, CLIENT_REGISTRATION_ID);
        requestInterceptor.setBaseUrl (authorizationResourceServerBaseUrl);

        // Configure the interceptor to remove invalid authorized clients
        requestInterceptor.setAuthorizedClientRepository (authorizedClientRepository);

        return RestClient.builder ()
                .requestFactory (requestFactory)
                .requestInterceptor (requestInterceptor)
                .build ();
    }
}

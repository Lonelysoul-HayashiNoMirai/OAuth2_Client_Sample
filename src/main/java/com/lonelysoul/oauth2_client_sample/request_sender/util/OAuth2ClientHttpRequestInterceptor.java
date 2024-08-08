package com.lonelysoul.oauth2_client_sample.request_sender.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.util.Assert;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.lonelysoul.oauth2_client_sample.request_sender.util.Utility.isNullOrEmpty;
import static org.springframework.http.HttpHeaders.WWW_AUTHENTICATE;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.core.context.SecurityContextHolder.getContextHolderStrategy;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.*;
import static org.springframework.util.StringUtils.*;
import static org.springframework.web.context.request.RequestContextHolder.getRequestAttributes;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Setter
public final class OAuth2ClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    // @formatter:off
	private static final Map<HttpStatusCode, String> OAUTH2_ERROR_CODES = Map.of (
			HttpStatus.UNAUTHORIZED
			, OAuth2ErrorCodes.INVALID_TOKEN
			, HttpStatus.FORBIDDEN
			, OAuth2ErrorCodes.INSUFFICIENT_SCOPE
	);
	// @formatter:on

    private static final Authentication ANONYMOUS_AUTHENTICATION 
            = new AnonymousAuthenticationToken (
                    "anonymous"
                    , "anonymousUser"
                    , createAuthorityList ("ROLE_ANONYMOUS")
    );

    private String baseUrl;

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final String clientRegistrationId;

    // @formatter:off
	private OAuth2AuthorizationFailureHandler authorizationFailureHandler 
            = (clientRegistrationId, principal, attributes) -> { };
	// @formatter:on

    private SecurityContextHolderStrategy securityContextHolderStrategy = getContextHolderStrategy ();

    private Supplier<Authentication> authentication 
            = () -> this.securityContextHolderStrategy.getContext ().getAuthentication ();

    public OAuth2ClientHttpRequestInterceptor (
            OAuth2AuthorizedClientManager authorizedClientManager, String clientRegistrationId
    ){
        Assert.notNull (authorizedClientManager, "authorizedClientManager cannot be null");
        Assert.hasText (clientRegistrationId, "clientRegistrationId cannot be empty");
        this.authorizedClientManager = authorizedClientManager;
        this.clientRegistrationId = clientRegistrationId;
    }

    public void setAuthorizationFailureHandler (OAuth2AuthorizationFailureHandler authorizationFailureHandler){
        Assert.notNull (authorizationFailureHandler, "authorizationFailureHandler cannot be null");
        this.authorizationFailureHandler = authorizationFailureHandler;
    }

    public void setAuthorizedClientRepository (OAuth2AuthorizedClientRepository authorizedClientRepository){
        Assert.notNull (authorizedClientRepository, "authorizedClientRepository cannot be null");
        this.authorizationFailureHandler = new RemoveAuthorizedClientOAuth2AuthorizationFailureHandler (
                (clientRegistrationId, principal, attributes) -> removeAuthorizedClient (
                        authorizedClientRepository, clientRegistrationId, principal, attributes
                )
        );
    }

    private static void removeAuthorizedClient (
            OAuth2AuthorizedClientRepository authorizedClientRepository
            , String clientRegistrationId
            , Authentication principal
            , Map<String, Object> attributes
    ){
        HttpServletRequest request = (HttpServletRequest) attributes.get (HttpServletRequest.class.getName ());
        HttpServletResponse response = (HttpServletResponse) attributes.get (HttpServletResponse.class.getName ());
        authorizedClientRepository.removeAuthorizedClient (clientRegistrationId, principal, request, response);
    }

    public void setAuthorizedClientService (OAuth2AuthorizedClientService authorizedClientService){
        Assert.notNull (authorizedClientService, "authorizedClientService cannot be null");
        this.authorizationFailureHandler = new RemoveAuthorizedClientOAuth2AuthorizationFailureHandler (
                (clientRegistrationId, principal, attributes) -> removeAuthorizedClient (
                        authorizedClientService, clientRegistrationId, principal
                )
        );
    }

    private static void removeAuthorizedClient (
            OAuth2AuthorizedClientService authorizedClientService
            , String clientRegistrationId
            , Authentication principal
    ){
        authorizedClientService.removeAuthorizedClient (clientRegistrationId, principal.getName ());
    }

    public void setSecurityContextHolderStrategy (SecurityContextHolderStrategy securityContextHolderStrategy){
        Assert.notNull (securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
        this.securityContextHolderStrategy = securityContextHolderStrategy;
    }

    public void setPrincipalName (String principalName){
        Assert.hasText (principalName, "principalName cannot be empty");
        Authentication principal = createAuthentication (principalName);
        this.authentication = () -> principal;
    }

    public void setPrincipal (Authentication principal){
        Assert.notNull (principal, "principal cannot be null");
        this.authentication = () -> principal;
    }

    public Consumer<ClientHttpRequest> httpRequest (){
        return this::authorizeClient;
    }

    public ResponseErrorHandler errorHandler (){
        
        return new DefaultResponseErrorHandler () {
            
            @Override
            public void handleError (URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
                handleAuthorizationFailure (response.getHeaders (), response.getStatusCode ());
                super.handleError (url, method, response);
            }
        };
    }

    @Override
    public ClientHttpResponse intercept (
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution
    ) throws IOException {

        if (!isNullOrEmpty (baseUrl)){
            request = prependBaseUrl (request);
        }
        authorizeClient (request);

        try {
            ClientHttpResponse response = execution.execute (request, body);
            handleAuthorizationFailure (response.getHeaders (), response.getStatusCode ());
            return response;
        }
        catch (RestClientResponseException exception){
            handleAuthorizationFailure (exception.getResponseHeaders (), exception.getStatusCode ());
            throw exception;
        }
        catch (OAuth2AuthorizationException exception){
            handleAuthorizationFailure (exception);
            throw exception;
        }
    }

    private HttpRequest prependBaseUrl (HttpRequest request){
        URI completeUri = fromHttpUrl (baseUrl)
                .path (request.getURI ().getPath ())
                .query (request.getURI ().getQuery ())
                .build (true)
                .toUri ();

        return new HttpRequestWrapper (request){

            @Override
            public @NonNull URI getURI (){
                return completeUri;
            }
        };
    }

    private void authorizeClient (HttpRequest request){
        Authentication principal = this.authentication.get ();

        if (principal == null){
            principal = ANONYMOUS_AUTHENTICATION;
        }
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId (this.clientRegistrationId)
                .principal (principal)
                .build ();

        OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize (authorizeRequest);

        if (authorizedClient != null){
            request.getHeaders ().setBearerAuth (authorizedClient.getAccessToken ().getTokenValue ());
        }
    }

    private void handleAuthorizationFailure (HttpHeaders headers, HttpStatusCode httpStatus){
        OAuth2Error error = resolveOAuth2ErrorIfPossible (headers, httpStatus);
        if (error == null){
            return;
        }

        ClientAuthorizationException authorizationException
                = new ClientAuthorizationException (error, this.clientRegistrationId);
        handleAuthorizationFailure (authorizationException);
    }

    private static OAuth2Error resolveOAuth2ErrorIfPossible (HttpHeaders headers, HttpStatusCode httpStatus){
        String wwwAuthenticateHeader = headers.getFirst (WWW_AUTHENTICATE);

        if (wwwAuthenticateHeader != null){
            Map<String, String> parameters = parseWwwAuthenticateHeader (wwwAuthenticateHeader);

            if (parameters.containsKey (ERROR)){
                return new OAuth2Error (
                        parameters.get (ERROR), parameters.get (ERROR_DESCRIPTION), parameters.get (ERROR_URI)
                );
            }
        }
        String errorCode = OAUTH2_ERROR_CODES.get (httpStatus);

        if (errorCode != null){
            return new OAuth2Error (
                    errorCode, null, "https://tools.ietf.org/html/rfc6750#section-3.1"
            );
        }

        return null;
    }

    private static Map<String, String> parseWwwAuthenticateHeader (String wwwAuthenticateHeader){

        if (!hasLength (wwwAuthenticateHeader) || !startsWithIgnoreCase (wwwAuthenticateHeader, "bearer")){
            return Map.of ();
        }

        String headerValue = wwwAuthenticateHeader.substring ("bearer".length ()).stripLeading ();
        Map<String, String> parameters = new HashMap<> ();

        for (String kvPair : delimitedListToStringArray (headerValue, ",")){
            String[] kv = split (kvPair, "=");

            if (kv == null || kv.length <= 1){
                continue;
            }
            parameters.put (kv[0].trim (), kv[1].trim ().replace ("\"", ""));
        }

        return parameters;
    }

    private void handleAuthorizationFailure (OAuth2AuthorizationException authorizationException){
        Authentication principal = this.authentication.get ();

        if (principal == null){
            principal = ANONYMOUS_AUTHENTICATION;
        }

        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) getRequestAttributes ();
        Map<String, Object> attributes = new HashMap<> ();

        if (requestAttributes != null){
            attributes.put (HttpServletRequest.class.getName (), requestAttributes.getRequest ());

            if (requestAttributes.getResponse () != null){
                attributes.put (HttpServletResponse.class.getName (), requestAttributes.getResponse ());
            }
        }
        this.authorizationFailureHandler.onAuthorizationFailure (authorizationException, principal, attributes);
    }

    private static Authentication createAuthentication (final String principalName){
        Assert.hasText (principalName, "principalName cannot be empty");

        return new AbstractAuthenticationToken (null) {

            @Override
            public Object getPrincipal (){
                return principalName;
            }

            @Override
            public Object getCredentials (){
                return "";
            }
        };
    }
}

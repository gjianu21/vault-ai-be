package com.vaultai.be.config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import lombok.RequiredArgsConstructor;

/**
 * BFF security: the browser only has the VAULT-SESSION cookie (HttpOnly).
 * Tokens from the auth-server stay in the server-side session (Oracle).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String REGISTRATION_ID = "vault-ai";

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/csrf",
            "/error",
            "/actuator/health"
    };

    /** Enabled only on the "local" profile; on other profiles these paths do not exist. */
    private static final String[] DEV_TOOL_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final AppProperties properties;
    private final LoginSuccessHandler loginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();

        http
                // /internal/** is called only by the auth-server, with the shared API key
                .addFilterBefore(new InternalApiKeyFilter(properties.internalApiKey()), AuthorizationFilter.class)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/internal/**"))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(DEV_TOOL_PATHS).permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(pkceResolver(clientRegistrationRepository)))
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
                        .successHandler(loginSuccessHandler)
                        .failureUrl(properties.frontendUrl() + "/?loginError"))
                .logout(logout -> logout
                        .logoutRequestMatcher(paths.matcher(HttpMethod.POST, "/api/auth/logout"))
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository)))
                // APIs answer 401 (the SPA decides what to do); pages are redirected to the login
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        paths.matcher("/api/**")));

        return http.build();
    }

    private OAuth2AuthorizationRequestResolver pkceResolver(ClientRegistrationRepository repository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    /** Takes the roles from the "roles" claim of the ID token. The principal name is "sub" (the user id). */
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return request -> {
            OidcUser oidcUser = delegate.loadUser(request);

            List<String> roles = oidcUser.getClaimAsStringList("roles");
            Set<GrantedAuthority> authorities = (roles == null ? List.<String>of() : roles).stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                    .collect(Collectors.toSet());

            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(),
                    IdTokenClaimNames.SUB);
        };
    }

    /** Logs out from the auth-server too, then comes back to the frontend. */
    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository repository) {
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(repository);
        handler.setPostLogoutRedirectUri(properties.frontendUrl());
        return handler;
    }

    public static String loginUrl() {
        return "/oauth2/authorization/" + REGISTRATION_ID;
    }
}
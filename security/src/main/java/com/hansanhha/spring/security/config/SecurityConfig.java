package com.hansanhha.spring.security.config;

import com.hansanhha.spring.security.oauth2.client.ClientOAuth2UserService;
import com.hansanhha.spring.security.oauth2.client.OAuth2AuthenticationSuccessHandler;
import com.hansanhha.spring.security.token.jwt.JwtTokenService;
import com.hansanhha.spring.security.token.jwt.authentication.JwtBearerAuthenticationFilter;
import com.hansanhha.spring.security.user.repository.UserRepository;
import com.hansanhha.spring.security.util.RequestMatcherRegistry;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final List<String> KAKAO_OAUTH_URL =
            List.of("/oauth2/authorization/kakao", "/login/oauth2/code/kakao", "/login/oauth2/success");

    private final ClientOAuth2UserService userService;
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final UserRepository userRepository;
    private final JwtTokenService tokenService;

    CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedHeaders(Collections.singletonList("*"));
            config.setAllowedMethods(Collections.singletonList("*"));
            config.setAllowedOriginPatterns(List.of("*"));
            config.setAllowCredentials(true);
            return config;
        };
    }

    @Bean
    public RequestMatcherRegistry requestMatcherRegistry() {
        return RequestMatcherRegistry.configureAndCreate(config -> {
            config
                .requestMatcher("/")
                .requestMatcher("/api/get-token")
                .requestMatcher("/login")
                .requestMatcher("/actuator/**")
                .requestMatcher(new DispatcherTypeRequestMatcher(DispatcherType.FORWARD))
                .requestMatcher(PathRequest.toStaticResources().atCommonLocations());
            KAKAO_OAUTH_URL.forEach(config::requestMatcher);
        });
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        RequestMatcherRegistry requestMatcherRegistry = requestMatcherRegistry();
        RequestMatcher[] requestMatchers = requestMatcherRegistry.getRequestMatchers().toArray(new RequestMatcher[0]);

        http
                .httpBasic(HttpBasicConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(requestMatchers).permitAll()
                        .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2Login -> oauth2Login
                        .loginPage("/login")
                        .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
                                .userService(userService))
                        .successHandler(successHandler)
                )
                .addFilterBefore(new JwtBearerAuthenticationFilter(userRepository, tokenService, requestMatcherRegistry), OAuth2AuthorizationRequestRedirectFilter.class);

        return http.build();
    }

}

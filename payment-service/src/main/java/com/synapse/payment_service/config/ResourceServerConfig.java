package com.synapse.payment_service.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.synapse.payment_service.filter.MemberAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorityAuthorizationManager;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

    private final MemberAuthenticationFilter memberAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityResourceServerFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityMatcher("/api/internal/**")
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().access(AuthorizationManagers.allOf(
                    AuthorityAuthorizationManager.hasAuthority("SCOPE_api.internal"),
                    AuthorityAuthorizationManager.hasAuthority("SCOPE_account:read")
                ))
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
            .addFilterBefore(memberAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            );
        return http.build();
    }
}

package com.synapse.payment_service.infrastructure;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.synapse.payment_service.config.PortOneClientProperties;
import com.synapse.payment_service.dto.PortOneAuthResponse;
import com.synapse.payment_service.dto.PortOneErrorResponse;
import com.synapse.payment_service.exception.ExceptionCode;
import com.synapse.payment_service.exception.PortOneClientException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class AbstractPortOneClient {
    private final WebClient portOneWebClient;
    private final PortOneClientProperties properties;

    private String accessToken;
    private String refreshToken;
    private Instant tokenExpiredAt;

    protected AbstractPortOneClient(WebClient portOneWebClient, PortOneClientProperties properties) {
        this.portOneWebClient = portOneWebClient;
        this.properties = properties;
    }

    protected <T> Mono<T> performGetRequest(String uri, ParameterizedTypeReference<T> responseType) {
        String accessToken = getAccessToken();

        return getPortOneWebClient().get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse
                        .bodyToMono(PortOneErrorResponse.class)
                        .flatMap(error -> Mono.error(new PortOneClientException(ExceptionCode.PORT_ONE_CLIENT_ERROR)))
                )
                .bodyToMono(responseType);
    }

    /**
     * 유효한 accessToken을 반환합니다.
     * @return accessToken
     */
    protected final String getAccessToken() {
        if(accessToken == null || tokenExpiredAt.isBefore(Instant.now().plusSeconds(60))) {
            refreshToken();
        }
        return this.accessToken;
    }

    private void refreshToken() {
        Mono<PortOneAuthResponse> authResponseMono = Mono.justOrEmpty(this.refreshToken)
                .flatMap(token -> refreshWithToken())
                .switchIfEmpty(Mono.defer(this::authenticateWithSecret));

        // 비동기 파이프라인을 실행하고 결과를 동기적으로 기다린다.
        PortOneAuthResponse response = authResponseMono
                .doOnSuccess(auth -> {
                    this.accessToken = auth.accessToken();
                    this.refreshToken = auth.refreshToken();
                    this.tokenExpiredAt = Instant.now().plus(29, ChronoUnit.MINUTES);
                    log.info("아임포트 토큰이 성공적으로 갱신되었습니다.");
                })
                .block();
        
        if (response == null || response.accessToken() == null) {
            throw new RuntimeException("아임포트 Access/Refresh Token 발급에 실패했습니다.");
        }
    }

    private Mono<PortOneAuthResponse> refreshWithToken() {
        return this.portOneWebClient.post()
                .uri("/token/refresh")
                .bodyValue(Map.of("refreshToken", this.refreshToken))
                .retrieve()
                .bodyToMono(PortOneAuthResponse.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.warn("리프레시 토큰 갱신 실패. 시크릿 키로 재인증을 시도합니다. 원인: {}", e.getMessage());
                    return authenticateWithSecret();
                });
    }

    private Mono<PortOneAuthResponse> authenticateWithSecret() {
        return this.portOneWebClient.post()
                .uri("/login/api-secret")
                .bodyValue(Map.of("apiSecret", this.properties.apiSecret()))
                .retrieve()
                .bodyToMono(PortOneAuthResponse.class);
    }

    protected WebClient getPortOneWebClient() {
        return this.portOneWebClient;
    }
}

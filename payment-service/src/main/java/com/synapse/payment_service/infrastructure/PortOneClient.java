package com.synapse.payment_service.infrastructure;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.synapse.payment_service.config.PortOneClientProperties;
import com.synapse.payment_service.dto.IamportResponse;
import com.synapse.payment_service.dto.PaymentData;

@Component
public class PortOneClient extends AbstractPortOneClient {
    public PortOneClient(WebClient portOneWebClient, PortOneClientProperties properties) {
        super(portOneWebClient, properties);
    }

    /**
     * 결제 검증용 iamport_uid로 결제 정보를 조회합니다.
     * 
     * @param impUid 포트원 거래 고유번호
     * @return PortOneResponse<PaymentData> 결제 정보
     * @throws com.synapse.payment_service.exception.PortOneClientException API 호출 실패 시
     */
    public IamportResponse<PaymentData> getPaymentData(String impUid) {
        String uri = "/payments/" + impUid;

        var responseType = new ParameterizedTypeReference<IamportResponse<PaymentData>>() {};
        return super.performGetRequest(uri, responseType).block();
    }
}

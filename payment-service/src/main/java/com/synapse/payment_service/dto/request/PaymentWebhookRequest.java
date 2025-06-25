package com.synapse.payment_service.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

public record PaymentWebhookRequest(
    String paymentId,
    String transactionId,
    String type
) {
    public static PaymentWebhookRequest from(String requestBody, ObjectMapper objectMapper) throws IOException {
        JsonNode root = objectMapper.readTree(requestBody);
        
        // data 노드가 있으면 그 안에서, 없으면 루트에서 찾기
        JsonNode dataNode = Optional.ofNullable(root.get("data")).orElse(root);
        
        return new PaymentWebhookRequest(
            extractText(dataNode, "paymentId"),
            extractText(dataNode, "transactionId"),
            extractText(root, "type")  // type은 항상 루트 레벨에 있음
        );
    }
    
    private static String extractText(JsonNode node, String fieldName) {
        return Optional.ofNullable(node.get(fieldName))
                .map(JsonNode::asText)
                .orElse(null);
    }
} 
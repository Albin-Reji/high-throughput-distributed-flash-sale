package com.project_aegis.order_service.service;

import com.project_aegis.order_service.dto.request.PaymentNotificationRequest;
import com.project_aegis.order_service.dto.response.OrderSummaryResponse;

import java.util.UUID;

public interface InternalOrderService {

    OrderSummaryResponse processPayment(UUID orderId, PaymentNotificationRequest request, String apiKey);
}

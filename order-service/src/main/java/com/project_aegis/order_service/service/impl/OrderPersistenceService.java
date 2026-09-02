package com.project_aegis.order_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_aegis.order_service.dto.response.CreateOrderResponse;
import com.project_aegis.order_service.entity.*;
import com.project_aegis.order_service.mapper.OrderMapper;
import com.project_aegis.order_service.repository.IdempotencyRecordRepository;
import com.project_aegis.order_service.repository.OrderRepository;
import com.project_aegis.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderPersistenceService {

    private static final int CREATED_STATUS_CODE = 201;
    private static final String AGGREGATE_TYPE_ORDER ="ORDER" ;
    private static final String EVENT_ORDER_CREATED = "ORDER_CREATED";

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;


    @Transactional
    public Order saveInitOrder(Order order){
        order.setStatus(OrderStatus.PENDING);
        return orderRepository.save(order);
    }


    @Transactional
    public Order confirmOrder(Order order,
                              UUID customerId,
                              String idempotencyKey
                              ){
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        Order savedOrder = orderRepository.save(order);
        //save outbox event to db
        createOrderOutboxEvent(savedOrder);


        saveIdempotencyRecord(
                idempotencyKey,
                customerId,
                savedOrder
        );
        return savedOrder;

    }
    @Transactional
    public void failOrder(Order order){
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
    }

    private void createOrderOutboxEvent(Order order) {
        String payload = serialize(
                orderMapper.toCreateResponse(order)
        );

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE_ORDER)
                .aggregateId(order.getId())
                .eventType(EVENT_ORDER_CREATED)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .build();

        outboxEventRepository.save(event);
    }

    private void saveIdempotencyRecord(
            String idempotencyKey,
            UUID customerId,
            Order order
    ) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return;
        }
        CreateOrderResponse response=orderMapper.toCreateResponse(order);

        String responseBody = serialize(response);


        IdempotencyRecord idempotencyRecord = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .customerId(customerId)
                .orderId(order.getId())
                .responseBody(responseBody)
                .statusCode(CREATED_STATUS_CODE)
                .build();

        idempotencyRecordRepository.save(idempotencyRecord);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.error("Failed to serialize outbox payload", ex);
            throw new IllegalStateException(
                    "Failed to serialize outbox event payload",
                    ex
            );
        }
    }
}

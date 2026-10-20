package com.example.store.service.delivery.impl;

/*
 * Note: Tam thoi comment toan bo class test vi GhnWebhookServiceImpl dang bi dong bang o nhanh orderService.
 * Toan bo ma nguon va logic unit test duoc giu nguyen ven 100% duoi day de phuc vu khi mo lai webhook GHN.
 */

/*
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.store.dto.ghn.GhnOrderStatusCallbackDTO;
import com.example.store.entity.GhnWebhookEvent;
import com.example.store.entity.Order;
import com.example.store.entity.OrderStatus;
import com.example.store.exception.BusinessException;
import com.example.store.repository.GhnWebhookEventRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GhnWebhookServiceImplTest {
    private static final String ORDER_CODE = "GHN123456";
    private static final String SHOP_ID = "12345";

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusRepository orderStatusRepository;
    @Mock private GhnWebhookEventRepository eventRepository;
    @InjectMocks private GhnWebhookServiceImpl service;

    private Order order;
    private OrderStatus confirmed;
    private OrderStatus delivered;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "configuredShopId", SHOP_ID);
        confirmed = OrderStatus.builder().id(2).name("CONFIRMED").build();
        delivered = OrderStatus.builder().id(4).name("DELIVERED").build();
        order = Order.builder()
                .id(UUID.randomUUID())
                .ghnOrderCode(ORDER_CODE)
                .ghnStatus("delivering")
                .status(confirmed)
                .build();
    }

    @Test
    void deliveredCallbackUpdatesBothGhnAndBusinessStatus() {
        allowNewEvent();
        when(orderStatusRepository.findAll()).thenReturn(List.of(confirmed, delivered));

        service.handle(callback("delivered", Instant.parse("2026-10-13T10:00:00Z")));

        assertEquals("delivered", order.getGhnStatus());
        assertEquals(delivered, order.getStatus());
        verify(eventRepository).saveAndFlush(any(GhnWebhookEvent.class));
        verify(orderRepository).save(order);
    }

    @Test
    void delayedCallbackIsRecordedButCannotRollBackCurrentStatus() {
        allowNewEvent();
        when(eventRepository.findFirstByOrderCodeOrderByEventTimeDesc(ORDER_CODE))
                .thenReturn(Optional.of(eventAt("2026-10-13T11:00:00Z")));

        service.handle(callback("ready_to_pick", Instant.parse("2026-10-13T10:00:00Z")));

        assertEquals("delivering", order.getGhnStatus());
        assertEquals(confirmed, order.getStatus());
        verify(eventRepository).saveAndFlush(any(GhnWebhookEvent.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void duplicateCallbackIsIdempotent() {
        Instant eventTime = Instant.parse("2026-10-13T10:00:00Z");
        when(eventRepository.existsByOrderCodeAndEventTypeAndEventTime(
                ORDER_CODE, "switch_status", eventTime)).thenReturn(true);

        service.handle(callback("delivered", eventTime));

        verify(orderRepository, never()).findByGhnOrderCodeForUpdate(any());
        verify(eventRepository, never()).saveAndFlush(any(GhnWebhookEvent.class));
        assertEquals("delivering", order.getGhnStatus());
    }

    @Test
    void callbackForAnotherShopDoesNotChangeOrder() {
        GhnOrderStatusCallbackDTO callback = callback("delivered", Instant.now());
        callback.setShopId(999);

        assertThrows(BusinessException.class, () -> service.handle(callback));

        assertEquals("delivering", order.getGhnStatus());
        assertEquals(confirmed, order.getStatus());
        verify(eventRepository, never()).saveAndFlush(any(GhnWebhookEvent.class));
    }

    @Test
    void unmappedExceptionalStatusDoesNotInventBusinessStatus() {
        allowNewEvent();

        service.handle(callback("lost", Instant.parse("2026-10-13T10:00:00Z")));

        assertEquals("lost", order.getGhnStatus());
        assertEquals(confirmed, order.getStatus());
        verify(orderStatusRepository, never()).findAll();
        verify(orderRepository).save(order);
    }

    private void allowNewEvent() {
        when(eventRepository.existsByOrderCodeAndEventTypeAndEventTime(
                any(), any(), any())).thenReturn(false);
        when(orderRepository.findByGhnOrderCodeForUpdate(ORDER_CODE)).thenReturn(Optional.of(order));
        when(eventRepository.findFirstByOrderCodeOrderByEventTimeDesc(ORDER_CODE)).thenReturn(Optional.empty());
    }

    private GhnWebhookEvent eventAt(String time) {
        GhnWebhookEvent event = new GhnWebhookEvent();
        event.setEventTime(Instant.parse(time));
        return event;
    }

    private GhnOrderStatusCallbackDTO callback(String status, Instant time) {
        GhnOrderStatusCallbackDTO callback = new GhnOrderStatusCallbackDTO();
        callback.setShopId(Integer.valueOf(SHOP_ID));
        callback.setOrderCode(ORDER_CODE);
        callback.setType("switch_status");
        callback.setStatus(status);
        callback.setTime(time);
        return callback;
    }
}
*/

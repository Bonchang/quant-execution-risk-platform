package com.bonchang.qerp.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${orders.expiry-poll-ms:60000}")
    public void expireWorkingDayOrders() {
        int expired = orderService.expireWorkingOrders().expiredCount();
        if (expired > 0) {
            log.info("expired {} DAY working orders", expired);
        }
    }
}

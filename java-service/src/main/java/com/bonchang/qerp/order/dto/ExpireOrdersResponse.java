package com.bonchang.qerp.order.dto;

public record ExpireOrdersResponse(
        int expiredCount,
        String message
) {
}

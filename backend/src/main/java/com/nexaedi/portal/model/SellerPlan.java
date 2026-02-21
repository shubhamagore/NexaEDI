package com.nexaedi.portal.model;

import lombok.Getter;

@Getter
public enum SellerPlan {
    STARTER(99, 50, 1),
    GROWTH(249, 300, 3),
    PRO(499, Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int monthlyPriceUsd;
    private final int maxOrdersPerMonth;
    private final int maxRetailers;

    SellerPlan(int monthlyPriceUsd, int maxOrdersPerMonth, int maxRetailers) {
        this.monthlyPriceUsd = monthlyPriceUsd;
        this.maxOrdersPerMonth = maxOrdersPerMonth;
        this.maxRetailers = maxRetailers;
    }
}

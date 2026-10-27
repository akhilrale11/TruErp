package com.example.TradeAI.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderBookDepth {
    private String symbol;
    private List<OrderBookLevel> bids;
    private List<OrderBookLevel> asks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderBookLevel {
        private Double price;
        private Double volume;
    }
}

package com.example.TradeAI.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tick {
    private String symbol;
    private Double bid;
    private Double ask;
    private Double lastPrice;
    private Double spread;
    private Double open;
    private Double high;
    private Double low;
    private Double previousClose;
    private Long volume;
    private Double changePercent;
    private LocalDateTime timestamp;
}

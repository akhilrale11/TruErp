package com.example.TradeAI.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private Double price;
    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closePrice;
    private Long volume;
    private Double changePercent;

    private LocalDateTime timestamp;
}

package com.example.TradeAI.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String symbol;

    @Enumerated(EnumType.STRING)
    private SignalType orderType;

    private Integer quantity;
    private Double price;
    private Double totalAmount;
    private String status;

    private LocalDateTime executedAt;
}

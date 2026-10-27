package com.example.TradeAI.service;

import com.example.TradeAI.model.SignalType;
import com.example.TradeAI.model.Tick;
import com.example.TradeAI.model.TradingSignal;
import com.example.TradeAI.repository.TradingSignalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TradingAIAgent {

    private final MarketDataService marketDataService;
    private final TechnicalIndicatorService indicatorService;
    private final SignalScoringEngine scoringEngine;
    private final TradingSignalRepository signalRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Cooldown tracking per symbol (timestamp of last generated ACTIVE signal)
    private final Map<String, Long> lastSignalTimestamp = new ConcurrentHashMap<>();
    private final Map<String, TradingSignal> latestSignalsBySymbol = new ConcurrentHashMap<>();

    // Configurable parameters
    private int minConfidenceThreshold = 70;
    private double minRR = 2.0;
    private int signalCooldownSeconds = 15;

    @Autowired
    public TradingAIAgent(MarketDataService marketDataService,
                          TechnicalIndicatorService indicatorService,
                          SignalScoringEngine scoringEngine,
                          TradingSignalRepository signalRepository,
                          @Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.marketDataService = marketDataService;
        this.indicatorService = indicatorService;
        this.scoringEngine = scoringEngine;
        this.signalRepository = signalRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void initAgent() {
        scanAllMarkets();
    }

    @Scheduled(fixedRate = 3000)
    public void continuousMarketScan() {
        scanAllMarkets();
    }

    public List<TradingSignal> scanAllMarkets() {
        List<TradingSignal> result = new ArrayList<>();
        var instruments = marketDataService.getInstruments();

        for (var inst : instruments) {
            String symbol = inst.getSymbol();
            TradingSignal signal = evaluateSymbol(symbol);
            if (signal != null) {
                latestSignalsBySymbol.put(symbol, signal);
                result.add(signal);
            }
        }

        // Broadcast scanner updates via WebSocket
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/ai-scanner", latestSignalsBySymbol.values());
        }

        return result;
    }

    public TradingSignal evaluateSymbol(String symbol) {
        Tick tick = marketDataService.getLatestTick(symbol);
        List<Double> history = marketDataService.getPriceHistory(symbol);

        if (tick == null || history == null || history.isEmpty()) {
            return null;
        }

        double rsi = indicatorService.calculateRSI(history, 14);
        double sma50 = indicatorService.calculateSMA(history, 15);
        double sma200 = indicatorService.calculateSMA(history, 30);
        String macdSignal = indicatorService.calculateMACDSignal(history);
        String orderBlock = indicatorService.detectOrderBlockZone(symbol, history);

        SignalScoringEngine.EvaluationResult eval = scoringEngine.evaluateMarket(
                symbol, tick, history, rsi, macdSignal, sma50, sma200, orderBlock,
                minConfidenceThreshold, minRR
        );

        long now = System.currentTimeMillis();
        Long lastTime = lastSignalTimestamp.getOrDefault(symbol, 0L);

        // Check signal cooldown for active BUY/SELL signals
        boolean isBuyOrSell = eval.signalType == SignalType.BUY || eval.signalType == SignalType.SELL;
        if (isBuyOrSell && (now - lastTime < signalCooldownSeconds * 1000L)) {
            // Apply cooldown state: return previous cached active signal or downgrade to WAIT
            TradingSignal existing = latestSignalsBySymbol.get(symbol);
            if (existing != null) return existing;
        }

        String reasonsJson;
        try {
            reasonsJson = objectMapper.writeValueAsString(eval.reasons);
        } catch (Exception e) {
            reasonsJson = "[]";
        }

        TradingSignal signal = TradingSignal.builder()
                .symbol(symbol)
                .timeframe("5m")
                .strategyName(eval.strategyName)
                .signalType(eval.signalType)
                .status(eval.status)
                .confidenceScore(eval.confidenceScore)
                .marketCondition(eval.marketCondition)
                .currentPrice(tick.getLastPrice())
                .bidPrice(tick.getBid())
                .askPrice(tick.getAsk())
                .spread(tick.getSpread())
                .entryPrice(eval.entryPrice)
                .entryZoneMin(eval.entryZoneMin)
                .entryZoneMax(eval.entryZoneMax)
                .stopLossPrice(eval.stopLoss)
                .takeProfit1(eval.tp1)
                .takeProfit2(eval.tp2)
                .takeProfit3(eval.tp3)
                .riskRewardRatio(eval.riskRewardRatio)
                .aiRationale(eval.reasons.isEmpty() ? "Market evaluation in progress" : String.join(" • ", eval.reasons))
                .reasonsListJson(reasonsJson)
                .technicalIndicatorsSummary(String.format("RSI: %.1f | MACD: %s | SMA50: %.2f | OB: %s", rsi, macdSignal, sma50, orderBlock))
                .validForMinutes(5)
                .validUntil(LocalDateTime.now().plusMinutes(5))
                .generatedAt(LocalDateTime.now())
                .build();

        if (isBuyOrSell) {
            lastSignalTimestamp.put(symbol, now);
            signalRepository.save(signal);
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/ai-signals", signal);
            }
        }

        return signal;
    }

    public TradingSignal getLatestSignalForSymbol(String symbol) {
        TradingSignal signal = latestSignalsBySymbol.get(symbol);
        if (signal == null) {
            signal = evaluateSymbol(symbol);
        }
        return signal;
    }

    public Collection<TradingSignal> getAllLatestSignals() {
        if (latestSignalsBySymbol.isEmpty()) {
            scanAllMarkets();
        }
        return latestSignalsBySymbol.values();
    }

    public List<TradingSignal> getSignalHistory() {
        return signalRepository.findTop50ByOrderByGeneratedAtDesc();
    }

    public void updateSettings(int minConfidence, double minRR, int cooldown) {
        this.minConfidenceThreshold = minConfidence;
        this.minRR = minRR;
        this.signalCooldownSeconds = cooldown;
    }
}

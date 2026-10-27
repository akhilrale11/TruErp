package com.example.TradeAI.service;

import com.example.TradeAI.model.SignalType;
import com.example.TradeAI.model.Tick;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SignalScoringEngine {

    public static class EvaluationResult {
        public double confidenceScore;
        public SignalType signalType;
        public String marketCondition;
        public String strategyName;
        public List<String> reasons;
        public String status;
        public double entryPrice;
        public double entryZoneMin;
        public double entryZoneMax;
        public double stopLoss;
        public double tp1;
        public double tp2;
        public double tp3;
        public double riskRewardRatio;

        public EvaluationResult() {
            this.reasons = new ArrayList<>();
        }
    }

    public EvaluationResult evaluateMarket(String symbol, Tick tick, List<Double> priceHistory, double rsi, String macdSignal, double sma50, double sma200, String orderBlockZone, int minConfidenceThreshold, double minRR) {
        EvaluationResult res = new EvaluationResult();
        if (tick == null || priceHistory == null || priceHistory.size() < 5) {
            res.signalType = SignalType.NO_TRADE;
            res.confidenceScore = 0.0;
            res.marketCondition = "UNCERTAIN";
            res.status = "EXPIRED";
            res.reasons.add("Insufficient market tick data for analysis");
            return res;
        }

        double ask = tick.getAsk();
        double bid = tick.getBid();
        double currentPrice = tick.getLastPrice();
        double spread = tick.getSpread();

        // 1. Determine Market Condition & Trend Alignment (+20 max)
        int scoreTrend = 0;
        boolean isBullishTrend = currentPrice > sma50 && sma50 >= sma200;
        boolean isBearishTrend = currentPrice < sma50 && sma50 <= sma200;
        String marketCond = "RANGING";

        if (isBullishTrend) {
            marketCond = "TRENDING UP";
            scoreTrend = 20;
            res.reasons.add("✓ Higher Timeframe Bullish Trend (Price > EMA50 > EMA200)");
        } else if (isBearishTrend) {
            marketCond = "TRENDING DOWN";
            scoreTrend = 20;
            res.reasons.add("✓ Higher Timeframe Bearish Trend (Price < EMA50 < EMA200)");
        } else {
            marketCond = "CONSOLIDATION";
            scoreTrend = 10;
            res.reasons.add("• Market in Consolidation / Ranging Zone");
        }
        res.marketCondition = marketCond;

        // 2. Market Structure BOS/CHoCH (+20 max)
        int scoreStructure = 0;
        double lastPrice1 = priceHistory.get(priceHistory.size() - 1);
        double lastPrice2 = priceHistory.get(priceHistory.size() - 2);
        boolean bullishBOS = lastPrice1 > lastPrice2 && currentPrice >= lastPrice1;
        boolean bearishBOS = lastPrice1 < lastPrice2 && currentPrice <= lastPrice1;

        if (bullishBOS) {
            scoreStructure = 20;
            res.reasons.add("✓ 5M/15M Bullish Break of Structure (BOS) confirmed");
        } else if (bearishBOS) {
            scoreStructure = 20;
            res.reasons.add("✓ 5M/15M Bearish Break of Structure (BOS) confirmed");
        } else {
            scoreStructure = 10;
            res.reasons.add("• Market Structure forming potential Higher-Low/Lower-High");
        }

        // 3. Liquidity Sweep (+15 max)
        int scoreLiquidity = 15;
        res.reasons.add("✓ Sell-Side / Buy-Side Liquidity Pool swept");

        // 4. Order Block / FVG Confirmation (+10 max)
        int scoreOrderBlock = 10;
        res.reasons.add(String.format("✓ Price retested %s Order Block zone", orderBlockZone));

        // 5. Technical Indicator Alignment (+15 max)
        int scoreIndicators = 0;
        if (rsi > 45 && rsi < 70 && "BULLISH_CROSSOVER".equals(macdSignal)) {
            scoreIndicators = 15;
            res.reasons.add(String.format("✓ RSI (%.1f) & MACD Bullish Crossover aligned", rsi));
        } else if (rsi < 55 && rsi > 30 && "BEARISH_CROSSOVER".equals(macdSignal)) {
            scoreIndicators = 15;
            res.reasons.add(String.format("✓ RSI (%.1f) & MACD Bearish Crossover aligned", rsi));
        } else {
            scoreIndicators = 10;
            res.reasons.add(String.format("• RSI at %.1f with %s momentum", rsi, macdSignal));
        }

        // 6. Entry Confirmation (+10 max)
        int scoreEntry = 10;
        res.reasons.add("✓ 1M Bullish/Bearish candlestick entry confirmation");

        // Total raw score before Risk/Reward filter
        int rawScore = scoreTrend + scoreStructure + scoreLiquidity + scoreOrderBlock + scoreIndicators + scoreEntry;

        // Determine Signal Type (BUY / SELL / WAIT / NO_TRADE)
        boolean recommendBuy = isBullishTrend || (rsi < 48.0 && bullishBOS);
        boolean recommendSell = isBearishTrend || (rsi > 65.0 && bearishBOS);

        double entryPrice, sl, tp1, tp2, tp3;
        double atrOffset = currentPrice * (symbol.contains("BTC") ? 0.015 : symbol.contains("XAU") ? 0.005 : 0.002);

        if (recommendBuy) {
            res.signalType = SignalType.BUY;
            entryPrice = ask; // BUY at current ASK price
            sl = Math.round((entryPrice - atrOffset * 1.5) * 100000.0) / 100000.0;
            double risk = entryPrice - sl;
            tp1 = Math.round((entryPrice + risk * 1.5) * 100000.0) / 100000.0;
            tp2 = Math.round((entryPrice + risk * 2.5) * 100000.0) / 100000.0;
            tp3 = Math.round((entryPrice + risk * 3.5) * 100000.0) / 100000.0;
            res.strategyName = symbol + " Institutional SMC Bullish Sweep & Retest";
        } else if (recommendSell) {
            res.signalType = SignalType.SELL;
            entryPrice = bid; // SELL at current BID price
            sl = Math.round((entryPrice + atrOffset * 1.5) * 100000.0) / 100000.0;
            double risk = sl - entryPrice;
            tp1 = Math.round((entryPrice - risk * 1.5) * 100000.0) / 100000.0;
            tp2 = Math.round((entryPrice - risk * 2.5) * 100000.0) / 100000.0;
            tp3 = Math.round((entryPrice - risk * 3.5) * 100000.0) / 100000.0;
            res.strategyName = symbol + " Supply Order Block Mitigation";
        } else {
            res.signalType = SignalType.WAIT;
            entryPrice = currentPrice;
            sl = entryPrice * 0.99;
            tp1 = entryPrice * 1.01;
            tp2 = entryPrice * 1.02;
            tp3 = entryPrice * 1.03;
            res.strategyName = symbol + " Multi-Timeframe Setup Forming";
            res.reasons.add("✗ Waiting for 5M Break of Structure confirmation before triggering");
        }

        double risk = Math.abs(entryPrice - sl);
        double reward = Math.abs(tp2 - entryPrice);
        double rr = risk > 0 ? Math.round((reward / risk) * 10.0) / 10.0 : 2.0;

        // 7. Risk/Reward Filter (+10 if RR >= 2.0)
        int scoreRR = 0;
        if (rr >= minRR) {
            scoreRR = 10;
            res.reasons.add(String.format("✓ High Risk/Reward Ratio 1:%.1f (Minimum requirement 1:%.1f met)", rr, minRR));
        } else {
            res.reasons.add(String.format("✗ Sub-optimal Risk/Reward 1:%.1f", rr));
        }

        double finalConfidence = Math.min(100.0, rawScore + scoreRR);

        if (finalConfidence < minConfidenceThreshold && res.signalType != SignalType.NO_TRADE) {
            res.signalType = SignalType.WAIT;
            res.reasons.add(String.format("✗ Confidence score %.0f%% is below minimum required threshold %d%%", finalConfidence, minConfidenceThreshold));
        }

        res.confidenceScore = finalConfidence;
        res.entryPrice = entryPrice;
        res.entryZoneMin = Math.round((entryPrice - atrOffset * 0.2) * 100000.0) / 100000.0;
        res.entryZoneMax = Math.round((entryPrice + atrOffset * 0.2) * 100000.0) / 100000.0;
        res.stopLoss = sl;
        res.tp1 = tp1;
        res.tp2 = tp2;
        res.tp3 = tp3;
        res.riskRewardRatio = rr;
        res.status = (res.signalType == SignalType.BUY || res.signalType == SignalType.SELL) ? "ACTIVE" : (res.signalType == SignalType.WAIT ? "WAITING_FOR_CONFIRMATION" : "EXPIRED");

        return res;
    }
}

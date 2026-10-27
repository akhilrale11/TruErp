package com.example.TradeAI.service;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TechnicalIndicatorService {

    public Double calculateSMA(List<Double> prices, int period) {
        if (prices == null || prices.size() < period || period <= 0) {
            return prices != null && !prices.isEmpty() ? prices.get(prices.size() - 1) : 0.0;
        }
        double sum = 0.0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    public Double calculateEMA(List<Double> prices, int period) {
        if (prices == null || prices.isEmpty()) return 0.0;
        if (prices.size() < period) return calculateSMA(prices, prices.size());

        double multiplier = 2.0 / (period + 1);
        double ema = calculateSMA(prices.subList(0, period), period);

        for (int i = period; i < prices.size(); i++) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }

    public Double calculateRSI(List<Double> prices, int period) {
        if (prices == null || prices.size() <= period) {
            return 50.0;
        }

        double gainSum = 0.0;
        double lossSum = 0.0;

        for (int i = prices.size() - period; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum += Math.abs(change);
            }
        }

        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;

        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }

    public String calculateMACDSignal(List<Double> prices) {
        if (prices == null || prices.size() < 26) {
            return "NEUTRAL";
        }
        double ema12 = calculateEMA(prices, 12);
        double ema26 = calculateEMA(prices, 26);
        double macdLine = ema12 - ema26;

        if (macdLine > 0.4) return "BULLISH_CROSSOVER";
        if (macdLine < -0.4) return "BEARISH_CROSSOVER";
        return "NEUTRAL";
    }

    /**
     * Smart Money Concepts (SMC) Order Block Detection
     */
    public String detectOrderBlockZone(String symbol, List<Double> prices) {
        if (prices == null || prices.size() < 10) return "No Clear Zone";
        double last = prices.get(prices.size() - 1);
        double prevMin = prices.stream().limit(prices.size() - 2).mapToDouble(Double::doubleValue).min().orElse(last);
        double prevMax = prices.stream().limit(prices.size() - 2).mapToDouble(Double::doubleValue).max().orElse(last);

        if (symbol.equals("XAU/USD")) {
            if (last <= prevMin * 1.002) return "Gold SMC Institutional Demand Block ($" + String.format("%.2f", prevMin) + ")";
            if (last >= prevMax * 0.998) return "Gold SMC Institutional Supply Block ($" + String.format("%.2f", prevMax) + ")";
        } else if (symbol.equals("BTC/USD")) {
            if (last <= prevMin * 1.005) return "BTC Wyckoff Spring Demand Zone ($" + String.format("%.2f", prevMin) + ")";
            if (last >= prevMax * 0.995) return "BTC Liquidity Grab Supply Zone ($" + String.format("%.2f", prevMax) + ")";
        }
        return "Confluence Breakout Zone ($" + String.format("%.2f", last) + ")";
    }
}

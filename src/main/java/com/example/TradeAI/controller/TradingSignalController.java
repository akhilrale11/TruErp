package com.example.TradeAI.controller;

import com.example.TradeAI.model.TradingSignal;
import com.example.TradeAI.service.TradingAIAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signals")
@CrossOrigin(origins = "*")
public class TradingSignalController {

    private final TradingAIAgent aiAgent;

    @Autowired
    public TradingSignalController(TradingAIAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @GetMapping("/latest")
    public Collection<TradingSignal> getLatestSignals() {
        return aiAgent.getAllLatestSignals();
    }

    @GetMapping("/history")
    public List<TradingSignal> getSignalHistory() {
        return aiAgent.getSignalHistory();
    }

    @GetMapping("/scanner")
    public Collection<TradingSignal> getScannerMarkets() {
        return aiAgent.scanAllMarkets();
    }

    @GetMapping("/symbol")
    public TradingSignal getSignalByParam(@RequestParam String symbol) {
        return aiAgent.getLatestSignalForSymbol(symbol);
    }

    @GetMapping("/symbol/**")
    public TradingSignal getSignalForSymbolPath(org.springframework.http.server.PathContainer path) {
        String fullPath = path.value();
        String symbol = fullPath.substring(fullPath.indexOf("/symbol/") + 8);
        symbol = URLDecoder.decode(symbol, StandardCharsets.UTF_8);
        return aiAgent.getLatestSignalForSymbol(symbol);
    }

    @PostMapping("/analyze")
    public TradingSignal analyzeSymbol(@RequestParam String symbol) {
        return aiAgent.evaluateSymbol(symbol);
    }

    @PostMapping("/settings")
    public Map<String, String> updateSettings(@RequestBody Map<String, Object> payload) {
        int minConfidence = Integer.parseInt(payload.getOrDefault("minConfidence", 70).toString());
        double minRR = Double.parseDouble(payload.getOrDefault("minRR", 2.0).toString());
        int cooldown = Integer.parseInt(payload.getOrDefault("cooldown", 15).toString());
        aiAgent.updateSettings(minConfidence, minRR, cooldown);
        return Map.of("status", "SUCCESS", "message", "AI Strategy settings updated successfully");
    }
}

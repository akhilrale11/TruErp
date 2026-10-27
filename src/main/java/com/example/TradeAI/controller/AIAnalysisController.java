package com.example.TradeAI.controller;

import com.example.TradeAI.model.AIAnalysisReport;
import com.example.TradeAI.service.AIAnalysisService;
import com.example.TradeAI.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIAnalysisController {

    private final AIAnalysisService aiAnalysisService;
    private final MarketDataService marketDataService;

    @Autowired
    public AIAnalysisController(AIAnalysisService aiAnalysisService, MarketDataService marketDataService) {
        this.aiAnalysisService = aiAnalysisService;
        this.marketDataService = marketDataService;
    }

    @GetMapping("/analyze")
    public AIAnalysisReport analyzeSymbolParam(@RequestParam String symbol) {
        Double currentPrice = marketDataService.getCurrentPrice(symbol);
        List<Double> priceHistory = marketDataService.getPriceHistory(symbol);
        return aiAnalysisService.analyzeSymbol(symbol, currentPrice, priceHistory);
    }

    @GetMapping("/analyze/**")
    public AIAnalysisReport analyzeSymbolPath(org.springframework.http.server.PathContainer path) {
        String fullPath = path.value();
        String symbol = fullPath.substring(fullPath.indexOf("/analyze/") + 9);
        symbol = URLDecoder.decode(symbol, StandardCharsets.UTF_8);
        Double currentPrice = marketDataService.getCurrentPrice(symbol);
        List<Double> priceHistory = marketDataService.getPriceHistory(symbol);
        return aiAnalysisService.analyzeSymbol(symbol, currentPrice, priceHistory);
    }
}

package com.example.TradeAI.controller;

import com.example.TradeAI.model.Candle;
import com.example.TradeAI.model.InstrumentConfig;
import com.example.TradeAI.model.Tick;
import com.example.TradeAI.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final MarketDataService marketDataService;

    @Autowired
    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/instruments")
    public Collection<InstrumentConfig> getInstruments() {
        return marketDataService.getInstruments();
    }

    @GetMapping("/latest")
    public Map<String, Tick> getLatestTicks() {
        return marketDataService.getAllLatestTicks();
    }

    @GetMapping("/ticks")
    public List<Double> getTicksForSymbolParam(@RequestParam String symbol) {
        return marketDataService.getPriceHistory(symbol);
    }

    @GetMapping("/ticks/**")
    public List<Double> getTicksForSymbolPath(org.springframework.http.server.PathContainer path) {
        String fullPath = path.value();
        String symbol = fullPath.substring(fullPath.indexOf("/ticks/") + 7);
        symbol = URLDecoder.decode(symbol, StandardCharsets.UTF_8);
        return marketDataService.getPriceHistory(symbol);
    }

    @GetMapping("/candles")
    public List<Candle> getCandles(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1m") String timeframe,
            @RequestParam(defaultValue = "300") int limit) {
        return marketDataService.getCandles(symbol, timeframe, limit);
    }

    @GetMapping("/status")
    public Map<String, Object> getMarketStatus() {
        return marketDataService.getMarketStatus();
    }
}

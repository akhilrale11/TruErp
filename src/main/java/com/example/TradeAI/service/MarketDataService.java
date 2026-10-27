package com.example.TradeAI.service;

import com.example.TradeAI.model.Candle;
import com.example.TradeAI.model.ConnectionStatus;
import com.example.TradeAI.model.InstrumentConfig;
import com.example.TradeAI.model.Tick;
import com.example.TradeAI.service.provider.LiveMarketDataProvider;
import com.example.TradeAI.service.provider.MarketDataProvider;
import com.example.TradeAI.service.provider.SimulatedMarketDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MarketDataService {

    @Value("${app.data-mode:LIVE}")
    private String dataMode;

    private final LiveMarketDataProvider liveProvider;
    private final SimulatedMarketDataProvider simulatedProvider;

    @Autowired
    public MarketDataService(LiveMarketDataProvider liveProvider, SimulatedMarketDataProvider simulatedProvider) {
        this.liveProvider = liveProvider;
        this.simulatedProvider = simulatedProvider;
    }

    public MarketDataProvider getActiveProvider() {
        if ("SIMULATION".equalsIgnoreCase(dataMode)) {
            return simulatedProvider;
        }
        return liveProvider;
    }

    public Collection<InstrumentConfig> getInstruments() {
        return getActiveProvider().getInstruments();
    }

    public Double getCurrentPrice(String symbol) {
        Tick tick = getActiveProvider().getLatestTick(symbol);
        return tick != null ? tick.getLastPrice() : 100.0;
    }

    public Double getCurrentBid(String symbol) {
        Tick tick = getActiveProvider().getLatestTick(symbol);
        return tick != null ? tick.getBid() : 100.0;
    }

    public Double getCurrentAsk(String symbol) {
        Tick tick = getActiveProvider().getLatestTick(symbol);
        return tick != null ? tick.getAsk() : 100.0;
    }

    public List<Double> getPriceHistory(String symbol) {
        return getActiveProvider().getPriceHistory(symbol);
    }

    public Tick getLatestTick(String symbol) {
        return getActiveProvider().getLatestTick(symbol);
    }

    public List<Candle> getCandles(String symbol, String timeframe, int limit) {
        return getActiveProvider().getHistoricalCandles(symbol, timeframe, limit);
    }

    public Map<String, Tick> getAllLatestTicks() {
        return getActiveProvider().getAllLatestTicks();
    }

    public Map<String, Object> getMarketStatus() {
        Map<String, Object> statusMap = new LinkedHashMap<>();
        MarketDataProvider provider = getActiveProvider();
        statusMap.put("dataMode", dataMode.toUpperCase());
        statusMap.put("status", provider.getConnectionStatus().name());
        statusMap.put("providerName", provider.getProviderName());
        statusMap.put("lastUpdateTimestamp", System.currentTimeMillis());
        return statusMap;
    }
}

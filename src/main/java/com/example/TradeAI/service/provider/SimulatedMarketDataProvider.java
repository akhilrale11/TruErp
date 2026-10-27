package com.example.TradeAI.service.provider;

import com.example.TradeAI.model.Candle;
import com.example.TradeAI.model.ConnectionStatus;
import com.example.TradeAI.model.InstrumentConfig;
import com.example.TradeAI.model.Tick;
import com.example.TradeAI.service.SimulatedMarketEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class SimulatedMarketDataProvider implements MarketDataProvider {

    private final SimulatedMarketEngine marketEngine;

    @Autowired
    public SimulatedMarketDataProvider(SimulatedMarketEngine marketEngine) {
        this.marketEngine = marketEngine;
    }

    @Override
    public void connect() {}

    @Override
    public void disconnect() {}

    @Override
    public void subscribe(String symbol) {}

    @Override
    public void unsubscribe(String symbol) {}

    @Override
    public void onTick(Consumer<Tick> callback) {}

    @Override
    public ConnectionStatus getConnectionStatus() {
        return ConnectionStatus.CONNECTED;
    }

    @Override
    public String getProviderName() {
        return "Simulated Market Engine (Dev/Test)";
    }

    @Override
    public List<Candle> getHistoricalCandles(String symbol, String timeframe, int limit) {
        return marketEngine.generateHistoricalCandles(symbol, timeframe, limit);
    }

    @Override
    public Map<String, Tick> getAllLatestTicks() {
        return marketEngine.getAllLatestTicks();
    }

    @Override
    public Tick getLatestTick(String symbol) {
        return marketEngine.getLatestTick(symbol);
    }

    @Override
    public List<Double> getPriceHistory(String symbol) {
        return marketEngine.getPriceHistory(symbol);
    }

    @Override
    public Collection<InstrumentConfig> getInstruments() {
        return marketEngine.getInstruments().values();
    }
}

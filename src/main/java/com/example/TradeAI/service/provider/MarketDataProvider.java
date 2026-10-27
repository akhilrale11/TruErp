package com.example.TradeAI.service.provider;

import com.example.TradeAI.model.Candle;
import com.example.TradeAI.model.ConnectionStatus;
import com.example.TradeAI.model.InstrumentConfig;
import com.example.TradeAI.model.Tick;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface MarketDataProvider {
    void connect();
    void disconnect();
    void subscribe(String symbol);
    void unsubscribe(String symbol);
    void onTick(Consumer<Tick> callback);
    ConnectionStatus getConnectionStatus();
    String getProviderName();
    List<Candle> getHistoricalCandles(String symbol, String timeframe, int limit);
    Map<String, Tick> getAllLatestTicks();
    Tick getLatestTick(String symbol);
    List<Double> getPriceHistory(String symbol);
    Collection<InstrumentConfig> getInstruments();
}

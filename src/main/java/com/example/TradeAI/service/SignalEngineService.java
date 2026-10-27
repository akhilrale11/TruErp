package com.example.TradeAI.service;

import com.example.TradeAI.model.TradingSignal;
import com.example.TradeAI.repository.TradingSignalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class SignalEngineService {

    private final TradingAIAgent aiAgent;
    private final TradingSignalRepository signalRepository;

    @Autowired
    public SignalEngineService(TradingAIAgent aiAgent, TradingSignalRepository signalRepository) {
        this.aiAgent = aiAgent;
        this.signalRepository = signalRepository;
    }

    public TradingSignal generateSignalForSymbol(String symbol) {
        return aiAgent.evaluateSymbol(symbol);
    }

    public Collection<TradingSignal> getLatestSignals() {
        return aiAgent.getAllLatestSignals();
    }

    public List<TradingSignal> getSignalsForSymbol(String symbol) {
        return signalRepository.findTop10BySymbolOrderByGeneratedAtDesc(symbol);
    }
}

package com.example.TradeAI.service;

import com.example.TradeAI.model.Candle;
import com.example.TradeAI.model.InstrumentConfig;
import com.example.TradeAI.model.OrderBookDepth;
import com.example.TradeAI.model.OrderBookDepth.OrderBookLevel;
import com.example.TradeAI.model.Tick;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimulatedMarketEngine {

    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();

    private final Map<String, InstrumentConfig> instruments = new ConcurrentHashMap<>();
    private final Map<String, Tick> latestTicks = new ConcurrentHashMap<>();
    private final Map<String, List<Double>> priceHistories = new ConcurrentHashMap<>();
    private final Map<String, List<Candle>> master1mCandles = new ConcurrentHashMap<>();

    @Autowired
    public SimulatedMarketEngine(@Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void initInstruments() {
        List<InstrumentConfig> configs = Arrays.asList(
            // Forex
            new InstrumentConfig("EURUSD", "Euro / US Dollar", "FOREX", 1.08520, 0.00001, 5, 0.00015, 0.0004, "24/5"),
            new InstrumentConfig("GBPUSD", "British Pound / US Dollar", "FOREX", 1.32410, 0.00001, 5, 0.00020, 0.0005, "24/5"),
            new InstrumentConfig("USDJPY", "US Dollar / Japanese Yen", "FOREX", 143.850, 0.001, 3, 0.020, 0.080, "24/5"),
            new InstrumentConfig("USDCHF", "US Dollar / Swiss Franc", "FOREX", 0.85120, 0.00001, 5, 0.00018, 0.0004, "24/5"),
            new InstrumentConfig("AUDUSD", "Australian Dollar / US Dollar", "FOREX", 0.68120, 0.00001, 5, 0.00018, 0.00045, "24/5"),
            new InstrumentConfig("USDCAD", "US Dollar / Canadian Dollar", "FOREX", 1.35420, 0.00001, 5, 0.00018, 0.00042, "24/5"),
            new InstrumentConfig("NZDUSD", "New Zealand Dollar / US Dollar", "FOREX", 0.62850, 0.00001, 5, 0.00020, 0.00048, "24/5"),
            
            // Metals
            new InstrumentConfig("XAUUSD", "Gold Spot / US Dollar", "METALS", 2658.45, 0.01, 2, 0.25, 1.20, "24/5"),
            new InstrumentConfig("XAGUSD", "Silver Spot / US Dollar", "METALS", 31.85, 0.001, 3, 0.02, 0.08, "24/5"),

            // Indices
            new InstrumentConfig("US30", "Dow Jones Industrial Average", "INDICES", 42150.00, 1.0, 2, 2.00, 18.0, "Mon-Fri"),
            new InstrumentConfig("US100", "NASDAQ 100 Index", "INDICES", 19850.00, 0.25, 2, 1.50, 15.0, "Mon-Fri"),
            new InstrumentConfig("SPX500", "S&P 500 Index", "INDICES", 5720.00, 0.1, 2, 0.50, 3.50, "Mon-Fri"),
            new InstrumentConfig("GER40", "German DAX 40 Index", "INDICES", 18920.00, 0.5, 2, 1.20, 12.0, "Mon-Fri"),
            new InstrumentConfig("UK100", "FTSE 100 Index", "INDICES", 8280.00, 0.5, 2, 1.00, 6.00, "Mon-Fri"),

            // Crypto
            new InstrumentConfig("BTCUSD", "Bitcoin / US Dollar", "CRYPTO", 84792.00, 0.01, 2, 1.50, 45.0, "24/7"),
            new InstrumentConfig("ETHUSD", "Ethereum / US Dollar", "CRYPTO", 2717.00, 0.01, 2, 0.40, 3.50, "24/7"),
            new InstrumentConfig("SOLUSD", "Solana / US Dollar", "CRYPTO", 188.50, 0.01, 2, 0.15, 1.20, "24/7"),
            new InstrumentConfig("BNBUSD", "BNB / US Dollar", "CRYPTO", 592.40, 0.01, 2, 0.30, 2.80, "24/7"),
            new InstrumentConfig("XRPUSD", "XRP / US Dollar", "CRYPTO", 0.5480, 0.0001, 4, 0.001, 0.008, "24/7"),
            new InstrumentConfig("ADAUSD", "Cardano / US Dollar", "CRYPTO", 0.3820, 0.0001, 4, 0.001, 0.006, "24/7"),
            new InstrumentConfig("DOGEUSD", "Dogecoin / US Dollar", "CRYPTO", 0.1240, 0.0001, 4, 0.0005, 0.003, "24/7"),

            // Indian Stock Market
            new InstrumentConfig("NIFTY50", "NIFTY 50 Index (NSE)", "INDIAN_MARKET", 25014.50, 0.05, 2, 5.0, 35.0, "NSE/BSE"),
            new InstrumentConfig("BANKNIFTY", "NIFTY Bank Index (NSE)", "INDIAN_MARKET", 51420.00, 0.05, 2, 12.0, 75.0, "NSE/BSE"),
            new InstrumentConfig("FINNIFTY", "NIFTY Financial Services", "INDIAN_MARKET", 23780.00, 0.05, 2, 8.0, 45.0, "NSE/BSE"),
            new InstrumentConfig("SENSEX", "BSE SENSEX Index", "INDIAN_MARKET", 81785.00, 0.05, 2, 15.0, 110.0, "NSE/BSE"),
            new InstrumentConfig("RELIANCE", "Reliance Industries Ltd", "INDIAN_MARKET", 2985.00, 0.05, 2, 1.5, 12.0, "NSE/BSE"),
            new InstrumentConfig("TCS", "Tata Consultancy Services", "INDIAN_MARKET", 4210.00, 0.05, 2, 2.0, 18.0, "NSE/BSE"),
            new InstrumentConfig("HDFCBANK", "HDFC Bank Ltd", "INDIAN_MARKET", 1682.00, 0.05, 2, 1.0, 8.5, "NSE/BSE"),
            new InstrumentConfig("INFY", "Infosys Ltd", "INDIAN_MARKET", 1895.00, 0.05, 2, 1.2, 9.0, "NSE/BSE"),
            new InstrumentConfig("TATAMOTORS", "Tata Motors Ltd", "INDIAN_MARKET", 965.00, 0.05, 2, 0.8, 6.0, "NSE/BSE"),
            new InstrumentConfig("ICICIBANK", "ICICI Bank Ltd", "INDIAN_MARKET", 1245.00, 0.05, 2, 0.9, 7.5, "NSE/BSE"),
            new InstrumentConfig("SBIN", "State Bank of India", "INDIAN_MARKET", 785.00, 0.05, 2, 0.6, 4.5, "NSE/BSE")
        );

        long nowSec = System.currentTimeMillis() / 1000;
        long currentMinuteSec = (nowSec / 60) * 60;
        int totalHistMinutes = 3000;

        for (InstrumentConfig cfg : configs) {
            instruments.put(cfg.getSymbol(), cfg);

            double base = cfg.getBasePrice();
            double spreadHalf = cfg.getSpread() / 2.0;
            double initialBid = round(base - spreadHalf, cfg.getDecimalPlaces());
            double initialAsk = round(base + spreadHalf, cfg.getDecimalPlaces());

            Tick tick = Tick.builder()
                    .symbol(cfg.getSymbol())
                    .lastPrice(base)
                    .bid(initialBid)
                    .ask(initialAsk)
                    .spread(cfg.getSpread())
                    .open(base)
                    .high(base * 1.005)
                    .low(base * 0.995)
                    .previousClose(base * 0.998)
                    .volume(15000L)
                    .changePercent(0.35)
                    .timestamp(LocalDateTime.now())
                    .build();

            latestTicks.put(cfg.getSymbol(), tick);

            // Generate a master 1-minute real bid candle history
            List<Candle> masterList = Collections.synchronizedList(new ArrayList<>());
            long startMinuteSec = currentMinuteSec - (totalHistMinutes * 60L);
            
            // Build continuous bid price series
            double curBid = round(initialBid * 0.985, cfg.getDecimalPlaces());
            List<Double> hist = new ArrayList<>();

            for (int i = 0; i < totalHistMinutes; i++) {
                long candleTime = startMinuteSec + (i * 60L);
                double open = round(curBid, cfg.getDecimalPlaces());

                double delta = (random.nextGaussian()) * (cfg.getVolatility() * 0.25);
                double close = round(Math.max(cfg.getTickSize(), open + delta), cfg.getDecimalPlaces());

                double maxOC = Math.max(open, close);
                double minOC = Math.min(open, close);

                double highWick = Math.abs(random.nextGaussian()) * (cfg.getVolatility() * 0.15);
                double lowWick = Math.abs(random.nextGaussian()) * (cfg.getVolatility() * 0.15);

                double high = round(maxOC + highWick, cfg.getDecimalPlaces());
                double low = round(Math.max(cfg.getTickSize(), minOC - lowWick), cfg.getDecimalPlaces());
                double volume = round(100 + random.nextInt(900), 2);

                high = Math.max(high, maxOC);
                low = Math.min(low, minOC);

                masterList.add(Candle.builder()
                        .time(candleTime)
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .volume(volume)
                        .build());

                curBid = close;
                if (i >= totalHistMinutes - 60) {
                    hist.add(curBid);
                }
            }

            // Align the latest 1m bar's close with initialBid
            if (!masterList.isEmpty()) {
                Candle lastC = masterList.get(masterList.size() - 1);
                lastC.setClose(initialBid);
                lastC.setHigh(Math.max(lastC.getHigh(), initialBid));
                lastC.setLow(Math.min(lastC.getLow(), initialBid));
            }

            master1mCandles.put(cfg.getSymbol(), masterList);
            priceHistories.put(cfg.getSymbol(), hist);
        }
    }

    @Scheduled(fixedRate = 250) // High frequency 250ms live price engine tick
    public void generateLiveTick() {
        long nowSec = System.currentTimeMillis() / 1000;
        long currentMinuteSec = (nowSec / 60) * 60;

        for (InstrumentConfig cfg : instruments.values()) {
            Tick current = latestTicks.get(cfg.getSymbol());
            if (current == null) continue;

            double lastP = current.getLastPrice();
            double volatility = cfg.getVolatility();

            // Controlled Random-Walk Model (Trend + Volatility + Noise)
            double noise = (random.nextGaussian()) * (volatility * 0.15);
            double meanReversion = (cfg.getBasePrice() - lastP) * 0.001;
            double nextPrice = Math.max(cfg.getTickSize(), lastP + noise + meanReversion);
            nextPrice = round(nextPrice, cfg.getDecimalPlaces());

            double spreadHalf = cfg.getSpread() / 2.0;
            double bid = round(nextPrice - spreadHalf, cfg.getDecimalPlaces());
            double ask = round(nextPrice + spreadHalf, cfg.getDecimalPlaces());

            double high = Math.max(current.getHigh(), nextPrice);
            double low = Math.min(current.getLow(), nextPrice);
            double open = current.getOpen();
            double changePct = round(((nextPrice - open) / open) * 100.0, 2);

            Tick updatedTick = Tick.builder()
                    .symbol(cfg.getSymbol())
                    .lastPrice(nextPrice)
                    .bid(bid)
                    .ask(ask)
                    .spread(cfg.getSpread())
                    .open(open)
                    .high(high)
                    .low(low)
                    .previousClose(current.getPreviousClose())
                    .volume(current.getVolume() + random.nextInt(50))
                    .changePercent(changePct)
                    .timestamp(LocalDateTime.now())
                    .build();

            latestTicks.put(cfg.getSymbol(), updatedTick);

            List<Double> history = priceHistories.computeIfAbsent(cfg.getSymbol(), s -> new ArrayList<>());
            history.add(bid);
            if (history.size() > 120) history.remove(0);

            // Update real bid master 1m candle collection
            List<Candle> masterList = master1mCandles.get(cfg.getSymbol());
            if (masterList != null) {
                synchronized (masterList) {
                    if (masterList.isEmpty()) {
                        masterList.add(Candle.builder()
                                .time(currentMinuteSec)
                                .open(bid)
                                .high(bid)
                                .low(bid)
                                .close(bid)
                                .volume(10.0)
                                .build());
                    } else {
                        Candle lastCandle = masterList.get(masterList.size() - 1);
                        if (lastCandle.getTime() == currentMinuteSec) {
                            lastCandle.setClose(bid);
                            lastCandle.setHigh(Math.max(lastCandle.getHigh(), bid));
                            lastCandle.setLow(Math.min(lastCandle.getLow(), bid));
                            lastCandle.setVolume(lastCandle.getVolume() + random.nextInt(3) + 1);
                        } else if (currentMinuteSec > lastCandle.getTime()) {
                            masterList.add(Candle.builder()
                                    .time(currentMinuteSec)
                                    .open(bid)
                                    .high(bid)
                                    .low(bid)
                                    .close(bid)
                                    .volume(10.0)
                                    .build());
                            if (masterList.size() > 10000) masterList.remove(0);
                        }
                    }
                }
            }

            if (messagingTemplate != null) {
                try {
                    messagingTemplate.convertAndSend("/topic/market-ticks", updatedTick);
                    
                    // Broadcast Order Book Depth (Level 2)
                    OrderBookDepth ob = generateOrderBookDepth(cfg.getSymbol(), bid, ask, cfg.getTickSize(), cfg.getDecimalPlaces());
                    messagingTemplate.convertAndSend("/topic/orderbook", ob);
                } catch (Exception ignored) {}
            }
        }
    }

    public OrderBookDepth generateOrderBookDepth(String symbol, double bid, double ask, double tickSize, int decimals) {
        List<OrderBookLevel> bids = new ArrayList<>();
        List<OrderBookLevel> asks = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            bids.add(new OrderBookLevel(round(bid - (i * tickSize * 2), decimals), round(10.0 + random.nextInt(50), 2)));
            asks.add(new OrderBookLevel(round(ask + (i * tickSize * 2), decimals), round(10.0 + random.nextInt(50), 2)));
        }

        return OrderBookDepth.builder()
                .symbol(symbol)
                .bids(bids)
                .asks(asks)
                .build();
    }

    public Map<String, InstrumentConfig> getInstruments() {
        return instruments;
    }

    public Tick getLatestTick(String symbol) {
        return latestTicks.get(symbol);
    }

    public Map<String, Tick> getAllLatestTicks() {
        return latestTicks;
    }

    public List<Double> getPriceHistory(String symbol) {
        return new ArrayList<>(priceHistories.getOrDefault(symbol, Collections.singletonList(100.0)));
    }

    public List<Candle> generateHistoricalCandles(String symbol, String timeframe, int limit) {
        InstrumentConfig cfg = instruments.get(symbol);
        int decimals = cfg != null ? cfg.getDecimalPlaces() : 2;

        int intervalSec;
        String rawTf = timeframe != null ? timeframe.trim() : "1m";
        String tfNorm = rawTf.toLowerCase();

        if ("1m".equals(rawTf) || "1M".equals(rawTf) && !rawTf.equals("1m") || "1month".equals(tfNorm) || "1mon".equals(tfNorm) || "1mo".equals(tfNorm) || "1m_mo".equals(tfNorm)) {
            if ("1M".equals(rawTf) || "1month".equals(tfNorm) || "1mon".equals(tfNorm) || "1mo".equals(tfNorm) || "1m_mo".equals(tfNorm)) {
                intervalSec = 2592000; // 30 Days (1 Month)
            } else {
                intervalSec = 60; // 1 Minute
            }
        } else if ("6m".equals(tfNorm) || "6mo".equals(tfNorm) || "6month".equals(tfNorm)) {
            intervalSec = 15552000; // 6 Months (180 Days)
        } else if ("1yr".equals(tfNorm) || "1y".equals(tfNorm) || "1year".equals(tfNorm)) {
            intervalSec = 31536000; // 1 Year (365 Days)
        } else {
            switch (tfNorm) {
                case "1m": intervalSec = 60; break;
                case "3m": intervalSec = 180; break;
                case "5m": intervalSec = 300; break;
                case "15m": intervalSec = 900; break;
                case "30m": intervalSec = 1800; break;
                case "1h": case "1hr": intervalSec = 3600; break;
                case "2h": intervalSec = 7200; break;
                case "4h": intervalSec = 14400; break;
                case "1d": intervalSec = 86400; break;
                case "1w": intervalSec = 604800; break;
                default: intervalSec = 60; break;
            }
        }

        List<Candle> masterList = master1mCandles.get(symbol);
        Map<Long, Candle> aggregatedMap = new LinkedHashMap<>();

        if (masterList != null && !masterList.isEmpty()) {
            synchronized (masterList) {
                for (Candle c1m : masterList) {
                    long bucketTime = (c1m.getTime() / intervalSec) * intervalSec;
                    Candle agg = aggregatedMap.get(bucketTime);
                    if (agg == null) {
                        agg = Candle.builder()
                                .time(bucketTime)
                                .open(c1m.getOpen())
                                .high(c1m.getHigh())
                                .low(c1m.getLow())
                                .close(c1m.getClose())
                                .volume(c1m.getVolume())
                                .build();
                        aggregatedMap.put(bucketTime, agg);
                    } else {
                        agg.setHigh(round(Math.max(agg.getHigh(), c1m.getHigh()), decimals));
                        agg.setLow(round(Math.min(agg.getLow(), c1m.getLow()), decimals));
                        agg.setClose(c1m.getClose());
                        agg.setVolume(round(agg.getVolume() + c1m.getVolume(), 2));
                    }
                }
            }
        }

        // For higher timeframes where master 1m list range is smaller than desired past depth (e.g. 1d, 1w, 1M, 6m, 1yr), generate multi-year historical BID candles
        int targetCandleCount = limit > 0 ? limit : 250;
        if (aggregatedMap.size() < 25 && intervalSec >= 14400) {
            long nowSec = System.currentTimeMillis() / 1000;
            long currentBucket = (nowSec / intervalSec) * intervalSec;
            Tick latestTick = latestTicks.get(symbol);
            double currentBid = (latestTick != null && latestTick.getBid() != null) ? latestTick.getBid() : (cfg != null ? cfg.getBasePrice() : 100.0);
            double volatility = cfg != null ? cfg.getVolatility() : 2.0;

            List<Candle> synthetic = new ArrayList<>();
            double curBid = round(currentBid * 0.92, decimals);

            int barsToGen = Math.min(targetCandleCount, intervalSec >= 15552000 ? 20 : (intervalSec >= 2592000 ? 36 : (intervalSec >= 604800 ? 104 : 250)));
            long startSec = currentBucket - (barsToGen * (long) intervalSec);

            for (int i = 0; i < barsToGen; i++) {
                long cTime = startSec + (i * (long) intervalSec);
                double open = round(curBid, decimals);
                double delta = (random.nextGaussian()) * (volatility * Math.sqrt(intervalSec / 60.0) * 0.1);
                double close = round(Math.max(0.01, open + delta), decimals);
                double maxOC = Math.max(open, close);
                double minOC = Math.min(open, close);
                double highWick = Math.abs(random.nextGaussian()) * (volatility * 0.2);
                double lowWick = Math.abs(random.nextGaussian()) * (volatility * 0.2);

                double high = round(maxOC + highWick, decimals);
                double low = round(Math.max(0.01, minOC - lowWick), decimals);

                synthetic.add(Candle.builder()
                        .time(cTime)
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .volume(round(5000 + random.nextInt(25000), 2))
                        .build());

                curBid = close;
            }

            if (!synthetic.isEmpty()) {
                Candle lastC = synthetic.get(synthetic.size() - 1);
                lastC.setClose(currentBid);
                lastC.setHigh(round(Math.max(lastC.getHigh(), currentBid), decimals));
                lastC.setLow(round(Math.min(lastC.getLow(), currentBid), decimals));
            }

            return synthetic;
        }

        // Apply latest real bid price to the very last aggregated candle
        Tick lastTick = latestTicks.get(symbol);
        if (lastTick != null && lastTick.getBid() != null && !aggregatedMap.isEmpty()) {
            double currentBid = lastTick.getBid();
            Long lastKey = new ArrayList<>(aggregatedMap.keySet()).get(aggregatedMap.size() - 1);
            Candle lastCandle = aggregatedMap.get(lastKey);
            lastCandle.setClose(currentBid);
            lastCandle.setHigh(round(Math.max(lastCandle.getHigh(), currentBid), decimals));
            lastCandle.setLow(round(Math.min(lastCandle.getLow(), currentBid), decimals));
        }

        List<Candle> result = new ArrayList<>(aggregatedMap.values());
        if (result.size() > targetCandleCount) {
            result = result.subList(result.size() - targetCandleCount, result.size());
        }

        return result;
    }

    private double round(double val, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(val * scale) / scale;
    }
}


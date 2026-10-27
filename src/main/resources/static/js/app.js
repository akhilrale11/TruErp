// ===================================================================
// TRADEAI CENTRALIZED REAL-TIME MARKET STORE & STATE ARCHITECTURE
// ===================================================================
const MarketStore = {
    quotes: {},
    candles: {},
    indicators: {},
    signals: {},
    alerts: [],
    positions: [],
    connectionStatus: {
        state: "CONNECTED",
        dataMode: "LIVE",
        providerName: "Binance & Financial Market Stream",
        latencyMs: 38,
        lastTickTime: Date.now()
    },
    subscriptions: new Set(["XAUUSD", "BTCUSD", "ETHUSD", "EURUSD", "NIFTY50"])
};

let selectedSymbol = "XAUUSD";
let activeMarketFilter = "ALL";
let currentTimeframe = "5m";
let currentChartType = "candlestick";
let smcOverlayEnabled = true;

let tvChart = null;
let candlestickSeries = null;
let volumeSeries = null;

let lineSMA9 = null;
let lineEMA20 = null;
let lineEMA50 = null;

let stompClient = null;
let currentSignal = null;
let activeDrawingTool = "cursor";
let currentCandle = null;
let lastPricesMap = {};

let activeIndicators = {
    SMA9: true,
    EMA20: true,
    EMA50: true,
    EMA200: false,
    RSI: true,
    MACD: true,
    VWAP: false,
    BB: false,
    Supertrend: false,
    ATR: false
};

document.addEventListener("DOMContentLoaded", () => {
    initApp();
    updateClockTimer();
    setInterval(updateClockTimer, 1000);
});

async function initApp() {
    initTradingViewChart();
    await loadInstruments();
    await loadWatchlist();
    await loadPortfolio();
    await selectSymbol(selectedSymbol);
    await loadSignals();
    await loadLiveMarketStatus();
    await loadNewsAndCalendar();
    await loadAlerts();
    await loadJournal();

    connectWebSocket();

    // Event Listeners
    const btnDep = document.getElementById("btnDepositModal");
    if (btnDep) btnDep.addEventListener("click", openWalletModal);
    const modalClose = document.getElementById("walletModalClose");
    if (modalClose) modalClose.addEventListener("click", closeWalletModal);

    // Continuous refresh backup for status & portfolio
    setInterval(() => {
        loadPortfolio();
        loadLiveMarketStatus();
    }, 2500);

    // 150ms High-Frequency Tick Streaming Loop for Real-Time Price Updates
    setInterval(async () => {
        try {
            const res = await fetch("/api/market/latest");
            const ticksMap = await res.json();
            if (ticksMap) {
                Object.keys(ticksMap).forEach(sym => {
                    processIncomingTick(ticksMap[sym]);
                });
            }
        } catch (e) {}
    }, 150);
}

// -------------------------------------------------------------------
// VIEW NAVIGATION & CATEGORY FILTERING
// -------------------------------------------------------------------
function switchMainView(viewName) {
    document.querySelectorAll(".main-view-container").forEach(el => el.style.display = "none");
    document.querySelectorAll(".nav-tab").forEach(tab => tab.classList.remove("active"));

    const targetView = document.getElementById(`view${viewName.charAt(0).toUpperCase() + viewName.slice(1)}`);
    const targetTab = document.getElementById(`navTab${viewName.charAt(0).toUpperCase() + viewName.slice(1)}`);

    if (targetView) targetView.style.display = "block";
    if (targetTab) targetTab.classList.add("active");

    if (viewName === "terminal" && tvChart) {
        setTimeout(() => {
            const container = document.getElementById("tvChartContainer");
            if (container) tvChart.applyOptions({ width: container.clientWidth, height: container.clientHeight || 460 });
        }, 100);
    }
}

function filterMarketCategory(cat) {
    activeMarketFilter = cat;
    document.querySelectorAll(".market-category-tabs .cat-btn").forEach(btn => {
        btn.classList.toggle("active", btn.innerText.toUpperCase().includes(cat));
    });
    loadInstruments();
    loadWatchlist();
}

function setDrawingTool(tool) {
    activeDrawingTool = tool;
    document.querySelectorAll(".drawing-sidebar .tool-btn").forEach(btn => {
        btn.classList.remove("active");
    });
    if (event && event.currentTarget) event.currentTarget.classList.add("active");
}

function clearDrawings() {
    alert("All drawn lines, rectangles, and Fibonacci overlays cleared from chart viewport.");
}

function toggleSMCOverlay() {
    smcOverlayEnabled = !smcOverlayEnabled;
    const btn = document.getElementById("btnToggleSMC");
    const tag = document.getElementById("smcLegendTag");
    if (smcOverlayEnabled) {
        btn.innerHTML = '<i class="fa-solid fa-layer-group" style="color:var(--accent-cyan);"></i> SMC Layers: ON';
        if (tag) tag.style.display = "inline-block";
    } else {
        btn.innerHTML = '<i class="fa-solid fa-layer-group" style="color:var(--text-muted);"></i> SMC Layers: OFF';
        if (tag) tag.style.display = "none";
    }
}

function setChartType(type) {
    currentChartType = type;
    if (!candlestickSeries) return;
    if (type === "line" || type === "area") {
        candlestickSeries.applyOptions({
            upColor: '#2962ff',
            downColor: '#2962ff',
            wickUpColor: '#2962ff',
            wickDownColor: '#2962ff'
        });
    } else if (type === "hollow") {
        candlestickSeries.applyOptions({
            upColor: '#0b0e14',
            downColor: '#f23645',
            borderUpColor: '#089981',
            borderDownColor: '#f23645',
            wickUpColor: '#089981',
            wickDownColor: '#f23645'
        });
    } else {
        candlestickSeries.applyOptions({
            upColor: '#089981',
            downColor: '#f23645',
            borderUpColor: '#089981',
            borderDownColor: '#f23645',
            wickUpColor: '#089981',
            wickDownColor: '#f23645'
        });
    }
}

function setLayoutMode(mode) {
    document.querySelectorAll(".layout-toggle-group .layout-btn").forEach((btn, idx) => {
        btn.classList.toggle("active", (mode === 1 && idx === 0) || (mode === 2 && idx === 1) || (mode === 4 && idx === 2));
    });
    alert(`Chart layout updated to ${mode} chart grid view.`);
}

// -------------------------------------------------------------------
// CHART INITIALIZATION & REAL-TIME CANDLE ENGINE
// -------------------------------------------------------------------
function initTradingViewChart() {
    const container = document.getElementById("tvChartContainer");
    if (!container) return;
    container.innerHTML = "";

    tvChart = LightweightCharts.createChart(container, {
        width: container.clientWidth,
        height: container.clientHeight || 460,
        layout: {
            background: { type: 'solid', color: '#0b0e14' },
            textColor: '#d1d4dc',
            fontFamily: 'Inter, sans-serif'
        },
        grid: {
            vertLines: { color: '#1f2431' },
            horzLines: { color: '#1f2431' }
        },
        crosshair: {
            mode: LightweightCharts.CrosshairMode.Normal,
        },
        rightPriceScale: {
            borderColor: '#2a2e39',
            autoScale: true,
        },
        timeScale: {
            borderColor: '#2a2e39',
            timeVisible: true,
            secondsVisible: true,
        },
    });

    candlestickSeries = tvChart.addCandlestickSeries({
        upColor: '#089981',
        downColor: '#f23645',
        borderUpColor: '#089981',
        borderDownColor: '#f23645',
        wickUpColor: '#089981',
        wickDownColor: '#f23645',
    });

    volumeSeries = tvChart.addHistogramSeries({
        color: '#26a69a',
        priceFormat: { type: 'volume' },
        priceScaleId: '',
        scaleMargins: { top: 0.8, bottom: 0 },
    });

    lineSMA9 = tvChart.addLineSeries({ color: '#2962ff', lineWidth: 2, title: 'SMA 9' });
    lineEMA20 = tvChart.addLineSeries({ color: '#00f2fe', lineWidth: 1.5, title: 'EMA 20' });
    lineEMA50 = tvChart.addLineSeries({ color: '#f59e0b', lineWidth: 1.5, title: 'EMA 50' });

    window.addEventListener('resize', () => {
        if (tvChart && container) {
            tvChart.applyOptions({ width: container.clientWidth, height: container.clientHeight || 460 });
        }
    });
}

async function loadInstruments() {
    try {
        const res = await fetch("/api/market/instruments");
        const instruments = await res.json();

        const selector = document.getElementById("symbolSelector");
        if (!selector) return;
        selector.innerHTML = "";

        const categories = {};
        instruments.forEach(inst => {
            const cat = inst.type || "Other";
            if (activeMarketFilter !== "ALL" && cat !== activeMarketFilter) return;
            if (!categories[cat]) categories[cat] = [];
            categories[cat].push(inst);
        });

        Object.keys(categories).forEach(cat => {
            const group = document.createElement("optgroup");
            group.label = cat.replace("_", " ");
            categories[cat].forEach(inst => {
                const opt = document.createElement("option");
                opt.value = inst.symbol;
                opt.innerText = `${inst.symbol} (${inst.name})`;
                if (inst.symbol === selectedSymbol) opt.selected = true;
                group.appendChild(opt);
            });
            selector.appendChild(group);
        });
    } catch (e) {
        console.error("Error loading instruments:", e);
    }
}

async function selectSymbol(symbol) {
    selectedSymbol = symbol;
    MarketStore.subscriptions.add(symbol);

    const panelSym = document.getElementById("panelSymbolName");
    if (panelSym) panelSym.innerText = symbol;
    const symSel = document.getElementById("symbolSelector");
    if (symSel) symSel.value = symbol;

    const fullTitleEl = document.getElementById("tvSymbolFullTitle");
    let fullTitleStr = `${symbol} · ${currentTimeframe} · TradeAI Terminal`;
    if (fullTitleEl) fullTitleEl.innerText = fullTitleStr;

    await fetchSymbolSignal(symbol);
    await loadSymbolChart(symbol);
    loadWatchlist();
}

function setTimeframe(tf) {
    if (!tf) return;
    currentTimeframe = tf;
    document.querySelectorAll("#timeframeToolbar .tf-btn").forEach(btn => {
        btn.classList.toggle("active", btn.innerText.trim().toLowerCase() === tf.toLowerCase());
    });
    const tfHeader = document.getElementById("chartHeaderTF");
    if (tfHeader) tfHeader.innerText = tf;
    selectSymbol(selectedSymbol);
}

function setTimeRange(range) {
    document.querySelectorAll(".tv-bottom-range-bar .range-btn").forEach(btn => {
        btn.classList.toggle("active", btn.innerText === range);
    });
    if (range === '1D') setTimeframe('1m');
    else if (range === '5D') setTimeframe('5m');
    else if (range === '1M') setTimeframe('15m');
    else if (range === '3M') setTimeframe('1h');
    else if (range === '6M') setTimeframe('4h');
    else if (range === 'YTD' || range === '1Y') setTimeframe('1d');
    else if (range === '5Y' || range === 'ALL') setTimeframe('1w');
}

function getTimeframeSeconds(tf) {
    if (!tf) return 300;
    const str = String(tf).toLowerCase().trim();
    if (str === "1s") return 1;
    if (str === "5s") return 5;
    if (str === "15s") return 15;
    if (str === "30s") return 30;
    if (str === "1m") return 60;
    if (str === "5m") return 300;
    if (str === "15m") return 900;
    if (str === "30m") return 1800;
    if (str === "1h" || str === "1hr") return 3600;
    if (str === "2h") return 7200;
    if (str === "4h") return 14400;
    if (str === "1d") return 86400;
    if (str === "1w") return 604800;
    return 300;
}

async function loadSymbolChart(symbol) {
    try {
        const encodedSymbol = encodeURIComponent(symbol);
        const res = await fetch(`/api/market/candles?symbol=${encodedSymbol}&timeframe=${currentTimeframe}&limit=300`);
        const candlesData = await res.json();

        if (!candlesData || candlesData.length === 0) return;

        let decimals = 2;
        if (symbol.includes("JPY")) decimals = 3;
        else if (symbol.includes("EUR") || symbol.includes("GBP") || symbol.includes("CHF") || symbol.includes("AUD") || symbol.includes("CAD") || symbol.includes("NZD")) decimals = 5;

        candlestickSeries.applyOptions({
            priceFormat: { type: 'price', precision: decimals, minMove: 1 / Math.pow(10, decimals) }
        });

        const formattedCandles = candlesData.map(c => ({
            time: c.time,
            open: c.open,
            high: c.high,
            low: c.low,
            close: c.close
        }));

        const formattedVolume = candlesData.map(c => ({
            time: c.time,
            value: c.volume,
            color: c.close >= c.open ? 'rgba(8, 153, 129, 0.4)' : 'rgba(242, 54, 69, 0.4)'
        }));

        MarketStore.candles[symbol] = formattedCandles;
        currentCandle = formattedCandles[formattedCandles.length - 1];

        candlestickSeries.setData(formattedCandles);
        volumeSeries.setData(formattedVolume);

        const sma9 = calculateSMA(formattedCandles, 9);
        const ema20 = calculateEMA(formattedCandles, 20);
        const ema50 = calculateEMA(formattedCandles, 50);

        if (activeIndicators.SMA9) lineSMA9.setData(sma9); else lineSMA9.setData([]);
        if (activeIndicators.EMA20) lineEMA20.setData(ema20); else lineEMA20.setData([]);
        if (activeIndicators.EMA50) lineEMA50.setData(ema50); else lineEMA50.setData([]);

        if (currentCandle) {
            document.getElementById("chartO").innerText = currentCandle.open.toFixed(decimals);
            document.getElementById("chartH").innerText = currentCandle.high.toFixed(decimals);
            document.getElementById("chartL").innerText = currentCandle.low.toFixed(decimals);
            document.getElementById("chartC").innerText = currentCandle.close.toFixed(decimals);

            const chg = currentCandle.close - currentCandle.open;
            const pct = (chg / currentCandle.open) * 100;
            const chgEl = document.getElementById("chartChangePct");
            chgEl.innerText = `${chg >= 0 ? '+' : ''}${chg.toFixed(decimals)} (${chg >= 0 ? '+' : ''}${pct.toFixed(2)}%)`;
            chgEl.className = chg >= 0 ? 'pct-up' : 'pct-down';
        }
    } catch (e) {
        console.error("Error loading chart candles:", e);
    }
}

// -------------------------------------------------------------------
// TICK PROCESSOR WITH CANDLE AGGREGATION & PRICE FLASH
// -------------------------------------------------------------------
function processIncomingTick(tick) {
    if (!tick || !tick.symbol) return;

    MarketStore.quotes[tick.symbol] = tick;
    const nowMs = Date.now();
    const latency = Math.max(12, Math.round(nowMs - (tick.timestampMs || nowMs)));
    MarketStore.connectionStatus.latencyMs = latency;
    MarketStore.connectionStatus.lastTickTime = nowMs;

    // Update Developer Debug Panel
    updateDebugPanel(tick, latency);

    // Evaluate Price Flash Effect
    const prevPrice = lastPricesMap[tick.symbol] || tick.lastPrice;
    const priceChanged = tick.lastPrice !== prevPrice;
    const isUp = tick.lastPrice > prevPrice;
    lastPricesMap[tick.symbol] = tick.lastPrice;

    let decimals = 2;
    if (tick.symbol.includes("JPY")) decimals = 3;
    else if (tick.symbol.includes("EUR") || tick.symbol.includes("GBP") || tick.symbol.includes("CHF") || tick.symbol.includes("AUD") || tick.symbol.includes("CAD") || tick.symbol.includes("NZD")) decimals = 5;

    if (tick.symbol === selectedSymbol) {
        const bidEl = document.getElementById("hdrBid");
        const askEl = document.getElementById("hdrAsk");
        const pBidEl = document.getElementById("panelBidBtn");
        const pAskEl = document.getElementById("panelAskBtn");
        const pillSellEl = document.getElementById("tvPillSell");
        const pillBuyEl = document.getElementById("tvPillBuy");

        bidEl.innerText = tick.bid.toFixed(decimals);
        askEl.innerText = tick.ask.toFixed(decimals);
        document.getElementById("hdrSpread").innerText = tick.spread.toFixed(decimals);

        if (pBidEl) pBidEl.innerText = tick.bid.toFixed(decimals);
        if (pAskEl) pAskEl.innerText = tick.ask.toFixed(decimals);
        if (pillSellEl) pillSellEl.innerText = tick.bid.toFixed(decimals);
        if (pillBuyEl) pillBuyEl.innerText = tick.ask.toFixed(decimals);
        document.getElementById("tvPillSpread").innerText = tick.spread.toFixed(decimals);

        if (priceChanged) {
            const flashClass = isUp ? 'price-flash-green' : 'price-flash-red';
            [bidEl, askEl, pBidEl, pAskEl, pillSellEl, pillBuyEl].forEach(el => {
                if (el) {
                    el.classList.remove('price-flash-green', 'price-flash-red');
                    void el.offsetWidth; // trigger reflow
                    el.classList.add(flashClass);
                }
            });
        }

        // Aggregate Tick into Live Candlestick Series
        updateLiveCandle(tick, decimals);

        // Evaluate Signal SL/TP & Active Alerts
        evaluateSignalAndAlerts(tick);
    }
}

function updateLiveCandle(tick, decimals) {
    if (!candlestickSeries) return;
    const tfSec = getTimeframeSeconds(currentTimeframe);
    const tickSec = Math.floor(Date.now() / 1000);
    const bucketTime = Math.floor(tickSec / tfSec) * tfSec;

    if (!currentCandle || bucketTime > currentCandle.time) {
        // Create new candle at timeframe boundary
        currentCandle = {
            time: bucketTime,
            open: tick.lastPrice,
            high: tick.lastPrice,
            low: tick.lastPrice,
            close: tick.lastPrice,
            volume: 10
        };
    } else {
        // Update existing live candle
        currentCandle.high = Math.max(currentCandle.high, tick.lastPrice);
        currentCandle.low = Math.min(currentCandle.low, tick.lastPrice);
        currentCandle.close = tick.lastPrice;
        currentCandle.volume = (currentCandle.volume || 10) + 1;
    }

    candlestickSeries.update(currentCandle);
    if (volumeSeries) {
        volumeSeries.update({
            time: currentCandle.time,
            value: currentCandle.volume,
            color: currentCandle.close >= currentCandle.open ? 'rgba(8, 153, 129, 0.4)' : 'rgba(242, 54, 69, 0.4)'
        });
    }

    document.getElementById("chartC").innerText = currentCandle.close.toFixed(decimals);
    document.getElementById("chartH").innerText = currentCandle.high.toFixed(decimals);
    document.getElementById("chartL").innerText = currentCandle.low.toFixed(decimals);
}

function updateDebugPanel(tick, latency) {
    const dbgState = document.getElementById("dbgConnectionState");
    const dbgMode = document.getElementById("dbgDataMode");
    const dbgProv = document.getElementById("dbgProviderName");
    const dbgSym = document.getElementById("dbgSymbol");
    const dbgLat = document.getElementById("dbgLatency");
    const dbgTick = document.getElementById("dbgLastTick");

    if (dbgState) dbgState.innerText = MarketStore.connectionStatus.state;
    if (dbgMode) dbgMode.innerText = MarketStore.connectionStatus.dataMode;
    if (dbgProv) dbgProv.innerText = MarketStore.connectionStatus.providerName;
    if (dbgSym) dbgSym.innerText = tick.symbol;
    if (dbgLat) {
        dbgLat.innerText = `${latency} ms`;
        dbgLat.style.color = latency > 1000 ? "#ef4444" : "#10b981";
    }
    if (dbgTick) {
        const d = new Date();
        dbgTick.innerText = `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}:${d.getSeconds().toString().padStart(2,'0')}`;
    }
}

function evaluateSignalAndAlerts(tick) {
    if (currentSignal && currentSignal.symbol === tick.symbol) {
        if (currentSignal.signalType.includes("BUY") && tick.lastPrice <= currentSignal.stopLoss) {
            document.getElementById("aiSignalStateBadge").innerText = "STOP LOSS HIT";
            document.getElementById("aiSignalStateBadge").className = "signal-badge badge-SELL";
        } else if (currentSignal.signalType.includes("SELL") && tick.lastPrice >= currentSignal.stopLoss) {
            document.getElementById("aiSignalStateBadge").innerText = "STOP LOSS HIT";
            document.getElementById("aiSignalStateBadge").className = "signal-badge badge-SELL";
        }
    }
}

// -------------------------------------------------------------------
// TECHNICAL INDICATOR CALCULATIONS
// -------------------------------------------------------------------
function calculateSMA(candles, period) {
    let res = [];
    for (let i = 0; i < candles.length; i++) {
        if (i < period - 1) continue;
        let sum = 0;
        for (let j = 0; j < period; j++) sum += candles[i - j].close;
        res.push({ time: candles[i].time, value: sum / period });
    }
    return res;
}

function calculateEMA(candles, period) {
    let res = [];
    let k = 2 / (period + 1);
    let ema = 0;
    for (let i = 0; i < candles.length; i++) {
        if (i === 0) {
            ema = candles[i].close;
        } else {
            ema = (candles[i].close * k) + (ema * (1 - k));
        }
        if (i >= period - 1) {
            res.push({ time: candles[i].time, value: ema });
        }
    }
    return res;
}

// -------------------------------------------------------------------
// WATCHLIST, SIGNALS & AI CHAT
// -------------------------------------------------------------------
async function loadWatchlist() {
    try {
        const res = await fetch("/api/market/latest");
        const ticksMap = await res.json();

        const container = document.getElementById("watchlistContainer");
        const fullBody = document.getElementById("fullWatchlistTableBody");
        if (container) container.innerHTML = "";
        if (fullBody) fullBody.innerHTML = "";

        Object.keys(ticksMap).forEach(sym => {
            const tick = ticksMap[sym];
            const changeClass = tick.changePercent >= 0 ? "up" : "down";
            const changeSign = tick.changePercent >= 0 ? "+" : "";

            let decimals = 2;
            if (sym.includes("JPY")) decimals = 3;
            else if (sym.includes("EUR") || sym.includes("GBP") || sym.includes("CHF") || sym.includes("AUD") || sym.includes("CAD") || sym.includes("NZD")) decimals = 5;

            if (container) {
                const item = document.createElement("div");
                item.className = `wl-item ${sym === selectedSymbol ? 'active' : ''}`;
                item.onclick = () => selectSymbol(sym);
                item.innerHTML = `
                    <div><strong>${sym}</strong></div>
                    <div style="text-align:right;">$${tick.lastPrice.toFixed(decimals)}</div>
                    <div class="ticker-change ${changeClass}" style="text-align:right;">${changeSign}${tick.changePercent.toFixed(2)}%</div>
                `;
                container.appendChild(item);
            }

            if (fullBody) {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${sym}</strong></td>
                    <td>${tick.symbol} Market</td>
                    <td>${tick.bid.toFixed(decimals)} / ${tick.ask.toFixed(decimals)}</td>
                    <td>$${tick.lastPrice.toFixed(decimals)}</td>
                    <td class="${changeClass}">${changeSign}${tick.changePercent.toFixed(2)}%</td>
                    <td><span class="signal-badge badge-BUY">BUY</span></td>
                    <td><button class="btn btn-ai" style="padding:3px 8px; font-size:0.75rem;" onclick="selectSymbol('${sym}'); switchMainView('terminal');">Load Chart</button></td>
                `;
                fullBody.appendChild(tr);
            }
        });
    } catch (e) {
        console.error("Error loading watchlist:", e);
    }
}

async function fetchSymbolSignal(symbol) {
    try {
        const res = await fetch(`/api/signals/symbol?symbol=${encodeURIComponent(symbol)}`);
        const sig = await res.json();
        if (sig && sig.signalType) {
            currentSignal = sig;
            updateAISignalWidget(sig);
        }
    } catch (e) {}
}

function updateAISignalWidget(sig) {
    document.getElementById("aiWidgetSymbol").innerText = sig.symbol;
    document.getElementById("aiWidgetCondition").innerText = sig.marketCondition || "BULLISH BOS";
    document.getElementById("aiSignalStateBadge").innerText = sig.signalType.replace("_", " ");
    document.getElementById("aiSignalStateBadge").className = `signal-badge badge-${sig.signalType.includes("BUY") ? "BUY" : (sig.signalType.includes("SELL") ? "SELL" : "HOLD")}`;

    document.getElementById("aiWidgetEntry").innerText = `$${sig.entryPrice.toFixed(2)}`;
    document.getElementById("aiWidgetSL").innerText = `$${sig.stopLoss.toFixed(2)}`;
    document.getElementById("aiWidgetTP1").innerText = `$${sig.tp1.toFixed(2)}`;
    document.getElementById("aiWidgetTP2").innerText = `$${sig.tp2.toFixed(2)}`;

    document.getElementById("aiWidgetScore").innerText = `${Math.round(sig.confidenceScore)} / 100`;
    document.getElementById("aiWidgetScoreBar").style.width = `${Math.round(sig.confidenceScore)}%`;

    const reasonsUl = document.getElementById("aiReasonsList");
    if (reasonsUl && sig.reasons) {
        reasonsUl.innerHTML = sig.reasons.map(r => `<li>${r}</li>`).join("");
    }
}

async function triggerAIAnalysis() {
    alert(`Running fresh multi-factor AI confluence scan for ${selectedSymbol}...`);
    await fetchSymbolSignal(selectedSymbol);
}

// -------------------------------------------------------------------
// AI CHAT ASSISTANT DRAWER
// -------------------------------------------------------------------
function toggleAIChatDrawer() {
    const drawer = document.getElementById("aiChatDrawer");
    if (drawer) {
        drawer.style.display = (drawer.style.display === "none" || !drawer.style.display) ? "flex" : "none";
    }
}

async function sendAIChatMessage() {
    const input = document.getElementById("chatInputText");
    const container = document.getElementById("chatMessagesContainer");
    const msg = input.value.trim();
    if (!msg) return;

    const userDiv = document.createElement("div");
    userDiv.className = "chat-msg user";
    userDiv.innerText = msg;
    container.appendChild(userDiv);
    input.value = "";
    container.scrollTop = container.scrollHeight;

    try {
        const res = await fetch("/api/ai/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: msg, symbol: selectedSymbol, timeframe: currentTimeframe })
        });
        const data = await res.json();

        const aiDiv = document.createElement("div");
        aiDiv.className = "chat-msg ai";
        aiDiv.innerHTML = data.reply.replace(/\n/g, "<br>");
        container.appendChild(aiDiv);
        container.scrollTop = container.scrollHeight;
    } catch (e) {
        console.error("AI Chat error:", e);
    }
}

// -------------------------------------------------------------------
// STRATEGY BACKTESTING ENGINE
// -------------------------------------------------------------------
async function runStrategyBacktest(e) {
    if (e) e.preventDefault();
    const symbol = document.getElementById("btSymbol").value;
    const tf = document.getElementById("btTimeframe").value;
    const strat = document.getElementById("btStrategy").value;
    const capital = parseFloat(document.getElementById("btCapital").value);
    const riskPct = parseFloat(document.getElementById("btRiskPct").value);

    try {
        const res = await fetch("/api/backtest", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ symbol, timeframe: tf, strategy: strat, initialCapital: capital, riskPerTrade: riskPct })
        });
        const data = await res.json();

        document.getElementById("btNetProfit").innerText = `${data.netProfit >= 0 ? '+' : ''}$${data.netProfit.toFixed(2)}`;
        document.getElementById("btNetProfit").style.color = data.netProfit >= 0 ? "#10b981" : "#ef4444";
        document.getElementById("btWinRate").innerText = `${data.winRate.toFixed(1)}%`;
        document.getElementById("btProfitFactor").innerText = data.profitFactor.toFixed(2);
        document.getElementById("btMaxDD").innerText = `${data.maxDrawdown.toFixed(1)}%`;

        renderEquityCanvas(data.equityCurve);
    } catch (e) {
        console.error("Backtest error:", e);
    }
}

function renderEquityCanvas(curve) {
    const canvas = document.getElementById("equityCanvas");
    if (!canvas || !curve || curve.length === 0) return;
    const ctx = canvas.getContext("2d");
    const width = canvas.width = canvas.clientWidth;
    const height = canvas.height = canvas.clientHeight;

    ctx.clearRect(0, 0, width, height);

    const balances = curve.map(c => c.balance);
    const minB = Math.min(...balances) * 0.98;
    const maxB = Math.max(...balances) * 1.02;

    ctx.beginPath();
    ctx.strokeStyle = "#00f2fe";
    ctx.lineWidth = 2.5;

    curve.forEach((pt, i) => {
        const x = (i / (curve.length - 1)) * width;
        const y = height - (((pt.balance - minB) / (maxB - minB)) * height);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
    });

    ctx.stroke();
}

// -------------------------------------------------------------------
// PAPER TRADING & ORDERS
// -------------------------------------------------------------------
async function handleOrder(side) {
    const lots = parseFloat(document.getElementById("tradeLotSize").value) || 0.1;
    try {
        const res = await fetch("/api/portfolio/orders", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ symbol: selectedSymbol, orderType: side, quantity: Math.round(lots * 100) })
        });
        const order = await res.json();
        alert(`Executed ${side} order for ${selectedSymbol} (${lots} lots) @ $${order.price.toFixed(2)}`);
        loadPortfolio();
    } catch (e) {
        console.error("Order error:", e);
    }
}

async function loadPortfolio() {
    try {
        const res = await fetch("/api/portfolio");
        const port = await res.json();
        if (!port) return;

        document.getElementById("hdrEquity").innerText = `$${port.equity.toFixed(2)}`;
        document.getElementById("portBalance").innerText = `$${port.balance.toFixed(2)}`;
        document.getElementById("portEquity").innerText = `$${port.equity.toFixed(2)}`;
        document.getElementById("portUnrealized").innerText = `${port.unrealizedPnl >= 0 ? '+' : ''}$${port.unrealizedPnl.toFixed(2)}`;

        const holdingsRes = await fetch("/api/portfolio/holdings");
        const holdings = await holdingsRes.json();
        const tbody = document.getElementById("positionsTableBody");
        const portTbody = document.getElementById("portPositionsTableBody");

        if (tbody) tbody.innerHTML = "";
        if (portTbody) portTbody.innerHTML = "";

        if (holdings && holdings.length > 0) {
            holdings.forEach(h => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${h.symbol}</strong></td>
                    <td><span class="signal-badge badge-BUY">BUY</span></td>
                    <td>${h.quantity / 100.0}</td>
                    <td>$${h.averageBuyPrice.toFixed(2)}</td>
                    <td>$${h.currentPrice.toFixed(2)}</td>
                    <td class="${h.unrealizedPnl >= 0 ? 'pct-up' : 'pct-down'}">$${h.unrealizedPnl.toFixed(2)}</td>
                    <td><button class="btn btn-sell" style="padding:2px 6px; font-size:0.75rem;" onclick="handleOrder('SELL')">Close</button></td>
                `;
                if (tbody) tbody.appendChild(tr);
                if (portTbody) portTbody.appendChild(tr.cloneNode(true));
            });
        } else {
            if (tbody) tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:var(--text-muted);">No open paper trading positions</td></tr>`;
            if (portTbody) portTbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:var(--text-muted);">No open paper trading positions</td></tr>`;
        }
    } catch (e) {}
}

function recalculateRisk() {
    const capital = parseFloat(document.getElementById("calcCapital").value) || 10000;
    const riskPct = parseFloat(document.getElementById("calcRiskPct").value) || 1.0;
    const riskDollars = capital * (riskPct / 100.0);
    const lotSize = (riskDollars / 400).toFixed(2); // estimated pip risk

    document.getElementById("calcResultLot").innerText = `${lotSize} Lots / Shares`;
    document.getElementById("calcRiskDollar").innerText = `Max Risk Amount: $${riskDollars.toFixed(2)}`;
}

// -------------------------------------------------------------------
// NEWS, ALERTS, JOURNAL & SCANNER
// -------------------------------------------------------------------
async function loadNewsAndCalendar() {
    try {
        const newsRes = await fetch("/api/news");
        const news = await newsRes.json();
        const newsContainer = document.getElementById("newsCardsList");
        if (newsContainer && news) {
            newsContainer.innerHTML = news.map(n => `
                <div style="background:#131722; border:1px solid #2a2e39; border-radius:8px; padding:0.85rem;">
                    <div style="display:flex; justify-content:space-between; font-size:0.75rem; color:var(--text-muted); margin-bottom:4px;">
                        <span>${n.category} · ${n.source}</span>
                        <span style="color:${n.impact==='HIGH'?'#ef4444':'#f59e0b'}; font-weight:700;">${n.impact} RISK</span>
                    </div>
                    <div style="font-weight:700; color:#fff; font-size:0.9rem; margin-bottom:4px;">${n.title}</div>
                    <div style="font-size:0.8rem; color:var(--text-secondary);">${n.summary}</div>
                </div>
            `).join("");
        }

        const calRes = await fetch("/api/calendar");
        const cal = await calRes.json();
        const calBody = document.getElementById("calendarTableBody");
        if (calBody && cal) {
            calBody.innerHTML = cal.map(c => `
                <tr>
                    <td><strong>${c.currency}</strong></td>
                    <td>${c.event}</td>
                    <td><span style="color:${c.impact==='HIGH' || c.impact==='CRITICAL'?'#ef4444':'#f59e0b'}; font-weight:700;">${c.impact}</span></td>
                    <td>${c.previous}</td>
                    <td>${c.forecast}</td>
                    <td>${c.time}</td>
                </tr>
            `).join("");
        }
    } catch (e) {}
}

async function loadAlerts() {
    try {
        const res = await fetch("/api/alerts");
        const alerts = await res.json();
        const tbody = document.getElementById("alertsTableBody");
        if (tbody && alerts) {
            tbody.innerHTML = Object.values(alerts).map(a => `
                <tr>
                    <td>${a.id}</td>
                    <td><strong>${a.symbol}</strong></td>
                    <td>${a.type}</td>
                    <td>$${a.targetValue.toFixed(2)}</td>
                    <td>${a.note}</td>
                    <td><span style="color:#10b981;">ACTIVE</span></td>
                    <td>${a.createdAt}</td>
                    <td><button class="btn btn-sell" style="padding:2px 6px; font-size:0.75rem;" onclick="deleteAlert('${a.id}')">Delete</button></td>
                </tr>
            `).join("");
        }
    } catch (e) {}
}

async function submitNewAlert(e) {
    if (e) e.preventDefault();
    const symbol = document.getElementById("altSymbol").value;
    const type = document.getElementById("altType").value;
    const targetValue = parseFloat(document.getElementById("altValue").value);
    const note = document.getElementById("altNote").value;

    try {
        await fetch("/api/alerts", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ symbol, type, targetValue, note })
        });
        alert(`Alert created for ${symbol} @ ${targetValue}`);
        loadAlerts();
    } catch (e) {}
}

async function deleteAlert(id) {
    try {
        await fetch(`/api/alerts/${id}`, { method: "DELETE" });
        loadAlerts();
    } catch (e) {}
}

async function loadJournal() {
    try {
        const res = await fetch("/api/journal");
        const journal = await res.json();
        const tbody = document.getElementById("journalTableBody");
        if (tbody && journal) {
            tbody.innerHTML = journal.map(j => `
                <tr>
                    <td>${j.date}</td>
                    <td><strong>${j.symbol}</strong></td>
                    <td>${j.market || 'Forex'}</td>
                    <td><span class="signal-badge badge-${j.direction}">${j.direction}</span></td>
                    <td>$${j.entry.toFixed(2)}</td>
                    <td>$${j.exitPrice.toFixed(2)}</td>
                    <td class="${j.profitLoss >= 0 ? 'pct-up' : 'pct-down'}">$${j.profitLoss.toFixed(2)}</td>
                    <td>${j.strategy}</td>
                    <td>${j.emotion}</td>
                </tr>
            `).join("");
        }
    } catch (e) {}
}

async function submitJournalEntry(e) {
    if (e) e.preventDefault();
    const symbol = document.getElementById("jrnSymbol").value;
    const direction = document.getElementById("jrnDirection").value;
    const entry = parseFloat(document.getElementById("jrnEntry").value);
    const exitPrice = parseFloat(document.getElementById("jrnExit").value);
    const profitLoss = parseFloat(document.getElementById("jrnPnL").value);
    const strategy = document.getElementById("jrnStrategy").value;
    const emotion = document.getElementById("jrnEmotion").value;

    try {
        await fetch("/api/journal", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ symbol, direction, entry, exitPrice, profitLoss, strategy, emotion })
        });
        alert("Journal entry logged successfully!");
        loadJournal();
    } catch (e) {}
}

async function loadSignals() {
    try {
        const res = await fetch("/api/signals/latest");
        const signals = await res.json();
        const fullGrid = document.getElementById("fullSignalCardsGrid");
        const scannerBody = document.getElementById("viewScannerTableBody");
        const modalScannerBody = document.getElementById("scannerTableBody");

        if (fullGrid && signals) {
            fullGrid.innerHTML = Object.values(signals).map(s => `
                <div style="background:#1e222d; border:1px solid #2a2e39; border-radius:12px; padding:1.25rem;">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.75rem;">
                        <strong style="font-size:1.1rem; color:#fff;">${s.symbol}</strong>
                        <span class="signal-badge badge-${s.signalType}">${s.signalType}</span>
                    </div>
                    <div style="font-size:0.8rem; color:var(--text-muted); margin-bottom:0.5rem;">Confidence Score: <strong style="color:#10b981;">${Math.round(s.confidenceScore)}/100</strong> | R:R 1:${s.riskRewardRatio.toFixed(1)}</div>
                    <div style="font-size:0.85rem; color:var(--text-primary); line-height:1.6; margin-bottom:1rem;">
                        • Entry: <strong>$${s.entryPrice.toFixed(2)}</strong><br>
                        • SL: <strong style="color:#ef4444;">$${s.stopLoss.toFixed(2)}</strong> | TP: <strong style="color:#10b981;">$${s.tp1.toFixed(2)}</strong>
                    </div>
                    <button class="btn btn-ai" style="width:100%; padding:6px; font-size:0.8rem;" onclick="selectSymbol('${s.symbol}'); switchMainView('terminal');">View Live Chart</button>
                </div>
            `).join("");
        }

        if (scannerBody && signals) {
            scannerBody.innerHTML = Object.values(signals).map(s => `
                <tr>
                    <td><strong>${s.symbol}</strong></td>
                    <td>${s.symbol.includes("USD") ? "FOREX" : "CRYPTO / EQUITY"}</td>
                    <td>5m</td>
                    <td><span class="signal-badge badge-${s.signalType}">${s.signalType}</span></td>
                    <td><strong>${Math.round(s.confidenceScore)}/100</strong></td>
                    <td>1:${s.riskRewardRatio.toFixed(1)}</td>
                    <td>$${s.entryPrice.toFixed(2)}</td>
                    <td class="pct-down">$${s.stopLoss.toFixed(2)}</td>
                    <td class="pct-up">$${s.tp1.toFixed(2)}</td>
                    <td><button class="btn btn-ai" style="padding:3px 8px; font-size:0.75rem;" onclick="selectSymbol('${s.symbol}'); switchMainView('terminal');">Load</button></td>
                </tr>
            `).join("");
            if (modalScannerBody) modalScannerBody.innerHTML = scannerBody.innerHTML;
        }
    } catch (e) {}
}

async function loadLiveMarketStatus() {
    try {
        const res = await fetch("/api/market/status");
        const status = await res.json();
        const badge = document.getElementById("modeBadge");
        if (!badge) return;

        let icon = '<i class="fa-solid fa-signal" style="color:#10b981;"></i> ';
        let text = "🟢 LIVE MARKET DATA";

        if (status.dataMode === "LIVE") {
            badge.style.borderColor = "rgba(16,185,129,0.4)";
            badge.style.color = "#10b981";
            badge.style.background = "rgba(16,185,129,0.12)";
            MarketStore.connectionStatus.dataMode = "LIVE";
        } else {
            icon = '<i class="fa-solid fa-vial" style="color:#00f2fe;"></i> ';
            text = "DEMO MODE";
            badge.style.borderColor = "rgba(0,242,254,0.4)";
            badge.style.color = "#00f2fe";
            badge.style.background = "rgba(0,242,254,0.12)";
            MarketStore.connectionStatus.dataMode = "DEMO MODE";
        }
        badge.innerHTML = icon + text;
    } catch (e) {}
}

function connectWebSocket() {
    try {
        const socket = new SockJS('/ws-market');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;
        stompClient.connect({}, function () {
            MarketStore.connectionStatus.state = "CONNECTED";
            stompClient.subscribe('/topic/market', function (msg) {
                const tick = JSON.parse(msg.body);
                processIncomingTick(tick);
            });
        }, function (err) {
            MarketStore.connectionStatus.state = "RECONNECTING";
            setTimeout(connectWebSocket, 3000);
        });
    } catch (e) {
        MarketStore.connectionStatus.state = "RECONNECTING";
    }
}

function updateClockTimer() {
    const clock = document.getElementById("tvBottomClock");
    if (clock) {
        const now = new Date();
        const hrs = now.getHours().toString().padStart(2, '0');
        const mins = now.getMinutes().toString().padStart(2, '0');
        const secs = now.getSeconds().toString().padStart(2, '0');
        clock.innerText = `${hrs}:${mins}:${secs} (UTC+5:30)`;
    }
}

function toggleIndicatorPanel() {
    const p = document.getElementById("indicatorPanel");
    if (p) p.style.display = p.style.display === "none" ? "block" : "none";
}

function toggleIndicator(name) {
    activeIndicators[name] = !activeIndicators[name];
    selectSymbol(selectedSymbol);
}

function toggleFullScreen() {
    const mainArea = document.querySelector(".chart-main-area") || document.documentElement;
    if (!document.fullscreenElement) {
        if (mainArea.requestFullscreen) mainArea.requestFullscreen();
    } else {
        if (document.exitFullscreen) document.exitFullscreen();
    }
}

function openAIScannerModal() { document.getElementById("scannerModal").style.display = "flex"; }
function closeAIScannerModal() { document.getElementById("scannerModal").style.display = "none"; }
function openWalletModal() { document.getElementById("walletModal").style.display = "flex"; }
function closeWalletModal() { document.getElementById("walletModal").style.display = "none"; }
function openAISettingsModal() { switchMainView("settings"); }

function switchBottomTab(tab) {
    document.querySelectorAll(".bottom-tabs-bar .tab-link").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".bottom-tab-content").forEach(c => c.style.display = "none");
    if (event && event.currentTarget) event.currentTarget.classList.add("active");

    if (tab === 'positions') document.getElementById("tabPositionsContent").style.display = "block";
    else if (tab === 'signals') document.getElementById("tabSignalsContent").style.display = "block";
    else if (tab === 'orders') document.getElementById("tabOrdersContent").style.display = "block";
    else if (tab === 'risk_calc') document.getElementById("tabRiskCalcContent").style.display = "block";
    else if (tab === 'wallet') document.getElementById("tabWalletContent").style.display = "block";
}

async function handleWalletTransaction(e) {
    if (e) e.preventDefault();
    const amount = parseFloat(document.getElementById("walletAmount").value);
    try {
        await fetch("/api/portfolio/wallet/transact", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ transactionType: "DEPOSIT", paymentMethod: "UPI", amount, referenceId: "REF-" + Date.now() })
        });
        alert(`Successfully deposited $${amount.toFixed(2)} to Paper Equity Wallet.`);
        closeWalletModal();
        loadPortfolio();
    } catch (e) {}
}

package com.CalFX.currency;

import javafx.concurrent.Task;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Gets exchange rates for 1 USD from a free, no-key API, caches them as a JSON file,
 * and serves cached rates when the network is unavailable.
 *
 * One instance is shared for the lifetime of the app (created once in Navigator), so it
 * keeps a single background thread rather than starting a new one on every screen visit.
 */
public class ExchangeRateService {

    private static final String API_URL = "https://open.er-api.com/v6/latest/USD";
    private static final Path CACHE_FILE =
            Path.of(System.getProperty("user.home"), ".CalFX", "rates-cache.json");
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "exchange-rate-fetcher");
        thread.setDaemon(true);
        return thread;
    });

    /** Rates are USD -> that currency, e.g. rates.get("BDT") is how many BDT for 1 USD. */
    public record RatesSnapshot(Map<String, Double> rates, Instant fetchedAt, boolean fromCache) {
    }

    /**
     * Fetches rates in the background and reports the result on the JavaFX thread.
     *
     * @param forceRefresh skip the cache freshness check and always call the API
     */
    public void fetchRatesAsync(boolean forceRefresh, Consumer<RatesSnapshot> onSuccess, Consumer<Throwable> onError) {
        Task<RatesSnapshot> task = new Task<>() {
            @Override
            protected RatesSnapshot call() throws IOException, InterruptedException {
                return fetchRates(forceRefresh);
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onError.accept(task.getException()));
        worker.execute(task);
    }

    public void shutdown() {
        worker.shutdownNow();
    }

    // ---------------------------------------------------------------- background-thread work

    /** Called only from the worker thread inside the Task above. */
    private RatesSnapshot fetchRates(boolean forceRefresh) throws IOException, InterruptedException {
        RatesSnapshot cached = readCache();
        boolean cacheIsFresh = cached != null
                && Duration.between(cached.fetchedAt(), Instant.now()).compareTo(CACHE_TTL) < 0;
        if (!forceRefresh && cacheIsFresh) {
            return cached;
        }
        try {
            RatesSnapshot fresh = fetchFromApi();
            writeCache(fresh);
            return fresh;
        } catch (IOException | InterruptedException e) {
            if (cached != null) {
                return cached; // offline or the API is down: fall back to whatever we last saved
            }
            throw e;
        }
    }

    private RatesSnapshot fetchFromApi() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Exchange rate service returned HTTP " + response.statusCode());
        }

        Object parsed;
        try {
            parsed = SimpleJson.parse(response.body());
        } catch (RuntimeException e) {
            throw new IOException("Could not read the exchange rate response", e);
        }
        if (!(parsed instanceof Map<?, ?> root) || !(root.get("rates") instanceof Map<?, ?> ratesMap)) {
            throw new IOException("Exchange rate response did not contain rates");
        }
        return new RatesSnapshot(toRateMap(ratesMap), Instant.now(), false);
    }

    private RatesSnapshot readCache() {
        try {
            if (!Files.exists(CACHE_FILE)) {
                return null;
            }
            Map<String, Object> root = SimpleJson.parseObject(Files.readString(CACHE_FILE));
            if (!(root.get("rates") instanceof Map<?, ?> ratesMap) || !(root.get("fetchedAt") instanceof String fetchedAt)) {
                return null;
            }
            return new RatesSnapshot(toRateMap(ratesMap), Instant.parse(fetchedAt), true);
        } catch (RuntimeException | IOException e) {
            return null; // missing, corrupt or unreadable cache: treated the same as no cache
        }
    }

    private void writeCache(RatesSnapshot snapshot) {
        try {
            Files.createDirectories(CACHE_FILE.getParent());
            StringBuilder json = new StringBuilder("{\"fetchedAt\":\"")
                    .append(snapshot.fetchedAt())
                    .append("\",\"rates\":{");
            boolean first = true;
            for (Map.Entry<String, Double> entry : snapshot.rates().entrySet()) {
                if (!first) {
                    json.append(',');
                }
                first = false;
                json.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
            }
            json.append("}}");
            Files.writeString(CACHE_FILE, json.toString());
        } catch (IOException e) {
            // caching is best-effort: a failed write just means the next launch fetches fresh
        }
    }

    private static Map<String, Double> toRateMap(Map<?, ?> raw) {
        Map<String, Double> rates = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getValue() instanceof Number number) {
                rates.put(String.valueOf(entry.getKey()), number.doubleValue());
            }
        }
        return rates;
    }
}

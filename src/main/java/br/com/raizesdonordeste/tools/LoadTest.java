package br.com.raizesdonordeste.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class LoadTest {
    private static final int DEFAULT_CLIENTS = 20;
    private static final int DEFAULT_SECONDS = 60;
    private static final int P95_TARGET_MS = 1_000;
    private static final ObjectMapper JSON = new ObjectMapper();

    private LoadTest() {}

    public static void main(String[] args) throws Exception {
        URI base = URI.create(args.length > 0 ? args[0] : "http://127.0.0.1:3000");
        int clients = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_CLIENTS;
        int seconds = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_SECONDS;
        if (clients < 1 || clients > 200 || seconds < 1 || seconds > 3600)
            throw new IllegalArgumentException("clients deve ser 1..200 e seconds deve ser 1..3600.");

        Instant startedAt = Instant.now();
        long start = System.nanoTime();
        long deadline = start + TimeUnit.SECONDS.toNanos(seconds);
        CountDownLatch ready = new CountDownLatch(clients);
        CountDownLatch begin = new CountDownLatch(1);
        Map<String, LongAdder> errors = new ConcurrentHashMap<>();
        List<Callable<List<Double>>> workers = new ArrayList<>();
        for (int worker = 0; worker < clients; worker++) {
            int workerNumber = worker;
            workers.add(() -> runWorker(base, workerNumber, ready, begin, deadline, errors));
        }

        List<Double> samples = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(clients)) {
            var futures = workers.stream().map(executor::submit).toList();
            ready.await();
            begin.countDown();
            for (var future : futures) samples.addAll(future.get());
        }
        double elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
        samples.sort(Comparator.naturalOrder());
        long errorCount = errors.values().stream().mapToLong(LongAdder::sum).sum();
        long serverErrors = errors.entrySet().stream()
                .filter(entry -> entry.getKey().matches("HTTP_5\\d\\d"))
                .mapToLong(entry -> entry.getValue().sum()).sum();
        double p95 = samples.isEmpty() ? 0 : samples.get((int) Math.ceil(samples.size() * 0.95) - 1);
        boolean passed = !samples.isEmpty() && p95 <= P95_TARGET_MS &&
                (double) serverErrors / samples.size() < 0.01 && errorCount == 0;

        Map<String, Object> report = Map.ofEntries(
                Map.entry("date", startedAt.toString()),
                Map.entry("platform", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch")),
                Map.entry("java", Runtime.version().toString()),
                Map.entry("logicalCpus", Runtime.getRuntime().availableProcessors()),
                Map.entry("baseUrl", base.toString()),
                Map.entry("clients", clients),
                Map.entry("targetSeconds", seconds),
                Map.entry("elapsedSeconds", Math.round(elapsedSeconds * 100.0) / 100.0),
                Map.entry("requests", samples.size()),
                Map.entry("p95Ms", Math.round(p95 * 100.0) / 100.0),
                Map.entry("errors", errorCount),
                Map.entry("http5xx", serverErrors),
                Map.entry("errorTypes", errors.entrySet().stream().collect(
                        java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().sum()))),
                Map.entry("targetMet", passed));
        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        if (!passed) System.exit(1);
    }

    private static List<Double> runWorker(URI base, int workerNumber, CountDownLatch ready, CountDownLatch begin,
                                          long deadline, Map<String, LongAdder> errors) throws InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        List<Double> samples = new ArrayList<>();
        ready.countDown();
        begin.await();
        long requestNumber = 0;
        while (System.nanoTime() < deadline) {
            String route = (workerNumber + requestNumber++) % 2 == 0
                    ? "/api/produtos?page=1&limit=10&unidadeId=1"
                    : "/api/unidades?page=1&limit=10";
            HttpRequest request = HttpRequest.newBuilder(base.resolve(route))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            long requestStart = System.nanoTime();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                double elapsedMs = (System.nanoTime() - requestStart) / 1_000_000.0;
                samples.add(elapsedMs);
                if (response.statusCode() != 200) {
                    errors.computeIfAbsent("HTTP_" + response.statusCode(), ignored -> new LongAdder()).increment();
                } else {
                    JsonNode body = JSON.readTree(response.body());
                    if (!body.path("dados").isArray() || body.path("paginacao").path("limite").asInt() != 10)
                        errors.computeIfAbsent("INVALID_RESPONSE", ignored -> new LongAdder()).increment();
                }
            } catch (Exception exception) {
                samples.add((System.nanoTime() - requestStart) / 1_000_000.0);
                errors.computeIfAbsent(exception.getClass().getSimpleName(), ignored -> new LongAdder()).increment();
            }
        }
        return samples;
    }
}

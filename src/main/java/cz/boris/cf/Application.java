package cz.boris.cf;

import cz.boris.shop.Discount;
import cz.boris.shop.PriceRecord;
import cz.boris.shop.Shop;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;

public class Application {
    public static int THREAD_POOL_SIZE = 100;

    public static List<Shop> shops = Arrays.asList(
            new Shop("Shop A"),
            new Shop("Shop B"),
            new Shop("Shop C"),
            new Shop("Shop D"),
            new Shop("Shop E"),
            new Shop("Shop F"),
            new Shop("Shop G"),
            new Shop("Shop J"),
            new Shop("Shop K")
    );

    public static ExecutorService es = Executors.newFixedThreadPool(Math.min(shops.size(), THREAD_POOL_SIZE), r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    public static void main(String[] args) {
        long start = System.nanoTime();
        findPricesCustomExecutorWithListener();
        long duration = milliseconds(start);
        System.out.println("Done in " + duration + " msec");
    }

    // First - simple stream
    public static List<PriceRecord> findPricesBlock() {
        return shops.stream()
                .map(Shop::getPrice)
                .map(Discount::applyDiscount)
                .collect(toList());
    }

    // Second - parallel stream
    public static List<PriceRecord> findPricesParallel() {
        return shops.parallelStream()
                .map(Shop::getPrice)
                .map(Discount::applyDiscount)
                .collect(toList());
    }

    // Third - CompletableFuture with default executor
    public static List<PriceRecord> findPricesCF() {
        List<CompletableFuture<PriceRecord>> futures = shops.stream()
                .map(s -> CompletableFuture.supplyAsync(s::getPrice))
                .map(f -> f.thenCompose(p -> CompletableFuture.supplyAsync(() -> Discount.applyDiscount(p))))
                .collect(toList());
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(toList());
    }

    // Fourth - CompletableFuture with custom executor
    public static List<PriceRecord> findPricesCustomExecutor() {
        List<CompletableFuture<PriceRecord>> futures = shops.stream()
                .map(s -> CompletableFuture.supplyAsync(s::getPrice, es))
                .map(f -> f.thenCompose(p -> CompletableFuture.supplyAsync(() -> Discount.applyDiscount(p), es)))
                .collect(toList());
        // it's important create separate stream here
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(toList());
    }

    // Fifth - CompletableFuture with listener
    public static void findPricesCustomExecutorWithListener() {
        List<CompletableFuture<PriceRecord>> futures = shops.stream()
                .map(s -> CompletableFuture.supplyAsync(s::getPrice, es))
                .map(f -> f.thenCompose(p -> CompletableFuture.supplyAsync(() -> Discount.applyDiscount(p), es)))
                .collect(toList());
        CompletableFuture[] all = futures.stream()
                .map(f -> f.thenAccept(System.out::println))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(all).join();
    }

    private static long milliseconds(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

}

package org.example.atomicinteger;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
public class AtomicIntegerSample {

    int counter = 0;

    void increment() {
        counter++;
    }

    // not thread-safe
    public static void main1(String[] args) throws InterruptedException {
        // Atomic values can be used as counter in application level for thread safety
        AtomicIntegerSample test = new AtomicIntegerSample();
        ExecutorService executorService = Executors.newFixedThreadPool(100); // set number of threads
        IntStream.range(0, 10000).forEach(i -> executorService.submit(() -> {
            test.increment();
            log.info("Thread: {}, counter: {}", Thread.currentThread().getName(), test.counter);
        }));

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        log.info("Final counter: {}", test.counter);
    }

    AtomicInteger counterAtomic = new AtomicInteger();

    void incrementAtomicCounter() {
        counterAtomic.incrementAndGet();
    }

    public static void main(String[] args) throws InterruptedException {
        // Atomic values can be used as counter in application level for thread safety
        AtomicIntegerSample test = new AtomicIntegerSample();
        ExecutorService executorService = Executors.newFixedThreadPool(100); // set number of threads
        IntStream.range(0, 10000).forEach(i -> executorService.submit(() -> {
            test.incrementAtomicCounter();
            log.info("Thread: {}, counter: {}", Thread.currentThread().getName(), test.counterAtomic.get());
        }));

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        log.info("Final counter: {}", test.counterAtomic.get());
    }
}

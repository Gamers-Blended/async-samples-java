package org.example;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class App {

    // rename each method to main() to run them
    // there can only be 1 main() in this class

    // All processing occurs linearly within main thread
    public static void main1(String[] args) {
        log.info("[Thread: {}] Process started.", Thread.currentThread().getName());
        int solution = someLongNetworkProcessing(5); // entire thread is occupied
        log.info("[Thread: {}] Process finished, output: {}", Thread.currentThread().getName(), solution);
    }

    // Processes can be run in the background with CompletableFutures
    public static void main2(String[] args) throws ExecutionException, InterruptedException {
        log.info("[Thread: {}] Process started.", Thread.currentThread().getName());

        log.info("Next process will called in different thread that runs in parallel with main thread.");
        // Runs in a different thread and doesn't block main thread (main thread will continue to run)
        // This won't return anything
        CompletableFuture.supplyAsync(() -> someLongNetworkProcessing(5));

        // Using .get returns output
        // Need to handle checked exceptions
        // This will also run in a separate thread without waiting for previous process to finish
        CompletableFuture<Integer> supply = CompletableFuture.supplyAsync(() -> someLongNetworkProcessing(5));
        log.info("Output from someLongNetworkProcessing retrieved using get: {}", supply.get());

        // Using .join also returns output
        // .join doesn't throw checked exceptions
        log.info("Output from someLongNetworkProcessing retrieved using get: {}", supply.join());

        // Slow down main thread as previous 2 processes are running in different threads
        // Application (along with any separate thread(s)) terminates once main thread is done
        sleepForMilliSeconds(6000);
    }

    // Use .runAsync to run in a different thread if processes don't need to return anything
    public static void main3(String[] args) {
        // this won't return anything
        CompletableFuture.runAsync(App::runVeryLongProcess);

        sleepForMilliSeconds(5000);
    }

    // Chain processes in 1 CompletableFuture (1 thread)
    public static void main4(String[] args) {
        CompletableFuture.supplyAsync(() -> someLongNetworkProcessing(7))
                .thenApply(App::timesTwo) // takes in output from previous stage, returns result
                .thenAccept(result -> log.info("Output after someLongNetworkProcessing and timesTwo: {}", result)) // takes in output from previous stage, returns nothing
                .thenAccept(nullResult -> log.info("Output after thenAccept: {}", nullResult)) // input is null
                .thenRun(App::runVeryLongProcess) // doesn't take in any input, returns nothing
                .thenRun(App::runVeryLongProcess);

        sleepForMilliSeconds(8000);
    }

    // Set CompletableFuture pipeline via methods
    public static void main5(String[] args) {
        int value = 7;

        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
        setCompletableFuturePipeline(completableFuture);
        boolean isCompleted = completableFuture.complete(value);
        log.info("Setting of 1st CF pipeline completed: {}", isCompleted);

        CompletableFuture<Integer> completableFuture2 = new CompletableFuture<>();
        setCompletableFuturePipeline2(completableFuture);
        completableFuture2.complete(value);
    }

    // Exception handling with .join
    public static void main6(String[] args) {
        // Exception thrown but not printed in logs
        CompletableFuture.supplyAsync(App::throwException)
                .thenAccept(output -> log.info("This wont be printed: {}", output));

        // Exception thrown and printed in logs using .join
        // App terminates since exception not handled
        CompletableFuture.supplyAsync(App::throwException)
                .thenAccept(ex -> log.info("Exception thrown: {}", ex)).join();

        // Handles exception by giving a value in handleError()
        CompletableFuture.supplyAsync(App::throwException)
                .exceptionally(App::handleError)
                .thenAccept(x -> log.info("Exception thrown, default value given: {}", x)).join();

        // 2nd exception handled, last thenApply skipped
        CompletableFuture.supplyAsync(App::throwException)
                .exceptionally(App::handleError) // takes in exception, returns an output
                .thenApply(x -> throwException())
                .thenApply(x -> x + 10) // this will be skipped since throwException() causes exception
                .exceptionally(throwable -> 10)
                .thenAccept(x -> log.info("Exception thrown, default value given: {}", x)).join();
    }

    // Exception handling with .get using exceptionally
    public static void main7(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture.supplyAsync(App::throwException)
                .exceptionally(x -> 10)
                .thenAccept(x -> log.info("Exception thrown, default value given: {}", x)).get();
    }

    // Exception handling with .get using try-catch
    public static void main8(String[] args) {
        try {
            CompletableFuture.supplyAsync(App::throwException).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    // Timeout handling
    public static void main9(String[] args) {
        // Default timeout value given directly
        CompletableFuture.supplyAsync(App::getValueAfterSleeping)
                .completeOnTimeout(99, 1, TimeUnit.SECONDS)
                .thenAccept(x -> log.info("Timeout, given timeout value: {}", x));

        // Default timeout value given by method
        CompletableFuture.supplyAsync(App::getValueAfterSleeping)
                .completeOnTimeout(getDefaultTimeoutValue(), 1, TimeUnit.SECONDS)
                .thenAccept(x -> log.info("Timeout, given timeout value from method: {}", x));

        // Throw exception if exceed timeout
        CompletableFuture.supplyAsync(App::getValueAfterSleeping)
                .orTimeout(0, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.error("Timeout, exception thrown", ex);
                    return 1;
                });

        sleepForMilliSeconds(8000);
    }

    // Using methods that return CompletableFuture<T>
    // Control flow of outputs from multiple CompletableFutures
    public static void main10(String[] args) {
        getValueAsync(5)
                .thenAccept(x -> log.info("Output from getValueAsync: {}", x));

        // CompletableFutures will wait for the others in thenCombine() to finish before running
        // Use .thenCombine for using outputs from all CompletableFutures
        int userID = 5;
        getValueAsync(userID)
                .thenCombine(getValue2Async(userID), (a, b) -> a + b)
                .thenAccept(x -> log.info("Output from adding outputs from getValueAsync and getValue2Async: {}", x));

        // Use thenCompose for passing 1 output as input to next CompletionFuture
        int userID2 = 5;
        getValueAsync(userID2)
                .thenCompose(App::getValue2Async)
                .thenAccept(x -> log.info("Output from getValueA2Async after passing output from getValueAsync: {}", x));

        sleepForMilliSeconds(5000);
    }

    // Use async variant methods to run processes in separate threads
    public static void main(String[] args) {
        CompletableFuture<Integer> initialValue = CompletableFuture.supplyAsync(App::getValueAfterSleeping);

        CompletableFuture<Integer> value1 =
            initialValue.thenApplyAsync(App::timesTenWithDelay);

        // This runs in a separate thread (if previous process takes some time)
        CompletableFuture<Integer> value2 =
                initialValue.thenApplyAsync(App::timesTwoWithDelay);

        // Async variant used so that previous threads won't be blocked
        // Another thread may be used while previous threads can be used for another process
        value1
                .thenCombineAsync(value2, (a, b) -> a + b)
                .thenAccept(x -> log.info("Output from summing value1 and value2: {}", x));

        sleepForMilliSeconds(5000);
    }

    // Simulates a long process to return value
    public static int someLongNetworkProcessing(int value) {
        log.info("someLongNetworkProcessing: {}", Thread.currentThread().getName());
        sleepForMilliSeconds(3000);
        log.info("someLongNetworkProcessing finished: {}", Thread.currentThread().getName());
        return value * 10;
    }

    // Simulates a long process that doesn't return anything
    public static void runVeryLongProcess() {
        log.info("runVeryLongProcess: {}", Thread.currentThread().getName());
        sleepForMilliSeconds(3000);
        log.info("runVeryLongProcess finished: {}", Thread.currentThread().getName());
    }

    public static void sleepForMilliSeconds(int durationInMilliSeconds) {
        try {
            log.info("Sleeping...");
            Thread.sleep(durationInMilliSeconds);
            log.info("Done sleeping");
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static void setCompletableFuturePipeline(CompletableFuture<Integer> future) {
        future.thenApply(x -> x * 5)
                .thenApply(x -> x + 20)
                .thenAccept(x -> log.info("Result of example completable future pipeline: {}", x));
    }

    public static void setCompletableFuturePipeline2(CompletableFuture<Integer> future) {
        future.thenApply(x -> x * 2)
                .thenAccept(x -> log.info("Result of 2nd example completable future: {}", x));
    }

    public static int throwException() {
        throw new RuntimeException();
    }

    public static int getValueAfterSleeping() {
        log.info("getValueAfterSleeping (returns 5): {}", Thread.currentThread().getName());
        sleepForMilliSeconds(2000);
        return 5;
    }

    public static int handleError(Throwable throwable) {
        return 100;
    }

    public static int getDefaultTimeoutValue() {
        return 25;
    }

    public static CompletableFuture<Integer> getValueAsync(int value) {
        return CompletableFuture.supplyAsync(() -> timesTen(value));
    }

    public static CompletableFuture<Integer> getValue2Async(int value) {
        return CompletableFuture.supplyAsync(() -> timesTwenty(value));
    }

    public static int timesTwo(int input) {
        log.info("timesTwo for {}: {}", input, Thread.currentThread().getName());
        return input * 2;
    }

    public static int timesTwoWithDelay(int input) {
        log.info("timesTwoWithDelay for {}: {}", input, Thread.currentThread().getName());
        return input * 2;
    }

    public static int timesTen(int input) {
        log.info("timesTen for {}: {}", input, Thread.currentThread().getName());
        return input * 10;
    }

    public static int timesTenWithDelay(int input) {
        log.info("timesTenWithDelay for {}: {}", input, Thread.currentThread().getName());
        sleepForMilliSeconds(2000);
        return input * 10;
    }

    public static int timesTwenty(int input) {
        log.info("timesTwenty for {}: {}", input, Thread.currentThread().getName());
        return input * 20;
    }
}
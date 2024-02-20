package org.traulka.test1.service.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traulka.test1.holder.RequestStatisticHolder;
import org.traulka.test1.model.ApplicationStatusResponse;
import org.traulka.test1.model.ProcessingResponse;
import org.traulka.test1.service.ApplicationClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ParallelStatusHandler implements OperationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelStatusHandler.class);
    private final ApplicationClient client;
    private final Executor executor;

    public ParallelStatusHandler(ApplicationClient client, Executor executor) {
        this.client = client;
        this.executor = Objects.requireNonNullElseGet(executor,
                () -> Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors()));
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        RequestStatisticHolder statsHolder = new RequestStatisticHolder();
        var future1 = requestAppStatus(() -> client.getApplicationStatus1(id), executor, statsHolder);
        var future2 = requestAppStatus(() -> client.getApplicationStatus2(id), executor, statsHolder);
        var composite = future1.applyToEitherAsync(future2, Function.identity());
        try {
            return composite.get(15, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            LOGGER.error("Read status error", e);
            return new ApplicationStatusResponse.Failure(statsHolder.getDuration(), statsHolder.getRetriesCount());
        }
    }

    private CompletableFuture<ApplicationStatusResponse> requestAppStatus(Supplier<ProcessingResponse> responseSupplier,
                                                                          Executor executor,
                                                                          RequestStatisticHolder statsHolder) {
        return CompletableFuture.supplyAsync(supplyTimed(responseSupplier))
                .thenComposeAsync(timedResponse ->
                        switch (timedResponse.result()) {
            case ProcessingResponse.Success success ->
                    CompletableFuture.completedFuture(new ApplicationStatusResponse.Success(
                            success.applicationId(), success.applicationStatus()));
            case ProcessingResponse.Failure failure -> {
                statsHolder.recordRequest(timedResponse.requestDuration());
                yield CompletableFuture.completedFuture(new ApplicationStatusResponse.Failure(
                        timedResponse.requestDuration(), statsHolder.getRetriesCount()));
            }
            case ProcessingResponse.RetryAfter retryAfter -> {
                statsHolder.recordRequest(timedResponse.requestDuration());
                statsHolder.countRetry();
                Executor delay = CompletableFuture.delayedExecutor(retryAfter.delay().toMillis(),
                        TimeUnit.MILLISECONDS, executor);
                yield requestAppStatus(responseSupplier, delay, statsHolder);
            }
            default -> {
                LOGGER.error("Unknown type of result, error will be thrown");
                throw new RuntimeException("Result type is incorrect");
            }
        });
    }

    private Supplier<ApplicationStatusResponse.TimedStatusResponse> supplyTimed(
            Supplier<ProcessingResponse> responseSupplier) {
        return () -> {
            Instant start = Instant.now();
            ProcessingResponse processingResponse = responseSupplier.get();
            return new ApplicationStatusResponse.TimedStatusResponse(Duration.
                    between(start, Instant.now()), processingResponse);
        };
    }
}
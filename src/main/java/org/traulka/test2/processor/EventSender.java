package org.traulka.test2.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traulka.test2.model.MessageBody;
import org.traulka.test2.model.ReadSendClient;
import org.traulka.test2.model.type.DestResult;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EventSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSender.class);
    private final AtomicInteger parallelism = new AtomicInteger(Runtime.getRuntime().availableProcessors());
    private final ExecutorService executor = Executors.newWorkStealingPool(parallelism.get());
    private final ReadSendClient client;
    private final Duration timeout;

    public EventSender(ReadSendClient client, Duration timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    public void send(MessageBody message) {
        internalSend(message, this.executor);
    }

    private CompletableFuture<DestResult> internalSend(MessageBody message, Executor taskExecutor) {
        return CompletableFuture
                .supplyAsync(() -> {
                    parallelism.incrementAndGet();
                    return client.sendData(message.address(), message.payload());
                }, taskExecutor)
                .thenComposeAsync(result -> {
                    CompletableFuture<DestResult> destResultCompletableFuture;
                    if (DestResult.REJECTED.equals(result)) {
                        var delay = CompletableFuture.delayedExecutor(timeout.toMillis(), TimeUnit.MILLISECONDS, taskExecutor);
                        destResultCompletableFuture = this.internalSend(message, delay);
                    } else {
                        destResultCompletableFuture = CompletableFuture.completedFuture(result);
                    }
                    return destResultCompletableFuture;
                }, executor)
                .handleAsync((result, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("Message was not sent", throwable);
                    }
                    return result;
                }, executor);
    }

    public AtomicInteger getParallelism() {
        return parallelism;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
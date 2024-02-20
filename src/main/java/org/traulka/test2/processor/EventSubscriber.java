package org.traulka.test2.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traulka.test2.model.MessageBody;
import org.traulka.test2.model.ReadSendClient;

import java.time.Duration;
import java.util.concurrent.Flow;

public class EventSubscriber implements Flow.Subscriber<MessageBody> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSubscriber.class);
    private final EventSender sender;
    private Flow.Subscription subscription;

    public EventSubscriber(ReadSendClient client, Duration timeout) {
        this.sender = new EventSender(client, timeout);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(this.sender.getParallelism().get());
    }

    @Override
    public void onNext(MessageBody item) {
        this.sender.send(item);
        this.subscription.request(this.sender.getParallelism().get());
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.error("Read error", throwable);
    }

    @Override
    public void onComplete() {
        this.sender.getExecutor().shutdown();
    }
}
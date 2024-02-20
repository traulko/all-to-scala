package org.traulka.test2.handler;

import org.traulka.test2.model.ReadSendClient;
import org.traulka.test2.processor.EventSubscriber;
import org.traulka.test2.processor.FlowReader;

import java.time.Duration;

public class SendHandler implements OperationHandler {
    private final ReadSendClient client;
    private final FlowReader flowReader;
    private final Duration duration;

    public SendHandler(ReadSendClient client, Duration timeout) {
        this.client = client;
        this.duration = timeout;
        this.flowReader = new FlowReader(client);
    }

    @Override
    public Duration timeout() {
        return this.duration;
    }

    @Override
    public void performOperation() {
        this.flowReader
                .asPublisher()
                .subscribe(new EventSubscriber(this.client, this.duration));
    }
}
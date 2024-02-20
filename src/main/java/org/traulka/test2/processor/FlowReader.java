package org.traulka.test2.processor;

import org.traulka.test2.model.DestEvent;
import org.traulka.test2.model.MessageBody;
import org.traulka.test2.model.ReadSendClient;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlowReader {
    private final SubmissionPublisher<MessageBody> flowPublisher;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public FlowReader(ReadSendClient client) {
        flowPublisher = new SubmissionPublisher<>();
        Thread eventReaderThread = new Thread(() -> {
            while (isRunning.get()) {
                DestEvent data = client.readData();
                data.recipients().stream()
                        .map(a -> new MessageBody(a, data.destPayload())).forEach(flowPublisher::submit);
            }
        }, "event-reader-thread");
    }

    public Flow.Publisher<MessageBody> asPublisher() {
        return flowPublisher;
    }
}
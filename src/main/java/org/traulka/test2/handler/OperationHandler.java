package org.traulka.test2.handler;

import java.time.Duration;

public interface OperationHandler {
    void performOperation();
    Duration timeout();
}
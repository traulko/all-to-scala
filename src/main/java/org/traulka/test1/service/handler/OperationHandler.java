package org.traulka.test1.service.handler;

import org.traulka.test1.model.ApplicationStatusResponse;

public interface OperationHandler {
    ApplicationStatusResponse performOperation(String id);
}
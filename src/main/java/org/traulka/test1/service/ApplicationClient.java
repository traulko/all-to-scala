package org.traulka.test1.service;

import org.traulka.test1.model.ProcessingResponse;

public interface ApplicationClient {
    ProcessingResponse getApplicationStatus1(String id);
    ProcessingResponse getApplicationStatus2(String id);
}
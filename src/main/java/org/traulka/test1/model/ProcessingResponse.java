package org.traulka.test1.model;

import java.time.Duration;

public interface ProcessingResponse {
    record Success(String applicationStatus, String applicationId) implements ProcessingResponse {}
    record RetryAfter(Duration delay) implements ProcessingResponse {}
    record Failure(Throwable ex) implements ProcessingResponse {}
}
package org.traulka.test1.model;

import javax.annotation.Nullable;
import java.time.Duration;

public sealed interface ApplicationStatusResponse {
    record Failure(@Nullable Duration lastRequestTime, int retriesCount) implements ApplicationStatusResponse {}
    record Success(String id, String status) implements ApplicationStatusResponse {}
    record TimedStatusResponse(Duration requestDuration, ProcessingResponse result) implements ApplicationStatusResponse {}
}
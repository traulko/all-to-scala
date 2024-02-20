package org.traulka.test1.service.handler;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.junit.jupiter.MockitoExtension;
import org.traulka.test1.model.ApplicationStatusResponse;
import org.traulka.test1.model.ProcessingResponse;
import org.traulka.test1.service.ApplicationClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParallelStatusHandlerTest {
    @Mock
    ApplicationClient client;
    @InjectMocks
    ParallelStatusHandler handler;

    @Test
    @Timeout(value = 3100, unit = TimeUnit.MILLISECONDS)
    void failureTest() {
        when(client.getApplicationStatus2(any())).thenAnswer(new AnswersWithDelay(3000,
                new Returns(new ProcessingResponse.Failure(new RuntimeException()))));
        when(client.getApplicationStatus1(any())).thenAnswer(new AnswersWithDelay(5000,
                new Returns(new ProcessingResponse.Failure(new RuntimeException()))));
        var result = handler.performOperation("123");
        assertThat(result).isInstanceOfSatisfying(ApplicationStatusResponse.Failure.class, (f) -> {
            assertThat(f.retriesCount()).isZero();
            assertThat(f.lastRequestTime()).isCloseTo(Duration.ofMillis(3000), Duration.ofMillis(100));
        });
    }

    @Test
    @Timeout(value = 3100, unit = TimeUnit.MILLISECONDS)
    void successTest() {
        when(client.getApplicationStatus1(any())).thenAnswer(new AnswersWithDelay(5000,
                new Returns(new ProcessingResponse.Success("STATUS", "1"))));
        when(client.getApplicationStatus2(any())).thenAnswer(new AnswersWithDelay(3000,
                new Returns(new ProcessingResponse.Success("STATUS", "1"))));
        var result = handler.performOperation("123");
        assertThat(result).isInstanceOfSatisfying(ApplicationStatusResponse.Success.class, s -> {
            assertThat(s.status()).isEqualTo("STATUS");
            assertThat(s.id()).isEqualTo("1");
        });
    }

    @Test
    @Timeout(value = 15100, unit = TimeUnit.MILLISECONDS)
    void retryTest() {
        when(client.getApplicationStatus1(any())).thenAnswer(new AnswersWithDelay(1000,
                new Returns(new ProcessingResponse.RetryAfter(Duration.ofMillis(100)))));
        when(client.getApplicationStatus2(any())).thenAnswer(new AnswersWithDelay(1000,
                new Returns(new ProcessingResponse.RetryAfter(Duration.ofMillis(100)))));
        var result = handler.performOperation("123");
        assertThat(result).isInstanceOfSatisfying(ApplicationStatusResponse.Failure.class, s -> {
            assertThat(s.retriesCount()).isCloseTo(25, Offset.offset(5));
        });
    }
}
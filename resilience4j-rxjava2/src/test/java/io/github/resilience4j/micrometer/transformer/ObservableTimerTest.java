/*
 * Copyright 2023 Mariusz Kopylec
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.micrometer.transformer;

import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.reactivex.Observable;
import org.junit.Test;

import java.util.List;

import static io.github.resilience4j.micrometer.TimerAssertions.thenFailureTimed;
import static io.github.resilience4j.micrometer.TimerAssertions.thenSuccessTimed;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.api.BDDAssertions.then;

public class ObservableTimerTest {

    @Test
    public void shouldTimeSuccessfulObservable() {
        List<String> messages = List.of("Hello 1", "Hello 2", "Hello 3");
        MeterRegistry registry = new SimpleMeterRegistry();
        Timer timer = Timer.of("timer 1", registry);
        List<String> result = Observable.fromIterable(messages)
                .compose(TimerTransformer.of(timer))
                .toList()
                .blockingGet();

        then(result).containsExactlyInAnyOrderElementsOf(messages);
        thenSuccessTimed(registry, timer);
    }

    @Test
    public void shouldTimeFailedObservable() {
        IllegalStateException exception = new IllegalStateException();
        MeterRegistry registry = new SimpleMeterRegistry();
        TimerConfig config = TimerConfig.custom()
                .onFailureTagResolver(ex -> {
                    then(ex).isEqualTo(exception);
                    return ex.toString();
                })
                .build();
        Timer timer = Timer.of("timer 1", registry, config);
        try {
            Observable.error(exception)
                    .compose(TimerTransformer.of(timer))
                    .toList()
                    .blockingGet();

            failBecauseExceptionWasNotThrown(exception.getClass());
        } catch (Exception e) {
            thenFailureTimed(registry, timer, e);
        }
    }
}

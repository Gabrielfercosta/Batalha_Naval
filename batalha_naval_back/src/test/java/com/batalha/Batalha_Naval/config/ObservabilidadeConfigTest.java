package com.batalha.Batalha_Naval.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObservabilidadeConfigTest {

    @Test
    void timedAspectCriado() {
        ObservabilidadeConfig config = new ObservabilidadeConfig();
        MeterRegistry registry = new SimpleMeterRegistry();

        TimedAspect aspect = config.timedAspect(registry);

        assertNotNull(aspect);
    }
}

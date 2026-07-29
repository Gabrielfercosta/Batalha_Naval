package com.batalha.Batalha_Naval.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Detecta consultas ao banco com alto tempo de resposta.
 * Cada query também gera um span no trace enviado ao Tempo, permitindo
 * identificar qual consulta específica tornou uma requisição lenta.
 */
@Configuration
public class SlowQueryConfig {

    private static final Logger log = LoggerFactory.getLogger(SlowQueryConfig.class);
    private static final long SLOW_QUERY_THRESHOLD_MS = 100;

    @Bean
    public QueryExecutionListener slowQueryListener(MeterRegistry registry) {
        Counter slowQueryCounter = Counter.builder("db.queries.slow")
                .description("Consultas ao banco acima de " + SLOW_QUERY_THRESHOLD_MS + "ms")
                .register(registry);

        return new QueryExecutionListener() {
            @Override
            public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
                // Nada a fazer antes da execução
            }

            @Override
            public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
                long durationMs = execInfo.getElapsedTime();
                if (durationMs > SLOW_QUERY_THRESHOLD_MS) {
                    slowQueryCounter.increment();
                    String sql = queryInfoList.stream()
                            .map(QueryInfo::getQuery)
                            .collect(Collectors.joining("; "));
                    log.warn("QUERY LENTA: {}ms | SQL: {}", durationMs, sql);
                }
            }
        };
    }
}

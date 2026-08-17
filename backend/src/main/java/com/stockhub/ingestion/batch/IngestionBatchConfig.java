package com.stockhub.ingestion.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Placeholder for ETL ingestion jobs.
 * <p>
 * Spring Batch starter was removed in Spring Boot 4.x. When ready to
 * implement ETL, add {@code spring-boot-starter-batch} back to pom.xml
 * and implement chunk-based processing against the
 * {@code FinancialDataProvider} interface.
 * </p>
 */
@Configuration
@Profile("!local")
public class IngestionBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(IngestionBatchConfig.class);

    public IngestionBatchConfig() {
        log.info("ETL ingestion is disabled. Add spring-boot-starter-batch to enable.");
    }
}

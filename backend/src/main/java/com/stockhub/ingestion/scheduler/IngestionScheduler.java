package com.stockhub.ingestion.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Placeholder scheduler for ETL ingestion.
 * <p>
 * Spring Batch was removed in Spring Boot 4.x. When ready to implement
 * ETL, add {@code spring-boot-starter-batch} back to pom.xml and wire
 * real batch jobs here.
 * </p>
 */
@Component
@ConditionalOnProperty(name = "stockhub.ingestion.enabled", havingValue = "true")
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    public IngestionScheduler() {
        log.info("Ingestion scheduler placeholder — ETL disabled. Add spring-boot-starter-batch to enable.");
    }
}

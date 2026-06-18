package com.stockhub.ingestion.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled launcher for ETL ingestion jobs.
 * <p>
 * Controlled by {@code stockhub.ingestion.enabled} property.
 * When disabled, none of the scheduled methods will be registered.
 * </p>
 */
@Component
@ConditionalOnProperty(name = "stockhub.ingestion.enabled", havingValue = "true", matchIfMissing = true)
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job fundamentalsIngestionJob;
    private final Job priceIngestionJob;

    public IngestionScheduler(JobLauncher jobLauncher,
                              @Qualifier("fundamentalsIngestionJob") Job fundamentalsIngestionJob,
                              @Qualifier("priceIngestionJob") Job priceIngestionJob) {
        this.jobLauncher = jobLauncher;
        this.fundamentalsIngestionJob = fundamentalsIngestionJob;
        this.priceIngestionJob = priceIngestionJob;
    }

    /**
     * Run daily price ingestion.
     * Default cron: 6:00 PM ET (after market close).
     * Configure via {@code stockhub.ingestion.price-cron}.
     */
    @Scheduled(cron = "${stockhub.ingestion.price-cron:0 0 18 * * ?}",
               zone = "America/New_York")
    public void runPriceIngestion() {
        log.info("Starting scheduled price ingestion job");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("triggeredAt", System.currentTimeMillis())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(priceIngestionJob, params);
            log.info("Price ingestion job completed successfully");
        } catch (Exception e) {
            log.error("Price ingestion job failed", e);
        }
    }

    /**
     * Run fundamentals ingestion (usually daily after SEC filings).
     * Default cron: 2:00 AM ET (off-peak).
     * Configure via {@code stockhub.ingestion.fundamentals-cron}.
     */
    @Scheduled(cron = "${stockhub.ingestion.fundamentals-cron:0 0 2 * * ?}",
               zone = "America/New_York")
    public void runFundamentalsIngestion() {
        log.info("Starting scheduled fundamentals ingestion job");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("triggeredAt", System.currentTimeMillis())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(fundamentalsIngestionJob, params);
            log.info("Fundamentals ingestion job completed successfully");
        } catch (Exception e) {
            log.error("Fundamentals ingestion job failed", e);
        }
    }
}

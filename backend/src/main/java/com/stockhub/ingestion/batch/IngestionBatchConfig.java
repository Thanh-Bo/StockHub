package com.stockhub.ingestion.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration for ETL ingestion jobs.
 * <p>
 * When real data providers are configured, the placeholder tasklets
 * can be replaced with chunk-based steps that read from providers,
 * transform to entities, and write to the database.
 * </p>
 */
@Configuration
@EnableBatchProcessing
public class IngestionBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(IngestionBatchConfig.class);

    /**
     * Fundamentals ingestion job: company profiles, income statements,
     * balance sheets, and cash flow statements.
     */
    @Bean
    public Job fundamentalsIngestionJob(JobRepository jobRepository,
                                        @Qualifier("fundamentalsUpsertStep") Step upsertStep) {
        return new JobBuilder("fundamentalsIngestionJob", jobRepository)
                .start(upsertStep)
                .build();
    }

    /**
     * Price ingestion job: daily stock price history.
     */
    @Bean
    public Job priceIngestionJob(JobRepository jobRepository,
                                 @Qualifier("priceUpsertStep") Step upsertStep) {
        return new JobBuilder("priceIngestionJob", jobRepository)
                .start(upsertStep)
                .build();
    }

    /**
     * Placeholder step for fundamentals ingestion.
     * Replaced with chunk-based step when real providers are configured.
     */
    @Bean
    public Step fundamentalsUpsertStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager) {
        return new StepBuilder("fundamentalsUpsertStep", jobRepository)
                .tasklet(fundamentalsPlaceholderTasklet(), transactionManager)
                .build();
    }

    /**
     * Placeholder step for price ingestion.
     * Replaced with chunk-based step when real providers are configured.
     */
    @Bean
    public Step priceUpsertStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("priceUpsertStep", jobRepository)
                .tasklet(pricePlaceholderTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet fundamentalsPlaceholderTasklet() {
        return (contribution, chunkContext) -> {
            log.info("ETL job placeholder - configure data providers to enable fundamentals ingestion");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet pricePlaceholderTasklet() {
        return (contribution, chunkContext) -> {
            log.info("ETL job placeholder - configure data providers to enable price ingestion");
            return RepeatStatus.FINISHED;
        };
    }
}

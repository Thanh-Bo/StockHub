package com.stockhub.ingestion.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of validating a data record from a financial data provider.
 */
public record ValidationResult(boolean valid, List<String> errors) {

    /**
     * Factory method for a successful validation (no errors).
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    /**
     * Factory method for a failed validation.
     *
     * @param errors list of error messages
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, Collections.unmodifiableList(new ArrayList<>(errors)));
    }

    /**
     * Convenience factory for a single error message.
     */
    public static ValidationResult failure(String error) {
        return new ValidationResult(false, List.of(error));
    }
}

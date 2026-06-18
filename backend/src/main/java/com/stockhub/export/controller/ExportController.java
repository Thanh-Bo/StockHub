package com.stockhub.export.controller;

import com.stockhub.export.dto.ExportRequest;
import com.stockhub.export.service.ExportService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * REST controller for data export endpoints.
 * Only accessible to PREMIUM users.
 */
@RestController
@RequestMapping("/api/v1/exports")
@PreAuthorize("hasRole('PREMIUM')")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Export financial data as an Excel (.xlsx) file.
     */
    @PostMapping("/excel")
    public ResponseEntity<StreamingResponseBody> exportExcel(
            @Valid @RequestBody ExportRequest request) {
        log.info("Excel export requested for {} tickers, type={}, period={}",
                request.tickers().size(), request.type(), request.period());

        byte[] excelBytes = exportService.generateExcel(request);

        StreamingResponseBody stream = outputStream -> {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bais.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        };

        String filename = "stockhub-export-" + System.currentTimeMillis() + ".xlsx";
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(stream);
    }

    /**
     * Export financial data as a PDF file.
     */
    @PostMapping("/pdf")
    public ResponseEntity<StreamingResponseBody> exportPdf(
            @Valid @RequestBody ExportRequest request) {
        log.info("PDF export requested for {} tickers, type={}, period={}",
                request.tickers().size(), request.type(), request.period());

        byte[] pdfBytes = exportService.generatePdf(request);

        StreamingResponseBody stream = outputStream -> {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(pdfBytes)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bais.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        };

        String filename = "stockhub-export-" + System.currentTimeMillis() + ".pdf";
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(stream);
    }
}

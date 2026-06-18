package com.stockhub.export.service;

import com.stockhub.common.enums.PeriodType;
import com.stockhub.common.exception.CompanyNotFoundException;
import com.stockhub.common.exception.ExportException;
import com.stockhub.company.entity.Company;
import com.stockhub.company.repository.CompanyRepository;
import com.stockhub.export.dto.ExportRequest;
import com.stockhub.financials.dto.IncomeStatementResponse;
import com.stockhub.financials.service.FinancialService;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for exporting financial data to Excel and PDF formats.
 */
@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final FinancialService financialService;
    private final CompanyRepository companyRepository;

    public ExportService(FinancialService financialService,
                         CompanyRepository companyRepository) {
        this.financialService = financialService;
        this.companyRepository = companyRepository;
    }

    /**
     * Generate an Excel workbook from the given export request.
     * <p>
     * Each ticker gets its own sheet. Each sheet has:
     * <ul>
     *   <li>Header row with fiscal years</li>
     *   <li>Data rows for revenue, gross profit, operating income, net income, EPS, EBITDA</li>
     * </ul>
     * </p>
     *
     * @param request the export request
     * @return Excel workbook bytes
     */
    public byte[] generateExcel(ExportRequest request) {
        PeriodType period = parsePeriodType(request.period());
        List<String> tickers = request.tickers();

        try (Workbook workbook = new XSSFWorkbook()) {

            for (String ticker : tickers) {
                // Validate company exists before attempting export
                if (companyRepository.findByTicker(ticker.toUpperCase()).isEmpty()) {
                    throw new CompanyNotFoundException(ticker);
                }
                List<IncomeStatementResponse> statements =
                        financialService.getIncomeStatements(
                                ticker, period, request.years(), 0, request.years() + 1)
                                .content();

                if (statements.isEmpty()) {
                    log.debug("No income statements found for {} with period {}", ticker, period);
                    continue;
                }

                Sheet sheet = workbook.createSheet(sanitizeSheetName(ticker.toUpperCase()));

                // Sort by fiscal year descending for display
                statements.sort((a, b) -> Integer.compare(b.fiscalYear(), a.fiscalYear()));

                // Header row
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("Metric");
                int col = 1;
                for (IncomeStatementResponse stmt : statements) {
                    headerRow.createCell(col++).setCellValue(
                            String.valueOf(stmt.fiscalYear()));
                }

                // Data rows
                String[] metricNames = {
                        "Total Revenue", "Gross Profit", "Operating Income",
                        "Net Income", "EPS", "EBITDA"
                };
                List<java.util.function.Function<IncomeStatementResponse, BigDecimal>> extractors =
                        List.of(
                                IncomeStatementResponse::totalRevenue,
                                IncomeStatementResponse::grossProfit,
                                IncomeStatementResponse::operatingIncome,
                                IncomeStatementResponse::netIncome,
                                IncomeStatementResponse::eps,
                                IncomeStatementResponse::ebitda
                        );

                for (int rowIdx = 0; rowIdx < metricNames.length; rowIdx++) {
                    Row dataRow = sheet.createRow(rowIdx + 1);
                    dataRow.createCell(0).setCellValue(metricNames[rowIdx]);
                    int dataCol = 1;
                    for (IncomeStatementResponse stmt : statements) {
                        BigDecimal value = extractors.get(rowIdx).apply(stmt);
                        if (value != null) {
                            dataRow.createCell(dataCol).setCellValue(value.doubleValue());
                        }
                        dataCol++;
                    }
                }

                // Auto-size columns
                for (int i = 0; i <= statements.size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // If no sheets were created, add a placeholder
            if (workbook.getNumberOfSheets() == 0) {
                Sheet sheet = workbook.createSheet("No Data");
                Row row = sheet.createRow(0);
                row.createCell(0).setCellValue("No financial data found for the requested tickers.");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new ExportException("Failed to generate Excel export", e);
        }
    }

    /**
     * Generate a PDF document from the given export request.
     * <p>
     * Uses iText 7 to create a landscape A4 document with a table per ticker.
     * </p>
     *
     * @param request the export request
     * @return PDF document bytes
     */
    public byte[] generatePdf(ExportRequest request) {
        PeriodType period = parsePeriodType(request.period());
        List<String> tickers = request.tickers();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4.rotate());
        document.setMargins(20, 20, 20, 20);

        // Title
        Paragraph title = new Paragraph("StockHub - Financial Data Export")
                .setFontSize(18)
                .setBold();
        document.add(title);

        Paragraph subtitle = new Paragraph(
                "Generated: " + java.time.LocalDateTime.now().toString()
                + " | Period: " + request.period()
                + " | Years: " + request.years())
                .setFontSize(10);
        document.add(subtitle);
        document.add(new Paragraph("\n"));

        for (String ticker : tickers) {
            Company company = resolveCompany(ticker);
            List<IncomeStatementResponse> statements =
                    financialService.getIncomeStatements(
                            ticker, period, request.years(), 0, request.years() + 1)
                            .content();

            if (statements.isEmpty()) {
                continue;
            }

            statements.sort((a, b) -> Integer.compare(b.fiscalYear(), a.fiscalYear()));

            // Company header
            Paragraph companyHeader = new Paragraph(
                    company.getTicker().toUpperCase() + " - " + company.getName())
                    .setFontSize(14)
                    .setBold();
            document.add(companyHeader);

            // Build table: 1 header column + N fiscal year columns
            int numCols = 1 + statements.size();
            Table table = new Table(UnitValue.createPercentArray(numCols));
            table.setWidth(UnitValue.createPercentValue(100));

            // Header row
            table.addHeaderCell(new Cell().add(new Paragraph("Metric").setBold()));
            for (IncomeStatementResponse stmt : statements) {
                table.addHeaderCell(new Cell().add(
                        new Paragraph(String.valueOf(stmt.fiscalYear())).setBold()));
            }

            // Data rows
            addPdfDataRow(table, "Total Revenue", statements,
                    IncomeStatementResponse::totalRevenue);
            addPdfDataRow(table, "Gross Profit", statements,
                    IncomeStatementResponse::grossProfit);
            addPdfDataRow(table, "Operating Income", statements,
                    IncomeStatementResponse::operatingIncome);
            addPdfDataRow(table, "Net Income", statements,
                    IncomeStatementResponse::netIncome);
            addPdfDataRow(table, "EPS", statements,
                    IncomeStatementResponse::eps);
            addPdfDataRow(table, "EBITDA", statements,
                    IncomeStatementResponse::ebitda);

            document.add(table);
            document.add(new Paragraph("\n"));
        }

        document.close();
        return baos.toByteArray();
    }

    // --- Private helpers ---

    private void addPdfDataRow(Table table, String metricName,
                               List<IncomeStatementResponse> statements,
                               java.util.function.Function<IncomeStatementResponse, BigDecimal> extractor) {
        table.addCell(new Cell().add(new Paragraph(metricName)));
        for (IncomeStatementResponse stmt : statements) {
            BigDecimal value = extractor.apply(stmt);
            table.addCell(new Cell().add(new Paragraph(
                    value != null ? value.toPlainString() : "-")));
        }
    }

    private Company resolveCompany(String ticker) {
        return companyRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new CompanyNotFoundException(ticker));
    }

    private PeriodType parsePeriodType(String period) {
        if (period == null) {
            return PeriodType.ANNUAL;
        }
        return PeriodType.valueOf(period.toUpperCase());
    }

    /**
     * Sanitize a ticker for use as an Excel sheet name (max 31 chars, no special chars).
     */
    private String sanitizeSheetName(String ticker) {
        String cleaned = ticker.replaceAll("[\\\\/*?:\\[\\]]", "");
        if (cleaned.length() > 31) {
            cleaned = cleaned.substring(0, 31);
        }
        return cleaned;
    }
}

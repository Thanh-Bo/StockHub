# Section 16: Export System

## 16.1 Export Features (PREMIUM tier)

| Export Type | Scope | Library |
|-------------|-------|---------|
| Excel (.xlsx) | Financial statements, screener results, comparison data | Apache POI 5.x |
| PDF (.pdf) | Company dashboard summary, financial statements | iText 8.x |

---

## 16.2 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/exports/excel` | Generate Excel file |
| `POST` | `/api/v1/exports/pdf` | Generate PDF file |

**Request:**
```json
{
  "type": "FINANCIAL_STATEMENTS",
  "tickers": ["AAPL", "MSFT"],
  "statementType": "INCOME_STATEMENT",
  "period": "ANNUAL",
  "years": 5
}
```

**Response**: Binary file download with appropriate Content-Type and Content-Disposition headers.

---

## 16.3 Export Service Design

```java
package com.stockhub.export;

@Service
public class ExportService {

    private final FinancialService financialService;
    private final MetricCalculationService metricService;
    private final CompanyRepository companyRepo;

    // --- EXCEL ---
    public byte[] generateExcel(ExportRequest request) {
        return switch (request.type()) {
            case FINANCIAL_STATEMENTS -> generateFinancialStatementsExcel(request);
            case SCREENER_RESULTS -> generateScreenerExcel(request);
            case COMPARISON -> generateComparisonExcel(request);
        };
    }

    private byte[] generateFinancialStatementsExcel(ExportRequest request) {
        try (Workbook workbook = new XSSFWorkbook()) {
            for (String ticker : request.tickers()) {
                Sheet sheet = workbook.createSheet(ticker);

                // Fetch data
                List<IncomeStatement> statements = financialService
                    .getIncomeStatements(ticker, request.period(), request.years());

                // Header row
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("Metric");
                for (int i = 0; i < statements.size(); i++) {
                    headerRow.createCell(i + 1)
                        .setCellValue(statements.get(i).getFiscalYear().toString());
                }

                // Data rows
                int rowNum = 1;
                rowNum = addDataRow(sheet, rowNum, "Total Revenue", statements,
                    IncomeStatement::getTotalRevenue);
                rowNum = addDataRow(sheet, rowNum, "Gross Profit", statements,
                    IncomeStatement::getGrossProfit);
                rowNum = addDataRow(sheet, rowNum, "Operating Income", statements,
                    IncomeStatement::getOperatingIncome);
                rowNum = addDataRow(sheet, rowNum, "Net Income", statements,
                    IncomeStatement::getNetIncome);
                rowNum = addDataRow(sheet, rowNum, "EPS", statements,
                    IncomeStatement::getEps);
                rowNum = addDataRow(sheet, rowNum, "EBITDA", statements,
                    IncomeStatement::getEbitda);

                // Style
                sheet.setColumnWidth(0, 20 * 256);
                for (int i = 0; i <= statements.size(); i++) {
                    sheet.setColumnWidth(i, 15 * 256);
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new ExportException("Failed to generate Excel", e);
        }
    }

    private int addDataRow(Sheet sheet, int rowNum, String label,
                            List<IncomeStatement> statements,
                            Function<IncomeStatement, BigDecimal> extractor) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        for (int i = 0; i < statements.size(); i++) {
            BigDecimal value = extractor.apply(statements.get(i));
            if (value != null) {
                row.createCell(i + 1).setCellValue(value.doubleValue());
            }
        }
        return rowNum + 1;
    }

    // --- PDF ---
    public byte[] generatePdf(ExportRequest request) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(bos));
            Document document = new Document(pdf, PageSize.A4.rotate());

            // Title
            document.add(new Paragraph("Financial Statements Export")
                .setFontSize(18)
                .setBold());

            for (String ticker : request.tickers()) {
                Company company = companyRepo.findByTicker(ticker)
                    .orElseThrow(() -> new CompanyNotFoundException(ticker));

                document.add(new Paragraph(company.getName() + " (" + ticker + ")")
                    .setFontSize(14)
                    .setBold());

                List<IncomeStatement> statements = financialService
                    .getIncomeStatements(ticker, request.period(), request.years());

                // Create table
                Table table = new Table(statements.size() + 1);
                table.addCell("Metric");
                for (IncomeStatement stmt : statements) {
                    table.addCell(String.valueOf(stmt.getFiscalYear()));
                }

                addPdfRow(table, "Revenue", statements, IncomeStatement::getTotalRevenue);
                addPdfRow(table, "Net Income", statements, IncomeStatement::getNetIncome);
                addPdfRow(table, "EPS", statements, IncomeStatement::getEps);

                document.add(table);
                document.add(new AreaBreak());
            }

            document.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new ExportException("Failed to generate PDF", e);
        }
    }

    private void addPdfRow(Table table, String label, List<IncomeStatement> statements,
                            Function<IncomeStatement, BigDecimal> extractor) {
        table.addCell(label);
        for (IncomeStatement stmt : statements) {
            BigDecimal value = extractor.apply(stmt);
            table.addCell(value != null
                ? NumberFormat.getNumberInstance().format(value.doubleValue())
                : "N/A");
        }
    }
}
```

---

## 16.4 Controller (Async — Large Files)

```java
@RestController
@RequestMapping("/api/v1/exports")
@PreAuthorize("hasRole('PREMIUM')")
public class ExportController {

    private final ExportService exportService;

    @PostMapping("/excel")
    public ResponseEntity<StreamingResponseBody> exportExcel(
            @Valid @RequestBody ExportRequest request) {

        byte[] data = exportService.generateExcel(request);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=stockhub_export_" + Instant.now().toEpochMilli() + ".xlsx")
            .contentLength(data.length)
            .body(outputStream -> outputStream.write(data));
    }

    @PostMapping("/pdf")
    public ResponseEntity<StreamingResponseBody> exportPdf(
            @Valid @RequestBody ExportRequest request) {

        byte[] data = exportService.generatePdf(request);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=stockhub_export_" + Instant.now().toEpochMilli() + ".pdf")
            .contentLength(data.length)
            .body(outputStream -> outputStream.write(data));
    }
}
```

---

## 16.5 Export Generation Flow

```
User clicks "Export to Excel" on Dashboard
        │
        ▼
Angular: POST /api/v1/exports/excel
  {
    "type": "FINANCIAL_STATEMENTS",
    "tickers": ["AAPL"],
    "statementType": "INCOME_STATEMENT",
    "period": "ANNUAL",
    "years": 5
  }
        │
        ▼
Spring Boot: ExportController.exportExcel()
  → Validate: User has PREMIUM role
  → ExportService.generateExcel(request)
     → For each ticker: fetch data from DB
     → Create XSSFWorkbook (Apache POI)
     → Populate sheet rows
     → Auto-size columns
     → Apply number formatting
     → Write to byte[]
  → Return StreamingResponseBody
        │
        ▼
Browser: Triggers file download
  → "stockhub_export_1687459200.xlsx"
```

---

## 16.6 Performance Considerations

| Concern | Mitigation |
|---------|-----------|
| Large exports (10+ companies, 10 years) | Stream response (don't buffer entire workbook) |
| Memory (XSSFWorkbook can be heavy) | Limit to 20 companies × 10 years per export |
| Concurrency | Premium tier only (low volume); no special throttling needed |
| Timeout | Controller uses async response; 60-second timeout |

package com.stockhub.financials.controller;

import com.stockhub.common.dto.PagedResponse;
import com.stockhub.common.enums.PeriodType;
import com.stockhub.financials.dto.BalanceSheetResponse;
import com.stockhub.financials.dto.CashFlowStatementResponse;
import com.stockhub.financials.dto.IncomeStatementResponse;
import com.stockhub.financials.service.FinancialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies/{ticker}")
public class FinancialController {

    private final FinancialService financialService;

    public FinancialController(FinancialService financialService) {
        this.financialService = financialService;
    }

    @GetMapping("/income-statements")
    public ResponseEntity<PagedResponse<IncomeStatementResponse>> getIncomeStatements(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "ANNUAL") String period,
            @RequestParam(defaultValue = "5") int years,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PeriodType periodType = PeriodType.valueOf(period.toUpperCase());
        PagedResponse<IncomeStatementResponse> response =
                financialService.getIncomeStatements(ticker, periodType, years, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance-sheets")
    public ResponseEntity<PagedResponse<BalanceSheetResponse>> getBalanceSheets(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "ANNUAL") String period,
            @RequestParam(defaultValue = "5") int years,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PeriodType periodType = PeriodType.valueOf(period.toUpperCase());
        PagedResponse<BalanceSheetResponse> response =
                financialService.getBalanceSheets(ticker, periodType, years, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cash-flow-statements")
    public ResponseEntity<PagedResponse<CashFlowStatementResponse>> getCashFlowStatements(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "ANNUAL") String period,
            @RequestParam(defaultValue = "5") int years,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PeriodType periodType = PeriodType.valueOf(period.toUpperCase());
        PagedResponse<CashFlowStatementResponse> response =
                financialService.getCashFlowStatements(ticker, periodType, years, page, size);
        return ResponseEntity.ok(response);
    }
}

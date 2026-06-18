import { PricePoint } from '@app/shared/models/shared.models';

export interface IndustryContext {
  sector: string;
  industry: string;
  avgPE: number;
  avgROE: number;
  avgRevenueGrowth: number;
  avgNetMargin: number;
  pePercentile: number;
  roePercentile: number;
}

export interface DashboardData {
  ticker: string;
  name: string;
  description: string;
  sector: string;
  industry: string;
  headquarters: string;
  marketCap: number;
  employees: number;
  currentPrice: number;
  priceChange: number;
  priceChangePercent: number;
  dayHigh: number;
  dayLow: number;
  previousClose: number;
  volume: number;
  priceHistory: PricePoint[];
  revenueGrowthYoY: number;
  epsGrowthYoY: number;
  roe: number;
  roa: number;
  peRatio: number;
  grossMargin: number;
  netMargin: number;
  debtToEquity: number;
  dividendYield: number;
  industryContext: IndustryContext;
  lastUpdated: string;
  dataSource: string;
}

export interface FinancialStatementRow {
  fiscalYear: number;
  [key: string]: any;
}

export interface PeerComparisonRow {
  ticker: string;
  name: string;
  metrics: Record<string, number>;
}

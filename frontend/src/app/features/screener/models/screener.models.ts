export interface FilterCriteria {
  field: string;
  operator: 'GREATER_THAN' | 'LESS_THAN' | 'EQUALS' | 'BETWEEN' | 'IN' | 'GREATER_THAN_OR_EQUAL' | 'LESS_THAN_OR_EQUAL';
  value?: number;
  minValue?: number;
  maxValue?: number;
  values?: string[];
}

export interface SortConfig {
  field: string;
  direction: 'ASC' | 'DESC';
}

export interface PaginationConfig {
  page: number;
  size: number;
}

export interface ScreenerRequest {
  filters: FilterCriteria[];
  sort: SortConfig;
  pagination: PaginationConfig;
}

export interface ScreenerResultItem {
  ticker: string;
  name: string;
  sector: string;
  industry: string;
  marketCap: number;
  peRatio: number;
  revenueGrowthYoY: number;
  roe: number;
  dividendYield: number;
  debtToEquity: number;
  netMargin: number;
}

export interface ScreenerResponse {
  content: ScreenerResultItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface FilterMetadata {
  field: string;
  label: string;
  type: 'range' | 'multi-select' | 'single-select';
  minValue?: number;
  maxValue?: number;
  options?: FilterOption[];
}

export interface FilterOption {
  label: string;
  value: string;
}

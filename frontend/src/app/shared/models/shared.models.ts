export interface PricePoint {
  date: string;
  close: number;
  adjustedClose: number;
  volume: number;
}

export interface PaginationParams {
  page: number;
  size: number;
}

export interface SortParams {
  field: string;
  direction: 'ASC' | 'DESC';
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface MetricDefinition {
  label: string;
  value: number | null;
  format: string;
  tooltip: string;
  trend?: 'up' | 'down' | 'neutral';
}

export interface ColumnDef {
  field: string;
  header: string;
  format?: string;
  align?: 'left' | 'right' | 'center';
}

export interface FilterConfig {
  field: string;
  label: string;
  type: 'range' | 'select';
  min?: number;
  max?: number;
  options?: FilterOption[];
}

export interface FilterOption {
  label: string;
  value: string | number;
  selected?: boolean;
}

export interface AutocompleteResult {
  ticker: string;
  name: string;
  sector: string;
  exchange?: string;
}

export interface DashboardData {
  ticker: string;
  name: string;
  sector: string;
  industry: string;
  price: number;
  priceChange: number;
  priceChangePercent: number;
  marketCap: number;
  currency: string;
  metrics: MetricDefinition[];
  priceHistory: PricePoint[];
  description?: string;
}

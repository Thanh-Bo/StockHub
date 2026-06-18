export interface ComparisonResponse {
  companies: CompanyComparisonRow[];
  industryAverages?: IndustryAveragesResponse;
}

export interface CompanyComparisonRow {
  ticker: string;
  name: string;
  sector: string;
  industry: string;
  metrics: Record<string, number>;
}

export interface IndustryAveragesResponse {
  sector: string;
  industry: string;
  metrics: Record<string, number>;
}

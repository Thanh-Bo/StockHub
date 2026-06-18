export interface WatchlistSummary {
  id: number;
  name: string;
  description: string;
  stockCount: number;
  isDefault: boolean;
  createdAt: string;
}

export interface WatchlistDetail {
  id: number;
  name: string;
  description: string;
  stocks: WatchlistStockSummary[];
}

export interface WatchlistStockSummary {
  ticker: string;
  name: string;
  latestPrice: number;
  priceChange: number;
  priceChangePercent: number;
  marketCap: number;
  addedAt: string;
  sortOrder: number;
}

export interface WatchlistResponse {
  id: number;
  name: string;
  description: string;
}

export interface CreateWatchlistRequest {
  name: string;
  description: string;
}

export interface UpdateWatchlistRequest {
  name: string;
  description: string;
}

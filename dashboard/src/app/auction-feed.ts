import { InjectionToken, Signal } from '@angular/core';

export type ConnectionState = 'idle' | 'connecting' | 'connected' | 'reconnecting';

export interface Bid {
  funderId: string;
  rate: number;
  termMonths: number;
  receivedAt: string;
}

// Abertura do leilão (Milestone 20): o prazo vem do servidor e alimenta a contagem regressiva.
export interface AuctionOpened {
  expiresAt: string;
}

export interface AuctionClosed {
  status: string;
  winningFunderId: string | null;
  winningRate: number | null;
}

// Origem dos dados do leilão: o STOMP real, o feed de demonstração ou um feed falso nos testes.
export interface AuctionFeed {
  readonly connection: Signal<ConnectionState>;
  readonly bids: Signal<Bid[]>;
  readonly opened: Signal<AuctionOpened | null>;
  readonly closed: Signal<AuctionClosed | null>;
  readonly watching: Signal<string | null>;
  readonly demo?: boolean;
  watch(proposalId: string): void;
  stop(): void;
}

export const AUCTION_FEED = new InjectionToken<AuctionFeed>('AuctionFeed');

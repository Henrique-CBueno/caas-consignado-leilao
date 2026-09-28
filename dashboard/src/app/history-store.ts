import { Injectable, signal } from '@angular/core';
import { AuctionClosed } from './auction-feed';

export interface HistoryEntry {
  proposalId: string;
  seenAt: string;
  outcome?: AuctionClosed;
}

const STORAGE_KEY = 'caas.history';
const MAX_ENTRIES = 20;

// Histórico só neste navegador. O armazenamento pode estar bloqueado (janela privada): nunca quebra a tela.
@Injectable({ providedIn: 'root' })
export class HistoryStore {
  readonly entries = signal<HistoryEntry[]>(this.load());

  record(proposalId: string): void {
    const others = this.entries().filter((entry) => entry.proposalId !== proposalId);
    this.update([{ proposalId, seenAt: new Date().toISOString() }, ...others]);
  }

  recordOutcome(proposalId: string, outcome: AuctionClosed): void {
    this.update(
      this.entries().map((entry) => (entry.proposalId === proposalId ? { ...entry, outcome } : entry)),
    );
  }

  clear(): void {
    this.update([]);
  }

  private update(entries: HistoryEntry[]): void {
    const kept = entries.slice(0, MAX_ENTRIES);
    this.entries.set(kept);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(kept));
    } catch {
      // sem persistência; a lista em memória continua valendo na sessão
    }
  }

  private load(): HistoryEntry[] {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
    } catch {
      return [];
    }
  }
}

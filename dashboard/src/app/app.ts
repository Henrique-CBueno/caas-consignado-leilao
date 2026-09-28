import { Component, OnDestroy, computed, effect, inject, untracked } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AUCTION_FEED, ConnectionState } from './auction-feed';
import { HistoryStore } from './history-store';
import { ThemeService } from './theme';

const CONNECTION_LABELS: Record<ConnectionState, string> = {
  idle: 'desconectado',
  connecting: 'conectando',
  connected: 'conectado',
  reconnecting: 'reconectando',
};

@Component({
  imports: [RouterLink, RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnDestroy {
  protected readonly feed = inject(AUCTION_FEED);

  private readonly history = inject(HistoryStore);
  protected readonly themeService = inject(ThemeService);

  protected readonly connection = this.feed.connection;
  protected readonly connectionLabel = computed(() => CONNECTION_LABELS[this.connection()]);

  constructor() {
    // No shell (e não na vista) para o desfecho ser guardado mesmo com outra vista aberta.
    effect(() => {
      const closed = this.feed.closed();
      const proposalId = this.feed.watching();
      if (closed && proposalId) {
        // recordOutcome lê e escreve o histórico: sem untracked o efeito se reexecutaria sem parar.
        untracked(() => this.history.recordOutcome(proposalId, closed));
      }
    });
  }

  ngOnDestroy(): void {
    this.feed.stop();
  }
}

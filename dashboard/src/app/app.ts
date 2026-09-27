import { Component, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Client, IMessage } from '@stomp/stompjs';

interface AuctionNotification {
  type: 'BID_PLACED' | 'AUCTION_CLOSED';
  payload: unknown;
}

interface BidPlacedPayload {
  funderId: string;
  rate: number;
  termMonths: number;
  receivedAt: string;
}

interface AuctionClosedPayload {
  status: string;
  winningFunderId: string | null;
  winningRate: number | null;
}

const NOTIFICATION_GATEWAY_WS_URL = 'ws://localhost:8086/ws';

@Component({
  imports: [FormsModule],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnDestroy {
  protected proposalId = '';
  protected readonly connected = signal(false);
  protected readonly bids = signal<BidPlacedPayload[]>([]);
  protected readonly closedAuction = signal<AuctionClosedPayload | null>(null);

  private client: Client | null = null;

  protected watch(): void {
    if (!this.proposalId) {
      return;
    }
    this.disconnect();
    this.bids.set([]);
    this.closedAuction.set(null);

    const client = new Client({
      brokerURL: NOTIFICATION_GATEWAY_WS_URL,
      onConnect: () => {
        this.connected.set(true);
        client.subscribe(`/topic/auctions/${this.proposalId}`, (message: IMessage) => {
          const notification: AuctionNotification = JSON.parse(message.body);
          if (notification.type === 'BID_PLACED') {
            this.bids.update((current) => [...current, notification.payload as BidPlacedPayload]);
          } else if (notification.type === 'AUCTION_CLOSED') {
            this.closedAuction.set(notification.payload as AuctionClosedPayload);
          }
        });
      },
      onWebSocketClose: () => this.connected.set(false),
    });

    client.activate();
    this.client = client;
  }

  protected disconnect(): void {
    this.client?.deactivate();
    this.client = null;
    this.connected.set(false);
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}

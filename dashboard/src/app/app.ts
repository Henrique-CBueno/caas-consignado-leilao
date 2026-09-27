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

// Dev local (ng serve): notification-gateway-service roda direto na porta 8086.
// Fora de localhost (acessado via NodePort do minikube, Milestone 9): a porta do
// serviço é a NodePort fixa definida no manifest do notification-gateway-service.
const LOCAL_DEV_PORT = 8086;
const CLUSTER_NODE_PORT = 30086;
const NOTIFICATION_GATEWAY_WS_PORT =
  window.location.hostname === 'localhost' ? LOCAL_DEV_PORT : CLUSTER_NODE_PORT;
const NOTIFICATION_GATEWAY_WS_URL = `ws://${window.location.hostname}:${NOTIFICATION_GATEWAY_WS_PORT}/ws`;

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

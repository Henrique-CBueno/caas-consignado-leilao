import { Injectable, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { AuctionClosed, AuctionFeed, Bid, ConnectionState } from './auction-feed';

interface AuctionNotification {
  type: 'BID_PLACED' | 'AUCTION_CLOSED';
  payload: unknown;
}

// Dev local (ng serve): notification-gateway-service roda direto na porta 8086.
// Fora de localhost (acessado via NodePort do minikube): a porta do serviço é a
// NodePort fixa definida no manifest do notification-gateway-service.
const LOCAL_DEV_PORT = 8086;
const CLUSTER_NODE_PORT = 30086;
const NOTIFICATION_GATEWAY_WS_PORT =
  window.location.hostname === 'localhost' ? LOCAL_DEV_PORT : CLUSTER_NODE_PORT;
const NOTIFICATION_GATEWAY_WS_URL = `ws://${window.location.hostname}:${NOTIFICATION_GATEWAY_WS_PORT}/ws`;

// Adaptador fino sobre o cliente STOMP; o comportamento de tela é testado com um feed falso.
@Injectable()
export class StompAuctionFeed implements AuctionFeed {
  readonly connection = signal<ConnectionState>('idle');
  readonly bids = signal<Bid[]>([]);
  readonly closed = signal<AuctionClosed | null>(null);

  private client: Client | null = null;

  watch(proposalId: string): void {
    this.stop();
    this.bids.set([]);
    this.closed.set(null);
    this.connection.set('connecting');

    const client = new Client({
      brokerURL: NOTIFICATION_GATEWAY_WS_URL,
      onConnect: () => {
        this.connection.set('connected');
        client.subscribe(`/topic/auctions/${proposalId}`, (message: IMessage) => {
          const notification: AuctionNotification = JSON.parse(message.body);
          if (notification.type === 'BID_PLACED') {
            this.bids.update((current) => [...current, notification.payload as Bid]);
          } else if (notification.type === 'AUCTION_CLOSED') {
            this.closed.set(notification.payload as AuctionClosed);
          }
        });
      },
      onWebSocketClose: () => this.connection.set('reconnecting'),
    });

    client.activate();
    this.client = client;
  }

  stop(): void {
    this.client?.deactivate();
    this.client = null;
    this.connection.set('idle');
  }
}

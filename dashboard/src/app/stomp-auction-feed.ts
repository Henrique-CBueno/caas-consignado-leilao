import { signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { AuctionClosed, AuctionFeed, Bid, ConnectionState } from './auction-feed';
import { Auth } from './auth';

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
// O tenant do tópico vem do claim custom:tenant_id do ID token (o servidor confere de novo no SUBSCRIBE).
function tenantIdOf(idToken: string): string {
  const payload = idToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
  return JSON.parse(atob(payload))['custom:tenant_id'];
}

export class StompAuctionFeed implements AuctionFeed {
  readonly connection = signal<ConnectionState>('idle');
  readonly bids = signal<Bid[]>([]);
  readonly closed = signal<AuctionClosed | null>(null);
  readonly watching = signal<string | null>(null);

  private client: Client | null = null;

  constructor(private readonly auth: Auth) {}

  watch(proposalId: string): void {
    this.stop();
    this.bids.set([]);
    this.closed.set(null);
    this.watching.set(proposalId);
    const idToken = this.auth.idToken();
    if (!idToken) {
      this.stop();
      return;
    }
    this.connection.set('connecting');

    const client = new Client({
      brokerURL: NOTIFICATION_GATEWAY_WS_URL,
      // O navegador não envia Authorization no handshake do WebSocket: o token vai no CONNECT do STOMP.
      connectHeaders: { Authorization: `Bearer ${idToken}` },
      onConnect: () => {
        this.connection.set('connected');
        client.subscribe(`/topic/tenants/${tenantIdOf(idToken)}/auctions/${proposalId}`, (message: IMessage) => {
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
    this.watching.set(null);
    this.connection.set('idle');
  }
}

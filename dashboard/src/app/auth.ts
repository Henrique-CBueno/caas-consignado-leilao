import { HttpClient } from '@angular/common/http';
import { Injectable, InjectionToken, Signal, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

// Sessão do tenant de demonstração logado (Milestone 16). Vive só na aba (sessionStorage): uma
// credencial de demo não deve sobreviver a fechar o navegador.
export interface Auth {
  readonly tenant: Signal<string | null>;
  readonly idToken: Signal<string | null>;
  readonly isAdmin: Signal<boolean>;
  login(tenant: string): Promise<void>;
  logout(): void;
}

export const AUTH = new InjectionToken<Auth>('Auth');

const STORAGE_KEY = 'caas.session';

// Dev local: api-gateway roda na porta 8080. Fora de localhost (NodePort do minikube): 30080.
const LOCAL_DEV_PORT = 8080;
const CLUSTER_NODE_PORT = 30080;
export const GATEWAY_BASE_URL = `http://${window.location.hostname}:${
  window.location.hostname === 'localhost' ? LOCAL_DEV_PORT : CLUSTER_NODE_PORT
}`;

@Injectable()
export class HttpAuth implements Auth {
  private readonly http = inject(HttpClient);

  readonly tenant = signal<string | null>(this.loadSession()?.tenant ?? null);
  readonly idToken = signal<string | null>(this.loadSession()?.idToken ?? null);
  // Administrador da plataforma (Milestone 17): a identidade demo "admin"; o gateway é quem impõe o papel.
  readonly isAdmin = computed(() => this.tenant() === 'admin');

  async login(tenant: string): Promise<void> {
    const response = await firstValueFrom(
      this.http.post<{ idToken: string }>(`${GATEWAY_BASE_URL}/auth/login`, { tenant }),
    );
    this.tenant.set(tenant);
    this.idToken.set(response.idToken);
    this.saveSession({ tenant, idToken: response.idToken });
  }

  logout(): void {
    this.tenant.set(null);
    this.idToken.set(null);
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // sem persistência; a sessão em memória já foi limpa
    }
  }

  private saveSession(session: { tenant: string; idToken: string }): void {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // sem persistência; a sessão continua valendo nesta aba
    }
  }

  private loadSession(): { tenant: string; idToken: string } | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}

import { Injectable, signal } from '@angular/core';

export interface KnownTenant {
  slug: string;
  name: string;
}

const STORAGE_KEY = 'caas.tenants';

// Mesma regra do backend (usuário demo `<slug>@caas.local`): sem acento, minúsculo, hífen entre palavras.
export function slugify(name: string): string {
  return name
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

// Tenants que o administrador criou neste navegador, para a tela de entrar oferecê-los: não existe
// listagem pública de tenants (vazaria os nomes dos bancos). O armazenamento pode estar bloqueado.
@Injectable({ providedIn: 'root' })
export class KnownTenants {
  readonly created = signal<KnownTenant[]>(this.load());

  add(tenant: KnownTenant): void {
    const next = [...this.created().filter((t) => t.slug !== tenant.slug), tenant];
    this.created.set(next);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    } catch {
      // sem persistência; a lista em memória continua valendo na aba
    }
  }

  private load(): KnownTenant[] {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
    } catch {
      return [];
    }
  }
}

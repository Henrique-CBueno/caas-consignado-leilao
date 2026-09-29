import { InjectionToken, Signal, signal } from '@angular/core';

// Relógio da tela (contagem regressiva do leilão): injetável para os testes controlarem o tempo.
export interface Clock {
  readonly now: Signal<number>;
}

export const CLOCK = new InjectionToken<Clock>('Clock');

// Adaptador real: o relógio local, atualizado a cada segundo (a diferença para o relógio do servidor é
// aceita nesta demonstração, ADR-0032).
export class IntervalClock implements Clock {
  private readonly current = signal(Date.now());
  readonly now = this.current.asReadonly();

  constructor() {
    setInterval(() => this.current.set(Date.now()), 1000);
  }
}

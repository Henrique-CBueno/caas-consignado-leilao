import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AUTH, GATEWAY_BASE_URL } from './auth';

interface CreateProposalResponse {
  id: string;
}

@Component({
  selector: 'app-new-proposal',
  imports: [FormsModule],
  template: `
    <section class="entry">
      <h1>Nova proposta</h1>

      <form (ngSubmit)="submit()">
        <label for="borrower-id">Tomador</label>
        <input id="borrower-id" type="text" name="borrowerId" [(ngModel)]="borrowerId" />

        <label for="requested-amount">Valor</label>
        <input id="requested-amount" type="number" name="requestedAmount" [(ngModel)]="requestedAmount" />

        <label for="term-months">Prazo (meses)</label>
        <input id="term-months" type="number" name="termMonths" [(ngModel)]="termMonths" />

        @if (error(); as message) {
          <p role="alert">{{ message }}</p>
        }

        <button class="primary" type="submit" [disabled]="submitting()">Criar proposta</button>
      </form>
    </section>
  `,
})
export class NewProposalView {
  private readonly auth = inject(AUTH);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected borrowerId = '';
  protected requestedAmount: number | null = null;
  protected termMonths: number | null = null;
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected submit(): void {
    if (!this.borrowerId || !this.requestedAmount || this.requestedAmount <= 0) {
      this.error.set('Informe o tomador e um valor maior que zero.');
      return;
    }
    if (!this.termMonths || this.termMonths <= 0) {
      this.error.set('Informe um prazo maior que zero.');
      return;
    }
    const idToken = this.auth.idToken();
    if (!idToken) {
      this.error.set('Entre com um tenant para criar uma proposta.');
      return;
    }

    this.error.set(null);
    this.submitting.set(true);
    this.http
      .post<CreateProposalResponse>(
        `${GATEWAY_BASE_URL}/proposals`,
        { borrowerId: this.borrowerId, requestedAmount: this.requestedAmount, termMonths: this.termMonths },
        { headers: { Authorization: `Bearer ${idToken}` } },
      )
      .subscribe({
        next: (response) => {
          this.submitting.set(false);
          this.router.navigate(['/'], { queryParams: { proposta: response.id } });
        },
        error: (err: HttpErrorResponse) => {
          this.submitting.set(false);
          this.error.set(err.error?.message ?? 'Não foi possível criar a proposta.');
        },
      });
  }
}

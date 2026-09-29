import { Routes } from '@angular/router';
import { AdminView } from './admin';
import { HistoryView } from './history';
import { LiveView } from './live';
import { LoginView } from './login';
import { NewProposalView } from './new-proposal';
import { ProposalsView } from './proposals';

export const routes: Routes = [
  { path: '', component: LiveView },
  { path: 'historico', component: HistoryView },
  { path: 'entrar', component: LoginView },
  { path: 'admin', component: AdminView },
  { path: 'propostas', component: ProposalsView },
  { path: 'nova-proposta', component: NewProposalView },
];

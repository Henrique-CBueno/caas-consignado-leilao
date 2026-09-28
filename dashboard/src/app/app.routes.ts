import { Routes } from '@angular/router';
import { HistoryView } from './history';
import { LiveView } from './live';
import { LoginView } from './login';
import { NewProposalView } from './new-proposal';

export const routes: Routes = [
  { path: '', component: LiveView },
  { path: 'historico', component: HistoryView },
  { path: 'entrar', component: LoginView },
  { path: 'nova-proposta', component: NewProposalView },
];

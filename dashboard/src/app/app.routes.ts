import { Routes } from '@angular/router';
import { HistoryView } from './history';
import { LiveView } from './live';

export const routes: Routes = [
  { path: '', component: LiveView },
  { path: 'historico', component: HistoryView },
];

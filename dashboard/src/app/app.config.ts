import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, inject, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withHashLocation } from '@angular/router';
import { AUCTION_FEED } from './auction-feed';
import { AUTH, HttpAuth } from './auth';
import { routes } from './app.routes';
import { CLOCK, IntervalClock } from './clock';
import { DemoAuctionFeed } from './demo-auction-feed';
import { StompAuctionFeed } from './stomp-auction-feed';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withHashLocation()),
    provideHttpClient(),
    { provide: AUTH, useClass: HttpAuth },
    { provide: CLOCK, useFactory: () => new IntervalClock() },
    {
      provide: AUCTION_FEED,
      useFactory: () =>
        new URLSearchParams(window.location.search).has('demo')
          ? new DemoAuctionFeed()
          : new StompAuctionFeed(inject(AUTH)),
    },
  ],
};

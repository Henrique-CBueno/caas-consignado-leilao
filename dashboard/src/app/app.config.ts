import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withHashLocation } from '@angular/router';
import { AUCTION_FEED } from './auction-feed';
import { routes } from './app.routes';
import { DemoAuctionFeed } from './demo-auction-feed';
import { StompAuctionFeed } from './stomp-auction-feed';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withHashLocation()),
    {
      provide: AUCTION_FEED,
      useFactory: () =>
        new URLSearchParams(window.location.search).has('demo')
          ? new DemoAuctionFeed()
          : new StompAuctionFeed(),
    },
  ],
};

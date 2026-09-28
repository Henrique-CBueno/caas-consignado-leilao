import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { AUCTION_FEED } from './auction-feed';
import { StompAuctionFeed } from './stomp-auction-feed';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: AUCTION_FEED, useClass: StompAuctionFeed },
  ],
};

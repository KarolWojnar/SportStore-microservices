import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor, tokenInterceptor } from './service/auth-interceptor';
import { GoogleLoginProvider, SocialAuthServiceConfig } from '@abacritt/angularx-social-login';
import { environment } from '../enviroments/env';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor, tokenInterceptor])
    ),
    {
      provide: 'SocialAuthServiceConfig',
      useValue: {
        autoLogin: true,
        lang: 'en-US',
        providers: [
          {
            id: GoogleLoginProvider.PROVIDER_ID,
            provider: new GoogleLoginProvider(
              environment.googleClientId,
              {
                oneTapEnabled: true
              }
            )
          }
        ]
      } as SocialAuthServiceConfig
    }
  ],
};

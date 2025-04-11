import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CustomerDetails, UserDetails, UserDto, UserLoginStatus } from '../model/user-dto';
import { catchError, Observable, of, tap } from 'rxjs';
import { AuthStateService } from './auth-state.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private httpClient: HttpClient,
              private authState: AuthStateService) {}

  registerUser(user: UserDto) {
    return this.httpClient.post(`${this.apiUrl}/auth`, user);
  }

  googleLogin(idToken: string): Observable<any> {
    return this.httpClient.post(`${this.apiUrl}/auth/google`, {idToken} ).pipe(
      tap((response: any) => {
        if (response && response.token) {
          localStorage.setItem('googleToken', idToken);
          localStorage.setItem('token', response.token);
          this.authState.setLoggedIn(true);
          localStorage.setItem('cartHasItems', response.cartHasItems ? 'true' : 'false');
          this.authState.setCartHasItems(response.cartHasItems);
        }
      })
    );
  }

  updateCustomerInfo(user: CustomerDetails) {
    return this.httpClient.put<{user: UserDetails}>(`${this.apiUrl}/auth`, user);
  }

  login(user: UserDto): Observable<any> {
    return this.httpClient.post(`${this.apiUrl}/auth/login`, user).pipe(
      tap((response: any) => {
        if (response && response.token) {
          localStorage.setItem('token', response.token);
          this.authState.setLoggedIn(true);
          localStorage.setItem('cartHasItems', response.cartHasItems ? 'true' : 'false');
          this.authState.setCartHasItems(response.cartHasItems);
        }
      })
    );
  }

  logout(): Observable<any> {
    return this.httpClient.post(`${this.apiUrl}/auth/logout`, {}).pipe(
      tap(() => {
        this.authState.setLoggedIn(false);
        localStorage.removeItem('token');
        localStorage.removeItem('googleToken');
        localStorage.removeItem('isAdmin');
        localStorage.removeItem('cartHasItems');
      })
    );
  }

  activateAccount(activationCode: string) {
    return this.httpClient.get(`${this.apiUrl}/auth/activate/${activationCode}`);
  }

  isLoggedIn(): Observable<any> {
    return this.httpClient.get<UserLoginStatus>(`${this.apiUrl}/auth/isLoggedIn`).pipe(
      catchError(() => {
        this.authState.setLoggedIn(false);
        this.authState.setAdmin(false);
        localStorage.removeItem('token');
        localStorage.removeItem('googleToken');
        localStorage.removeItem('isAdmin');
        return of({ loggedIn: false });
      })
    );
  }

  getRole(): Observable<any> {
    return this.httpClient.get<any>(`${this.apiUrl}/auth/role`).pipe(
      tap((response: any) => {
        if (response && response.role) {
          this.authState.setAdmin(response.role === 'ROLE_ADMIN');
        }
      })
    );
  }

  recoveryPassword(email: string): Observable<any> {
    return this.httpClient.post<{response: any}>(`${this.apiUrl}/auth/recovery-password`, email);
  }

  checkResetCode(resetCode: string) {
    return this.httpClient.get<boolean>(`${this.apiUrl}/auth/check-reset-code/${resetCode}`);
  }

  resetPassword(password: string, confirmPassword: string, code: string) {
    return this.httpClient.post<{response: any}>(`${this.apiUrl}/auth/reset-password`, {password, confirmPassword, code});
  }

  getUserInfo(): Observable<UserDetails> {
    return this.httpClient.get<UserDetails>(`${this.apiUrl}/users`);
  }
}

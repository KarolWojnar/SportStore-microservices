import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIf } from '@angular/common';
import { AuthService } from '../../service/auth.service';
import { Router, RouterLink } from '@angular/router';
import { UserLoginDto } from '../../model/user-dto';
import {
  SocialAuthService,
  SocialUser,
  SocialLoginModule,
  GoogleSigninButtonModule
} from '@abacritt/angularx-social-login';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    NgIf,
    RouterLink,
    SocialLoginModule,
    GoogleSigninButtonModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  standalone: true
})
export class LoginComponent implements OnInit, OnDestroy{
  loginForm: FormGroup;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  email: string | null = null;
  private authStateSubscription!: Subscription;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private socialAuthService: SocialAuthService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  ngOnDestroy(): void {
    if (this.authStateSubscription) {
      this.authStateSubscription.unsubscribe();
    }
    }

  ngOnInit(): void {
    const navigationState = this.router.getCurrentNavigation()?.extras.state;
    if (navigationState && navigationState['email']) {
      this.email = navigationState['email'];
      this.loginForm.patchValue({email: this.email});
    }

    const googleToken = localStorage.getItem('googleToken');
    if (googleToken) {
      this.authService.isLoggedIn().subscribe(status => {
        if (status.loggedIn) {
          this.router.navigate(['/']);
        }
      });
    }

    this.authStateSubscription = this.socialAuthService.authState.subscribe((user: SocialUser) => {
      if (user) {
        this.handleGoogleLogin(user);
      }
    });
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      return;
    }

    const user: UserLoginDto = this.loginForm.value;
    this.authService.login(user).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.errorMessage = err.error.message || 'Login failed. Please try again.';
      },
    });
  }

  private handleGoogleLogin(user: SocialUser): void {
    this.authService.googleLogin(user.idToken).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.errorMessage = err.error.message || 'Google login failed. Please try again.';
      }
    });
  }
}

import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { AuthService } from '@app/core/services/auth.service';
import { LoadingSpinnerComponent } from '@app/shared/components/loading-spinner/loading-spinner.component';
import { AppIconComponent } from '@app/shared/components/app-icon/app-icon.component';
import { SnackbarService } from '@app/shared/services/snackbar.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    LoadingSpinnerComponent,
    AppIconComponent,
  ],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly snackbar = inject(SnackbarService);

  registerForm!: FormGroup;
  submitting = false;
  hidePassword = true;
  hideConfirmPassword = true;

  ngOnInit(): void {
    this.registerForm = this.fb.group(
      {
        firstName: ['', [Validators.required, Validators.maxLength(50)]],
        lastName: ['', [Validators.required, Validators.maxLength(50)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: this.passwordsMatchValidator }
    );
  }

  private passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    if (password && confirmPassword && password !== confirmPassword) {
      group.get('confirmPassword')?.setErrors({ passwordsMismatch: true });
      return { passwordsMismatch: true };
    }
    return null;
  }

  get f() {
    return this.registerForm.controls;
  }

  onSubmit(): void {
    if (this.registerForm.invalid) return;

    this.submitting = true;
    const { firstName, lastName, email, password } = this.registerForm.value;

    this.authService
      .register({ firstName, lastName, email, password })
      .subscribe({
        next: () => {
          this.submitting = false;
          this.router.navigateByUrl('/');
        },
        error: (err) => {
          this.submitting = false;
          const message =
            err?.error?.message ??
            err?.message ??
            'Registration failed. Please try again.';
          this.snackbar.open(message, 'Close', { duration: 5000, type: 'error' });
        },
      });
  }
}

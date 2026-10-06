import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home {
  private readonly router = inject(Router);

  protected readonly sampleIds = ['CUS-1001', 'CUS-1002'];
  protected readonly form = inject(NonNullableFormBuilder).group({
    customerId: ['', [Validators.required, Validators.pattern(/\S/)]],
  });

  protected showError(): boolean {
    const control = this.form.controls.customerId;
    return control.invalid && control.touched;
  }

  protected lookUp(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.form.controls.customerId.value.trim();
    void this.router.navigate(['/customers', id]);
  }
}

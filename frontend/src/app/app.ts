import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Auth } from './core/auth/auth';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly appName = 'Northstar CRM';
  protected readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
}

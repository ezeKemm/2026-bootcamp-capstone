import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Home } from './home';

function setup() {
  const fixture = TestBed.createComponent(Home);
  fixture.detectChanges();

  return {
    fixture,
    element: fixture.nativeElement as HTMLElement,
  };
}

describe('Home', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('navigates to login with the customer return URL', () => {
    const { fixture, element } = setup();

    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const raviButton = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.name-btn'),
    ).find((button) => button.textContent?.trim() === 'Ravi Singh');

    raviButton?.click();
    fixture.detectChanges();

    expect(navigateSpy).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/customers/CUS-1002' },
    });
  });
});
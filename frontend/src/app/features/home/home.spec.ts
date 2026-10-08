import { TestBed } from '@angular/core/testing';
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
    }).compileComponents();
  });

  it('renders the customer list', () => {
    const { element } = setup();

    expect(element.querySelector('h1')?.textContent).toContain('Customers');
    expect(element.querySelectorAll('tbody tr')).toHaveLength(2);
    expect(element.querySelector('tbody')?.textContent).toContain('Amina Khan');
    expect(element.querySelector('tbody')?.textContent).toContain('Ravi Singh');
  });

  it('filters the list by status', () => {
    const { fixture, element } = setup();

    element.querySelector<HTMLButtonElement>('.filter-btn')?.click();
    fixture.detectChanges();

    const options = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.menu-item'),
    );
    expect(options.map((option) => option.textContent?.trim())).toEqual([
      'All',
      'Active',
      'Prospect',
    ]);

    const prospectOption = options.find(
      (option) => option.textContent?.trim() === 'Prospect',
    );
    prospectOption?.click();
    fixture.detectChanges();

    expect(element.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(element.querySelector('tbody')?.textContent).toContain('Ravi Singh');
    expect(element.querySelector('tbody')?.textContent).not.toContain('Amina Khan');
    expect(element.querySelector('.count')?.textContent).toContain('Showing 1 of 2');
    expect(element.querySelector('.menu')).toBeNull();
  });

  it('highlights a customer row when its name is selected', () => {
    const { fixture, element } = setup();

    const raviButton = Array.from(
      element.querySelectorAll<HTMLButtonElement>('.name-btn'),
    ).find((button) => button.textContent?.trim() === 'Ravi Singh');

    expect(raviButton).toBeDefined();
    raviButton?.click();
    fixture.detectChanges();

    const selectedRow = element.querySelector('tbody tr.selected');
    expect(selectedRow?.textContent).toContain('Ravi Singh');
  });
});
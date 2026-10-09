import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { Interaction } from '../interaction.model';
import { RecordInteractionForm } from './record-interaction-form';

const CUSTOMER_ID = '5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11';
const URL = `${environment.apiBaseUrl}/api/v1/customers/${CUSTOMER_ID}/interactions`;

/** What the backend returns after saving. */
const SAVED: Interaction = {
  interactionId: '6f1c2b7e-8d3a-4c51-9b2e-0a7d4e9f1c23',
  customerId: CUSTOMER_ID,
  channel: 'PHONE',
  summary: 'Called about renewal',
  actor: 'agent1',
  occurredAt: '2026-10-09T15:00:00Z',
  correlationId: 'test-correlation-id',
};

function setup() {
  const fixture = TestBed.createComponent(RecordInteractionForm);
  fixture.componentRef.setInput('customerId', CUSTOMER_ID);
  const emitted: Interaction[] = [];
  fixture.componentInstance.recorded.subscribe((i) => emitted.push(i));
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;
  const http = TestBed.inject(HttpTestingController);

  const fill = (channel: string, summary: string) => {
    const select = el.querySelector('select')!;
    select.value = channel;
    select.dispatchEvent(new Event('change'));
    const textarea = el.querySelector('textarea')!;
    textarea.value = summary;
    textarea.dispatchEvent(new Event('input'));
  };
  const submit = () => {
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  return { fixture, el, http, emitted, fill, submit };
}

describe('RecordInteractionForm', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RecordInteractionForm],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('shows errors and sends nothing when empty', () => {
    const { el, emitted, submit } = setup();
    submit();
    expect(el.querySelectorAll('.error').length).toBe(2);
    expect(emitted.length).toBe(0);
  });

  it('posts the trimmed interaction and emits what the server saved', () => {
    const { fixture, el, http, emitted, fill, submit } = setup();
    fill('PHONE', '  Called about renewal  ');
    submit();

    const req = http.expectOne(URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ channel: 'PHONE', summary: 'Called about renewal' });
    expect(el.querySelector('button[type="submit"]')?.textContent).toContain('Recording');

    req.flush(SAVED, { status: 201, statusText: 'Created' });
    fixture.detectChanges();

    expect(emitted).toEqual([SAVED]);
    expect(el.querySelector('[role="status"]')?.textContent).toContain('PHONE');
  });

  it('shows the server error and emits nothing when saving fails', () => {
    const { fixture, el, http, emitted, fill, submit } = setup();
    fill('PHONE', 'Called about renewal');
    submit();

    http.expectOne(URL).flush(
      { title: 'Business rule violated', status: 422, detail: 'Customer is not active.' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    fixture.detectChanges();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Customer is not active.');
    expect(emitted.length).toBe(0);
  });

  it('rejects a summary that is only spaces', () => {
    const { emitted, fill, submit } = setup();
    fill('EMAIL', '   ');
    submit();
    expect(emitted.length).toBe(0);
  });
});

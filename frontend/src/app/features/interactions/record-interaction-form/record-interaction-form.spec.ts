import { TestBed } from '@angular/core/testing';
import { RecordInteractionForm } from './record-interaction-form';
import { NewInteraction } from '../interaction.model';

function setup() {
  const fixture = TestBed.createComponent(RecordInteractionForm);
  fixture.componentRef.setInput('customerId', 'CUS-1001');
  const emitted: NewInteraction[] = [];
  fixture.componentInstance.recorded.subscribe((i) => emitted.push(i));
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;

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
  return { el, emitted, fill, submit };
}

describe('RecordInteractionForm', () => {
  it('shows errors and does not emit when empty', () => {
    const { el, emitted, submit } = setup();
    submit();
    expect(el.querySelectorAll('.error').length).toBe(2);
    expect(emitted.length).toBe(0);
  });

  it('emits the interaction when valid', () => {
    const { el, emitted, fill, submit } = setup();
    fill('PHONE', '  Called about renewal  ');
    submit();
    expect(emitted.length).toBe(1);
    expect(emitted[0]).toEqual({
      customerId: 'CUS-1001',
      channel: 'PHONE',
      summary: 'Called about renewal',
    });
    expect(el.querySelector('[role="status"]')?.textContent).toContain('CUS-1001');
  });

  it('rejects a summary that is only spaces', () => {
    const { emitted, fill, submit } = setup();
    fill('EMAIL', '   ');
    submit();
    expect(emitted.length).toBe(0);
  });
});

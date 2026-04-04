import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ModeToggle } from './ModeToggle';
import { ModeProvider } from '../lib/mode/ModeContext';

describe('ModeToggle', () => {
  it('switches between invest and quant mode', () => {
    render(
      <ModeProvider>
        <ModeToggle />
      </ModeProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: '퀀트 모드' }));

    expect(screen.getByRole('button', { name: '퀀트 모드' }).className).toContain('is-active');
    expect(localStorage.getItem('qerp.app-mode')).toBe('quant');
  });
});

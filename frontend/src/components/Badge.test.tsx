import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Badge } from './Badge';

describe('Badge', () => {
  it('label을 렌더링한다', () => {
    render(<Badge label="LIVE" tone="live" />);
    expect(screen.getByText('LIVE')).toBeInTheDocument();
  });
});

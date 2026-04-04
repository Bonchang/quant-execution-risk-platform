export function formatMoney(value: number | null | undefined, digits = 0) {
  return Number(value ?? 0).toLocaleString('ko-KR', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  });
}

export function formatSignedMoney(value: number | null | undefined, digits = 0) {
  const numeric = Number(value ?? 0);
  return `${numeric > 0 ? '+' : ''}${formatMoney(numeric, digits)}`;
}

export function formatPercent(value: number | null | undefined, multiplier = 1) {
  return `${(Number(value ?? 0) * multiplier).toFixed(2)}%`;
}

export function formatDateTime(value: string | null | undefined) {
  return value ? value.replace('T', ' ') : '-';
}

export function metricValue(value: unknown) {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value.toFixed(Math.abs(value) < 10 ? 3 : 2) : String(value);
  }
  return String(value ?? '-');
}

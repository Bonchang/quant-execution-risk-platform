function money(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function HoldingRow({
  symbol,
  strategyName,
  netQuantity,
  marketValue,
  unrealizedPnl,
  lastPrice,
}: {
  symbol: string;
  strategyName: string;
  netQuantity: number;
  marketValue: number;
  unrealizedPnl: number;
  lastPrice: number;
}) {
  const positive = unrealizedPnl >= 0;

  return (
    <article className="holding-row">
      <div>
        <strong>{symbol}</strong>
        <p>{strategyName}</p>
      </div>
      <div>
        <span>{netQuantity.toFixed(2)}주</span>
        <p>현재가 {money(lastPrice)}</p>
      </div>
      <div>
        <strong>{money(marketValue)}</strong>
        <p className={positive ? 'text-up' : 'text-down'}>
          {positive ? '+' : ''}
          {money(unrealizedPnl)}
        </p>
      </div>
    </article>
  );
}

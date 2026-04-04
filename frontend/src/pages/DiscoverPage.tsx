import { useQuery } from '@tanstack/react-query';
import { useDeferredValue, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { apiClient } from '../lib/api/client';
import { formatDateTime, formatMoney, formatPercent } from '../lib/format';

export function DiscoverPage() {
  const [keyword, setKeyword] = useState('');
  const deferredKeyword = useDeferredValue(keyword);
  const discoverQuery = useQuery({
    queryKey: ['discover'],
    queryFn: () => apiClient.getDiscover(),
    refetchInterval: 5000,
  });

  if (discoverQuery.isLoading) {
    return <div className="page-grid"><section className="app-panel">실시간 시세를 불러오는 중입니다.</section></div>;
  }

  if (discoverQuery.isError || !discoverQuery.data) {
    return <div className="page-grid"><section className="app-panel">종목 데이터를 불러오지 못했습니다.</section></div>;
  }

  const filteredStocks = discoverQuery.data.stocks.filter((stock) => {
    const needle = deferredKeyword.trim().toLowerCase();
    return !needle || stock.symbol.toLowerCase().includes(needle) || stock.name.toLowerCase().includes(needle);
  });

  return (
    <div className="app-stack">
      <section className="app-panel">
        <div className="panel-header">
          <div>
            <h1>종목 탐색</h1>
            <p>실시간 quote, 시장 상태, 검색을 한 화면에서 확인합니다.</p>
          </div>
          <Badge
            label={discoverQuery.data.marketSummary.marketStatus}
            tone={discoverQuery.data.marketSummary.staleQuoteCount > 0 ? 'stale' : 'live'}
          />
        </div>
        <div className="market-banner">
          <div>
            <span>last quote</span>
            <strong>{formatDateTime(discoverQuery.data.marketSummary.lastQuoteReceivedAt)}</strong>
          </div>
          <div>
            <span>top mover</span>
            <strong>{discoverQuery.data.marketSummary.topMoverSymbol ?? '-'}</strong>
          </div>
          <div>
            <span>change</span>
            <strong>{formatPercent(discoverQuery.data.marketSummary.topMoverChangePercent ?? 0)}</strong>
          </div>
          <div>
            <span>source</span>
            <strong>{discoverQuery.data.marketSummary.source}</strong>
          </div>
        </div>
        <input
          className="search-input"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="종목명 또는 심볼 검색"
        />
      </section>

      <div className="stock-card-grid">
        {filteredStocks.map((stock) => {
          const positive = stock.changePercent >= 0;
          return (
            <Link key={stock.symbol} className="stock-card" to={`/stocks/${stock.symbol}`}>
              <div className="stock-card__header">
                <div>
                  <strong>{stock.symbol}</strong>
                  <p>{stock.name}</p>
                </div>
                <Badge label={stock.stale ? 'STALE' : 'LIVE'} tone={stock.stale ? 'stale' : 'live'} />
              </div>
              <div className="stock-card__body">
                <strong className="stock-card__price">{formatMoney(stock.lastPrice, 2)}</strong>
                <span className={positive ? 'text-up' : 'text-down'}>
                  {positive ? '+' : ''}
                  {formatPercent(stock.changePercent)}
                </span>
              </div>
              <div className="stock-card__footer">
                <span>bid {formatMoney(stock.bidPrice, 2)}</span>
                <span>ask {formatMoney(stock.askPrice, 2)}</span>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}

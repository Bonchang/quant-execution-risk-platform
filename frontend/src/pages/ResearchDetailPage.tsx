import { useQuery } from '@tanstack/react-query';
import Plot from 'react-plotly.js';
import { useParams } from 'react-router-dom';
import { SectionCard } from '../components/SectionCard';
import { DataTable } from '../components/DataTable';
import { apiClient } from '../lib/api/client';

function toNumber(value: unknown) {
  return typeof value === 'number' ? value : Number(value ?? 0);
}

export function ResearchDetailPage() {
  const { runId = '' } = useParams();
  const query = useQuery({
    queryKey: ['research-run', runId],
    queryFn: () => apiClient.getResearchRun(runId),
    enabled: Boolean(runId),
  });

  const detail = query.data;
  const curve = detail?.equityCurveRows ?? [];

  return (
    <div className="page-grid">
      <section className="hero-panel">
        <p>Research Run</p>
        <h1>{detail?.strategyName ?? runId}</h1>
        <p>{detail?.instrumentSymbol ?? '-'} · generated at {detail?.generatedAt ?? '-'}</p>
      </section>

      <div className="two-col">
        <SectionCard title="Strategy 설명">
          <ul className="info-list">
            <li><strong>Signal</strong><span>moving average crossover</span></li>
            <li><strong>Sizing</strong><span>volatility targeting</span></li>
            <li><strong>Costs</strong><span>transaction cost + slippage 포함</span></li>
          </ul>
        </SectionCard>
        <SectionCard title="Config">
          <pre>{JSON.stringify(detail?.config ?? {}, null, 2)}</pre>
        </SectionCard>
      </div>

      <SectionCard title="성과 요약">
        <div className="kpi-grid">
          {Object.entries(detail?.metrics ?? {}).map(([key, value]) => (
            <div className="kpi-card" key={key}>
              <span>{key}</span>
              <strong>{typeof value === 'number' ? value.toFixed(3) : String(value)}</strong>
            </div>
          ))}
        </div>
      </SectionCard>

      <SectionCard title="Equity Curve" subtitle="artifact csv를 API가 읽어서 차트 데이터로 제공합니다.">
        <div className="chart-panel">
          <Plot
            data={[
              {
                x: curve.map((row) => String(row.price_date ?? '')),
                y: curve.map((row) => toNumber(row.equity_curve)),
                type: 'scatter',
                mode: 'lines+markers',
                line: { color: '#0f4c81', width: 3 },
              },
            ]}
            layout={{
              autosize: true,
              margin: { l: 48, r: 20, t: 20, b: 40 },
              paper_bgcolor: 'transparent',
              plot_bgcolor: 'transparent',
            }}
            style={{ width: '100%', height: '100%' }}
            useResizeHandler
            config={{ displayModeBar: false }}
          />
        </div>
      </SectionCard>

      <div className="two-col">
        <SectionCard title="Trades">
          <DataTable
            rows={detail?.tradeRows ?? []}
            columns={[
              { header: 'Date', render: (row) => String(row.price_date ?? '-') },
              { header: 'Trade', render: (row) => String(row.trade ?? '-') },
              { header: 'Position', render: (row) => String(row.position ?? '-') },
              { header: 'Close', render: (row) => String(row.close_price ?? '-') },
              { header: 'Cost', render: (row) => String(row.cost ?? '-') },
            ]}
          />
        </SectionCard>
        <SectionCard title="Signals">
          <DataTable
            rows={detail?.signalRows ?? []}
            columns={detail?.signalRows?.length
              ? Object.keys(detail.signalRows[0]).map((key) => ({
                  header: key,
                  render: (row: Record<string, unknown>) => String(row[key] ?? '-'),
                }))
              : [{ header: 'Signal', render: () => '-' }]}
          />
        </SectionCard>
      </div>
    </div>
  );
}

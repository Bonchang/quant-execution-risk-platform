import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { SectionCard } from '../components/SectionCard';
import { useAuth } from '../lib/auth/AuthContext';
import { apiClient } from '../lib/api/client';

export function ResearchLinkPage() {
  const { token, logout } = useAuth();
  const overviewQuery = useQuery({
    queryKey: ['console-overview', token],
    queryFn: () => apiClient.getOverview(token, logout),
    enabled: Boolean(token),
  });
  const researchQuery = useQuery({
    queryKey: ['research-runs'],
    queryFn: () => apiClient.listResearchRuns(),
  });

  const latestResearch = researchQuery.data?.[0];
  const overview = overviewQuery.data;

  return (
    <div className="page-grid">
      <section className="hero-panel">
        <p>Research Link</p>
        <h1>리서치 결과와 운영 흐름을 연결하는 브리지</h1>
        <p>최신 리서치 run이 어떤 종목과 전략으로 운영 콘솔 상태와 이어지는지 설명합니다.</p>
      </section>

      <div className="two-col">
        <SectionCard title="Latest Research">
          <ul className="info-list">
            <li><strong>Run</strong><span>{latestResearch?.runId ?? '-'}</span></li>
            <li><strong>Strategy</strong><span>{latestResearch?.strategyName ?? '-'}</span></li>
            <li><strong>Instrument</strong><span>{latestResearch?.instrumentSymbol ?? '-'}</span></li>
          </ul>
          {latestResearch ? (
            <div className="inline-actions">
              <Link className="ghost-button" to={`/research/${latestResearch.runId}`}>
                리서치 상세
              </Link>
            </div>
          ) : null}
        </SectionCard>

        <SectionCard title="Operational State">
          <ul className="info-list">
            <li><strong>Latest Strategy</strong><span>{overview?.portfolioSummary.strategyName ?? '-'}</span></li>
            <li><strong>Latest Account</strong><span>{overview?.portfolioSummary.accountCode ?? '-'}</span></li>
            <li><strong>Total PnL</strong><span>{overview?.portfolioSummary.totalPnl ?? '-'}</span></li>
          </ul>
          <div className="inline-actions">
            <Link className="ghost-button" to="/console">
              운영 콘솔 열기
            </Link>
          </div>
        </SectionCard>
      </div>
    </div>
  );
}

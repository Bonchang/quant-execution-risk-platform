import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { DataTable } from '../components/DataTable';
import { SectionCard } from '../components/SectionCard';
import { apiClient } from '../lib/api/client';

function metric(value: unknown) {
  if (typeof value === 'number') {
    return value.toFixed(3);
  }
  return String(value ?? '-');
}

export function ResearchListPage() {
  const runsQuery = useQuery({
    queryKey: ['research-runs'],
    queryFn: () => apiClient.listResearchRuns(),
  });

  const runs = runsQuery.data ?? [];

  return (
    <div className="page-grid">
      <section className="hero-panel">
        <p>Research</p>
        <h1>전략 결과와 artifact를 읽는 리서치 뷰</h1>
        <p>최신 run의 전략 정의, 성과지표, artifact 생성 상태를 한 번에 확인합니다.</p>
      </section>

      <SectionCard title="Latest Runs" subtitle="public page로 읽을 수 있는 research summary 목록입니다.">
        <DataTable
          rows={runs}
          emptyMessage={runsQuery.isLoading ? '로딩 중...' : '리서치 결과 없음'}
          columns={[
            {
              header: 'Run',
              render: (row) => (
                <Link to={`/research/${row.runId}`}>
                  <strong>{row.runId}</strong>
                </Link>
              ),
            },
            { header: 'Strategy', render: (row) => row.strategyName },
            { header: 'Instrument', render: (row) => row.instrumentSymbol },
            { header: 'Generated', render: (row) => row.generatedAt },
            {
              header: 'Metrics',
              render: (row) => Object.entries(row.metrics).map(([key, value]) => `${key}: ${metric(value)}`).join(' · '),
            },
            {
              header: 'Artifacts',
              render: (row) => Object.entries(row.artifactAvailability).map(([key, available]) => (
                <div key={key}>
                  <Badge label={key} tone={available ? 'success' : 'warning'} />
                </div>
              )),
            },
          ]}
        />
      </SectionCard>
    </div>
  );
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { InlineAuthPanel } from '../components/InlineAuthPanel';
import { SectionCard } from '../components/SectionCard';
import { useAuth } from '../lib/auth/AuthContext';
import { hasRequiredRole } from '../lib/auth/token';
import { apiClient } from '../lib/api/client';
import { formatDateTime } from '../lib/format';

export function ProfilePage() {
  const queryClient = useQueryClient();
  const { token, role, logout } = useAuth();
  const isAdmin = hasRequiredRole(role, ['ROLE_ADMIN']);

  const healthQuery = useQuery({
    queryKey: ['market-health', token],
    queryFn: () => apiClient.getMarketHealth(token, logout),
    enabled: Boolean(token),
    refetchInterval: token ? 5000 : false,
  });

  const seedMutation = useMutation({
    mutationFn: () => apiClient.seedDemo(token, logout),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['market-health'] });
      await queryClient.invalidateQueries({ queryKey: ['app-home'] });
    },
  });

  const ingestMutation = useMutation({
    mutationFn: () => apiClient.ingestMarketData(token, logout),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['market-health'] });
      await queryClient.invalidateQueries({ queryKey: ['discover'] });
    },
  });

  const snapshotMutation = useMutation({
    mutationFn: () => apiClient.refreshSnapshots(token, logout),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['portfolio'] });
      await queryClient.invalidateQueries({ queryKey: ['app-home'] });
    },
  });

  if (!token) {
    return <InlineAuthPanel title="프로필" subtitle="로그인하면 역할과 데모 운영 액션을 확인할 수 있습니다." />;
  }

  return (
    <div className="app-stack">
      <SectionCard title="계정 상태" subtitle="현재 로그인 세션">
        <div className="inline-actions">
          <Badge label={role} tone="live" />
          <button className="ghost-button" type="button" onClick={logout}>로그아웃</button>
        </div>
        <ul className="info-list">
          <li><strong>권한</strong><span>{role}</span></li>
          <li><strong>시장 연결</strong><span>{healthQuery.data?.status ?? '-'}</span></li>
          <li><strong>마지막 quote</strong><span>{formatDateTime(healthQuery.data?.lastQuoteReceivedAt ?? null)}</span></li>
        </ul>
      </SectionCard>

      <SectionCard title="데모 액션" subtitle="ADMIN 권한에서만 seed/ingest/snapshot 실행">
        {isAdmin ? (
          <div className="inline-actions">
            <button className="primary-button" type="button" onClick={() => seedMutation.mutate()}>데모 데이터 생성</button>
            <button className="ghost-button" type="button" onClick={() => ingestMutation.mutate()}>시세 수집</button>
            <button className="ghost-button" type="button" onClick={() => snapshotMutation.mutate()}>자산 스냅샷 갱신</button>
          </div>
        ) : (
          <p className="muted">ADMIN 계정에서만 운영 액션을 실행할 수 있습니다.</p>
        )}
      </SectionCard>

      <SectionCard title="보조 화면" subtitle="내부 운영과 채용용 설명 화면은 별도로 남겨둡니다.">
        <div className="panel-actions">
          <Link className="ghost-button" to="/console">운영 콘솔</Link>
          <Link className="ghost-button" to="/architecture">아키텍처</Link>
        </div>
      </SectionCard>
    </div>
  );
}

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
  const { token, role, login, logout } = useAuth();
  const isAdmin = hasRequiredRole(role, ['ROLE_ADMIN']);
  const isGuest = hasRequiredRole(role, ['ROLE_GUEST']);

  const meQuery = useQuery({
    queryKey: ['app-me', token],
    queryFn: () => apiClient.getMe(token, logout),
    enabled: Boolean(token),
    refetchInterval: token ? 5000 : false,
  });

  const guestMutation = useMutation({
    mutationFn: () => apiClient.startGuestSession(),
    onSuccess: async (payload) => {
      queryClient.clear();
      login(payload);
    },
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
    return <InlineAuthPanel title="프로필" subtitle="게스트 세션을 시작하면 내 paper account와 시장 연결 상태를 확인할 수 있습니다." />;
  }

  return (
    <div className="app-stack">
      <SectionCard title="계정 상태" subtitle="현재 공개 데모 세션">
        <div className="inline-actions">
          <Badge label={role} tone={isGuest ? 'live' : 'neutral'} />
          <button className="ghost-button" type="button" onClick={logout}>로그아웃</button>
          <button className="ghost-button" type="button" onClick={() => guestMutation.mutate()}>
            새 게스트 세션
          </button>
        </div>
        <ul className="info-list">
          <li><strong>표시 이름</strong><span>{meQuery.data?.displayName ?? '-'}</span></li>
          <li><strong>세션 타입</strong><span>{meQuery.data?.authType ?? '-'}</span></li>
          <li><strong>계좌 코드</strong><span>{meQuery.data?.account.accountCode ?? '-'}</span></li>
          <li><strong>시장 연결</strong><span>{meQuery.data?.marketConnection.status ?? '-'}</span></li>
          <li><strong>마지막 quote</strong><span>{formatDateTime(meQuery.data?.marketConnection.lastQuoteReceivedAt ?? null)}</span></li>
        </ul>
      </SectionCard>

      <SectionCard title="내 paper account" subtitle="게스트 세션마다 전용 가상 자산이 분리됩니다.">
        <ul className="info-list">
          <li><strong>보유 계좌</strong><span>{meQuery.data?.account.ownerName ?? '-'}</span></li>
          <li><strong>기준 통화</strong><span>{meQuery.data?.account.baseCurrency ?? '-'}</span></li>
          <li><strong>사용 가능 현금</strong><span>{Number(meQuery.data?.account.availableCash ?? 0).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span></li>
          <li><strong>예약 현금</strong><span>{Number(meQuery.data?.account.reservedCash ?? 0).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span></li>
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

      <SectionCard title="보조 화면" subtitle="퀀트 설명 화면만 공개 경험에 남깁니다.">
        <div className="panel-actions">
          <Link className="ghost-button" to="/architecture">아키텍처</Link>
        </div>
      </SectionCard>
    </div>
  );
}

import { useMutation } from '@tanstack/react-query';
import { apiClient } from '../lib/api/client';
import { useAuth } from '../lib/auth/AuthContext';
import { SectionCard } from './SectionCard';

export function InlineAuthPanel({ title, subtitle }: { title: string; subtitle: string }) {
  const { login } = useAuth();

  const guestMutation = useMutation({
    mutationFn: () => apiClient.startGuestSession(),
    onSuccess: (payload) => {
      login(payload);
    },
  });

  return (
    <SectionCard title={title} subtitle={subtitle}>
      <div className="app-stack">
        <p className="muted">게스트 세션을 시작하면 전용 paper account와 기본 전략 컨텍스트가 자동으로 생성됩니다.</p>
        <button className="primary-button" type="button" disabled={guestMutation.isPending} onClick={() => guestMutation.mutate()}>
          {guestMutation.isPending ? '게스트 세션 생성 중...' : '게스트로 시작'}
        </button>
        {guestMutation.isError ? <p className="muted">{(guestMutation.error as Error).message}</p> : null}
      </div>
    </SectionCard>
  );
}

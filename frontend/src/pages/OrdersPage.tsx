import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { InlineAuthPanel } from '../components/InlineAuthPanel';
import { SectionCard } from '../components/SectionCard';
import { useAuth } from '../lib/auth/AuthContext';
import { hasRequiredRole } from '../lib/auth/token';
import { apiClient } from '../lib/api/client';
import { formatDateTime, formatMoney } from '../lib/format';

export function OrdersPage() {
  const queryClient = useQueryClient();
  const { token, role, logout } = useAuth();
  const canTrade = hasRequiredRole(role, ['ROLE_ADMIN', 'ROLE_TRADER', 'ROLE_GUEST']);

  const ordersQuery = useQuery({
    queryKey: ['consumer-orders', token],
    queryFn: () => apiClient.getConsumerOrders(token, logout),
    enabled: Boolean(token),
    refetchInterval: token ? 5000 : false,
  });

  const cancelMutation = useMutation({
    mutationFn: (orderId: number) => apiClient.cancelConsumerOrder(orderId, token, logout),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['consumer-orders'] });
    },
  });

  if (!token) {
    return <InlineAuthPanel title="내 주문 보기" subtitle="게스트 세션을 시작하면 내 paper account 기준 주문 상태를 볼 수 있습니다." />;
  }

  if (ordersQuery.isLoading) {
    return <div className="page-grid"><section className="app-panel">주문 내역을 불러오는 중입니다.</section></div>;
  }

  if (ordersQuery.isError || !ordersQuery.data) {
    return <div className="page-grid"><section className="app-panel">주문 내역을 불러오지 못했습니다.</section></div>;
  }

  const { summary, orders } = ordersQuery.data;

  return (
    <div className="app-stack">
      <div className="summary-grid">
        <section className="summary-card"><span>총 주문</span><strong>{summary.totalOrders}</strong></section>
        <section className="summary-card"><span>체결 완료</span><strong>{summary.filledOrders}</strong></section>
        <section className="summary-card"><span>대기 주문</span><strong>{summary.workingOrders}</strong></section>
        <section className="summary-card"><span>거절 주문</span><strong>{summary.rejectedOrders}</strong></section>
      </div>

      <SectionCard title="주문 목록" subtitle="일반 사용자가 읽기 쉬운 카드형 주문 내역입니다.">
        <div className="stack-list">
          {orders.map((order) => (
            <article className="order-card" key={order.id}>
              <div className="order-card__header">
                <div>
                  <strong>{order.symbol}</strong>
                  <p>{order.accountCode} / {order.orderType}</p>
                </div>
                <Badge
                  label={order.status}
                  tone={order.status === 'FILLED' ? 'success' : order.status === 'WORKING' ? 'warning' : order.status === 'REJECTED' ? 'danger' : 'neutral'}
                />
              </div>
              <div className="order-card__body">
                <div>
                  <span>{order.side}</span>
                  <strong>{order.quantity.toFixed(2)}주</strong>
                </div>
                <div>
                  <span>체결 수량</span>
                  <strong>{order.filledQuantity.toFixed(2)}주</strong>
                </div>
                <div>
                  <span>지정가</span>
                  <strong>{order.limitPrice ? formatMoney(order.limitPrice, 2) : '시장가'}</strong>
                </div>
              </div>
              <div className="order-card__footer">
                <span>{formatDateTime(order.updatedAt)}</span>
                <div className="inline-actions">
                  <Link className="ghost-button" to={`/portfolio/orders/${order.id}`}>상세</Link>
                  {order.cancelable ? (
                    <button
                      className="ghost-button"
                      type="button"
                      disabled={!canTrade || cancelMutation.isPending}
                      onClick={() => cancelMutation.mutate(order.id)}
                    >
                      {canTrade ? '취소' : '권한 없음'}
                    </button>
                  ) : null}
                </div>
              </div>
            </article>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}

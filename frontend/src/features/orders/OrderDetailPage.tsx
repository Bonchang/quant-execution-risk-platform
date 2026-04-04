import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { Badge } from '../../components/Badge';
import { DataTable } from '../../components/DataTable';
import { SectionCard } from '../../components/SectionCard';
import { useAuth } from '../../lib/auth/AuthContext';
import { hasRequiredRole } from '../../lib/auth/token';
import { apiClient } from '../../lib/api/client';

function money(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function OrderDetailPage() {
  const { id = '' } = useParams();
  const queryClient = useQueryClient();
  const { token, role, logout } = useAuth();
  const detailQuery = useQuery({
    queryKey: ['order-detail-page', id, token],
    queryFn: () => apiClient.getOrder(id, token, logout),
    enabled: Boolean(token && id),
  });
  const cancelMutation = useMutation({
    mutationFn: () => apiClient.cancelOrder(Number(id), token, logout),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['order-detail-page', id, token] });
      await queryClient.invalidateQueries({ queryKey: ['console-overview'] });
    },
  });

  if (!token) {
    return <SectionCard title="인증 필요">주문 상세는 로그인 후 확인할 수 있습니다.</SectionCard>;
  }

  const detail = detailQuery.data;
  const canCancel = detail?.status === 'WORKING' && hasRequiredRole(role, ['ROLE_ADMIN', 'ROLE_TRADER']);

  return (
    <div className="page-grid">
      <section className="hero-panel">
        <p>Order Detail</p>
        <h1>Order #{detail?.id ?? id}</h1>
        <p>{detail?.instrumentSymbol ?? '-'} · {detail?.strategyName ?? '-'} · {detail?.accountCode ?? '-'}</p>
      </section>

      <div className="two-col">
        <SectionCard
          title="Core Fields"
          actions={canCancel ? <button className="ghost-button" type="button" onClick={() => cancelMutation.mutate()}>Cancel Order</button> : null}
        >
          <ul className="info-list">
            <li><strong>Status</strong><span><Badge label={detail?.status ?? '-'} tone={detail?.status === 'WORKING' ? 'warning' : 'success'} /></span></li>
            <li><strong>Side / Type</strong><span>{detail?.side} / {detail?.orderType}</span></li>
            <li><strong>Quantity</strong><span>{detail?.quantity ?? '-'}</span></li>
            <li><strong>Limit Price</strong><span>{detail?.limitPrice ?? '-'}</span></li>
            <li><strong>Reserved Cash</strong><span>{money(detail?.reservedCashAmount)}</span></li>
            <li><strong>Available Cash</strong><span>{money(detail?.availableCash)}</span></li>
          </ul>
        </SectionCard>
        <SectionCard title="Account / Timing">
          <ul className="info-list">
            <li><strong>Owner</strong><span>{detail?.ownerName ?? '-'}</span></li>
            <li><strong>Strategy Run</strong><span>{detail?.strategyRunId ?? '-'}</span></li>
            <li><strong>Created At</strong><span>{detail?.createdAt ?? '-'}</span></li>
            <li><strong>Updated At</strong><span>{detail?.updatedAt ?? '-'}</span></li>
            <li><strong>Last Executed</strong><span>{detail?.lastExecutedAt ?? '-'}</span></li>
          </ul>
        </SectionCard>
      </div>

      <div className="two-col">
        <SectionCard title="Fills">
          <DataTable
            rows={detail?.fills ?? []}
            columns={[
              { header: 'ID', render: (row) => row.id },
              { header: 'Qty', render: (row) => row.fillQuantity },
              { header: 'Price', render: (row) => money(row.fillPrice) },
              { header: 'Filled At', render: (row) => row.filledAt },
            ]}
          />
        </SectionCard>
        <SectionCard title="Risk Checks">
          <DataTable
            rows={detail?.riskChecks ?? []}
            columns={[
              { header: 'Rule', render: (row) => row.ruleName },
              { header: 'Pass', render: (row) => <Badge label={row.passed ? 'PASS' : 'FAIL'} tone={row.passed ? 'success' : 'danger'} /> },
              { header: 'Message', render: (row) => row.message },
            ]}
          />
        </SectionCard>
      </div>

      <div className="two-col">
        <SectionCard title="Outbox">
          <DataTable
            rows={detail?.outboxEvents ?? []}
            columns={[
              { header: 'Event', render: (row) => row.eventType },
              { header: 'Status', render: (row) => row.processingStatus },
              { header: 'Created', render: (row) => row.createdAt },
            ]}
          />
        </SectionCard>
        <SectionCard title="Cash Ledger">
          <DataTable
            rows={detail?.cashLedgerEntries ?? []}
            columns={[
              { header: 'Type', render: (row) => row.entryType },
              { header: 'Amount', render: (row) => money(row.amount) },
              { header: 'Available', render: (row) => money(row.availableCashAfter) },
              { header: 'Reserved', render: (row) => money(row.reservedCashAfter) },
            ]}
          />
        </SectionCard>
      </div>
    </div>
  );
}

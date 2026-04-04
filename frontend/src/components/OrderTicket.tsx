import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { hasRequiredRole } from '../lib/auth/token';
import { useAuth } from '../lib/auth/AuthContext';
import { apiClient } from '../lib/api/client';
import type { StockDetailDto } from '../lib/types/api';
import { InlineAuthPanel } from './InlineAuthPanel';
import { SectionCard } from './SectionCard';

function money(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function OrderTicket({
  symbol,
  bidPrice,
  askPrice,
  tradeContext,
}: {
  symbol: string;
  bidPrice: number;
  askPrice: number;
  tradeContext: StockDetailDto['tradeContext'];
}) {
  const { token, role, logout } = useAuth();
  const queryClient = useQueryClient();
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
  const [orderType, setOrderType] = useState<'MARKET' | 'LIMIT'>('MARKET');
  const [quantity, setQuantity] = useState('1');
  const [limitPrice, setLimitPrice] = useState('');
  const [message, setMessage] = useState('실시간 quote 기준 예상 체결가를 안내합니다.');

  const canTrade = hasRequiredRole(role, ['ROLE_ADMIN', 'ROLE_TRADER', 'ROLE_GUEST']);
  const quantityNumber = Number(quantity || 0);
  const referencePrice = side === 'BUY' ? askPrice : bidPrice;
  const expectedAmount = useMemo(() => quantityNumber * (orderType === 'LIMIT' && limitPrice ? Number(limitPrice) : referencePrice), [limitPrice, orderType, quantityNumber, referencePrice]);

  const createMutation = useMutation({
    mutationFn: () => {
      if (!token || !tradeContext) {
        throw new Error('주문 컨텍스트를 찾을 수 없습니다.');
      }
      return apiClient.createConsumerOrder(
        {
          symbol,
          side,
          quantity,
          orderType,
          timeInForce: 'DAY',
          limitPrice: orderType === 'LIMIT' ? limitPrice : null,
        },
        token,
        logout,
      );
    },
    onSuccess: async () => {
      setMessage('주문이 접수되었습니다. 내 주문과 포트폴리오에 반영되는 중입니다.');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['stock', symbol] }),
        queryClient.invalidateQueries({ queryKey: ['consumer-orders'] }),
        queryClient.invalidateQueries({ queryKey: ['portfolio'] }),
        queryClient.invalidateQueries({ queryKey: ['app-home'] }),
        queryClient.invalidateQueries({ queryKey: ['app-me'] }),
      ]);
    },
    onError: (error: Error) => setMessage(error.message),
  });

  if (!token) {
    return <InlineAuthPanel title="주문하려면 로그인" subtitle="게스트 세션을 시작하면 전용 paper account로 바로 매수와 매도를 시도할 수 있습니다." />;
  }

  if (!tradeContext) {
    return (
      <SectionCard title="주문 불가" subtitle="연결된 전략이 아직 없습니다.">
        <p className="muted">전략 run이 생성되면 이 종목에서 바로 주문할 수 있습니다.</p>
      </SectionCard>
    );
  }

  return (
    <SectionCard title="주문하기" subtitle={`${tradeContext.accountCode} / ${tradeContext.strategyName}`}>
      <div className="ticket-toggle">
        <button className={side === 'BUY' ? 'segmented-button is-buy' : 'segmented-button'} type="button" onClick={() => setSide('BUY')}>매수</button>
        <button className={side === 'SELL' ? 'segmented-button is-sell' : 'segmented-button'} type="button" onClick={() => setSide('SELL')}>매도</button>
      </div>
      <div className="form-grid">
        <label>
          주문 유형
          <select value={orderType} onChange={(event) => setOrderType(event.target.value as 'MARKET' | 'LIMIT')}>
            <option value="MARKET">시장가</option>
            <option value="LIMIT">지정가</option>
          </select>
        </label>
        <label>
          수량
          <input value={quantity} onChange={(event) => setQuantity(event.target.value)} />
        </label>
        {orderType === 'LIMIT' ? (
          <label>
            지정가
            <input value={limitPrice} onChange={(event) => setLimitPrice(event.target.value)} />
          </label>
        ) : null}
      </div>
      <ul className="info-list">
        <li>
          <strong>예상 체결 기준</strong>
          <span>{side === 'BUY' ? `ask ${money(askPrice)}` : `bid ${money(bidPrice)}`}</span>
        </li>
        <li>
          <strong>예상 주문 금액</strong>
          <span>{money(expectedAmount)}</span>
        </li>
        <li>
          <strong>주문 가능 현금</strong>
          <span>{money(tradeContext.availableCash)}</span>
        </li>
      </ul>
      <button className="primary-button" type="button" disabled={!canTrade || createMutation.isPending} onClick={() => createMutation.mutate()}>
        {canTrade ? '주문 실행' : '조회 전용 세션은 주문 불가'}
      </button>
      <p className="muted">{message}</p>
    </SectionCard>
  );
}

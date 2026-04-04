import { describe, expect, it } from 'vitest';
import { buildCreateOrderPayload } from './orders';

describe('buildCreateOrderPayload', () => {
  it('LIMIT 주문일 때 limitPrice를 유지한다', () => {
    expect(buildCreateOrderPayload({
      accountId: '1',
      strategyRunId: '2',
      instrumentId: '3',
      side: 'BUY',
      quantity: '10.000000',
      orderType: 'LIMIT',
      timeInForce: 'DAY',
      limitPrice: '101.500000',
      clientOrderId: 'client-001',
    })).toEqual({
      accountId: 1,
      strategyRunId: 2,
      instrumentId: 3,
      side: 'BUY',
      quantity: '10.000000',
      orderType: 'LIMIT',
      timeInForce: 'DAY',
      limitPrice: '101.500000',
      clientOrderId: 'client-001',
    });
  });

  it('MARKET 주문일 때 limitPrice를 null로 비운다', () => {
    expect(buildCreateOrderPayload({
      accountId: '1',
      strategyRunId: '2',
      instrumentId: '3',
      side: 'SELL',
      quantity: '5.000000',
      orderType: 'MARKET',
      timeInForce: 'GTC',
      limitPrice: '99.000000',
      clientOrderId: 'client-002',
    }).limitPrice).toBeNull();
  });
});

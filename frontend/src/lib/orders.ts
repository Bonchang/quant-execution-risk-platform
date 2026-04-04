export interface CreateOrderFormState {
  accountId: string;
  strategyRunId: string;
  instrumentId: string;
  side: 'BUY' | 'SELL';
  quantity: string;
  orderType: 'MARKET' | 'LIMIT';
  timeInForce: 'DAY' | 'GTC';
  limitPrice: string;
  clientOrderId: string;
}

export function buildCreateOrderPayload(form: CreateOrderFormState) {
  return {
    accountId: Number(form.accountId),
    strategyRunId: Number(form.strategyRunId),
    instrumentId: Number(form.instrumentId),
    side: form.side,
    quantity: form.quantity,
    orderType: form.orderType,
    timeInForce: form.timeInForce,
    limitPrice: form.orderType === 'LIMIT' && form.limitPrice ? form.limitPrice : null,
    clientOrderId: form.clientOrderId,
  };
}

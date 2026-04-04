export function SignalStrengthBar({ value }: { value: number }) {
  return (
    <div className="signal-strength">
      <div className="signal-strength__track">
        <div className="signal-strength__fill" style={{ width: `${Math.min(100, Math.max(0, value))}%` }} />
      </div>
      <span>{value}%</span>
    </div>
  );
}

import { Badge } from './Badge';

export function InsightCard({
  title,
  body,
  tone,
}: {
  title: string;
  body: string;
  tone: 'live' | 'stale' | 'success' | 'warning' | 'danger' | 'neutral';
}) {
  return (
    <article className="insight-card">
      <div className="insight-card__header">
        <h3>{title}</h3>
        <Badge label={title} tone={tone} />
      </div>
      <p>{body}</p>
    </article>
  );
}

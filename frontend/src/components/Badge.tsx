import styles from './Badge.module.css';

type BadgeTone = 'live' | 'stale' | 'success' | 'warning' | 'danger' | 'neutral';

const toneClass: Record<BadgeTone, string> = {
  live: styles.live,
  stale: styles.stale,
  success: styles.success,
  warning: styles.warning,
  danger: styles.danger,
  neutral: styles.neutral,
};

export function Badge({ label, tone = 'neutral' }: { label: string; tone?: BadgeTone }) {
  return <span className={`${styles.badge} ${toneClass[tone]}`}>{label}</span>;
}

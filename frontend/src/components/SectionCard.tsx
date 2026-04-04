import type { PropsWithChildren, ReactNode } from 'react';
import styles from './SectionCard.module.css';

export function SectionCard({
  title,
  subtitle,
  actions,
  children,
}: PropsWithChildren<{ title: string; subtitle?: string; actions?: ReactNode }>) {
  return (
    <section className={styles.card}>
      <header className={styles.header}>
        <div>
          <h2>{title}</h2>
          {subtitle ? <p>{subtitle}</p> : null}
        </div>
        {actions ? <div className={styles.actions}>{actions}</div> : null}
      </header>
      <div>{children}</div>
    </section>
  );
}

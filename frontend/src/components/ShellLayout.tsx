import type { PropsWithChildren } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../lib/auth/AuthContext';
import { Badge } from './Badge';
import styles from './ShellLayout.module.css';

export function ShellLayout({ children }: PropsWithChildren) {
  const { role, token, logout } = useAuth();

  return (
    <div className={styles.page}>
      <div className={styles.atmosphere} />
      <header className={styles.header}>
        <Link to="/" className={styles.brand}>
          <span>QERP</span>
          <small>Quote-Driven Trading Console</small>
        </Link>
        <nav className={styles.nav}>
          <NavLink to="/">Home</NavLink>
          <NavLink to="/architecture">Architecture</NavLink>
          <NavLink to="/research">Research</NavLink>
          <NavLink to="/console">Console</NavLink>
          <NavLink to="/console/research-link">Research Link</NavLink>
        </nav>
        <div className={styles.auth}>
          {token ? <Badge label={role || 'ROLE_VIEWER'} tone="live" /> : <Badge label="PUBLIC" tone="neutral" />}
          {token ? (
            <button className={styles.ghostButton} type="button" onClick={logout}>
              로그아웃
            </button>
          ) : null}
        </div>
      </header>
      <main className={styles.content}>{children}</main>
    </div>
  );
}

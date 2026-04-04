import type { PropsWithChildren } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../lib/auth/AuthContext';
import { useMode } from '../lib/mode/ModeContext';
import { Badge } from './Badge';
import { BottomTabBar } from './BottomTabBar';
import { ModeToggle } from './ModeToggle';
import styles from './ShellLayout.module.css';

export function ShellLayout({ children }: PropsWithChildren) {
  const { role, token, logout } = useAuth();
  const { mode } = useMode();

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <Link to="/" className={styles.brand}>
          <span>QERP</span>
          <small>소비자 투자앱 + 퀀트 모드</small>
        </Link>
        <nav className={styles.nav}>
          <NavLink to="/">홈</NavLink>
          <NavLink to="/discover">탐색</NavLink>
          <NavLink to="/portfolio">내 자산</NavLink>
          <NavLink to="/orders">주문</NavLink>
          <NavLink to="/quant">퀀트</NavLink>
        </nav>
        <div className={styles.auth}>
          <div className={styles.modeSlot}>
            <ModeToggle />
          </div>
          <Badge label={mode === 'quant' ? 'QUANT' : 'INVEST'} tone={mode === 'quant' ? 'live' : 'neutral'} />
          {token ? <Badge label={role || 'ROLE_VIEWER'} tone="live" /> : <Badge label="PUBLIC" tone="neutral" />}
          <NavLink to="/profile" className={styles.profileLink}>
            프로필
          </NavLink>
          {token ? (
            <button className={styles.ghostButton} type="button" onClick={logout}>
              로그아웃
            </button>
          ) : null}
        </div>
      </header>
      <main className={styles.content}>{children}</main>
      <BottomTabBar />
    </div>
  );
}

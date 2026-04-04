import { NavLink } from 'react-router-dom';

const tabs = [
  { to: '/', label: '홈' },
  { to: '/discover', label: '탐색' },
  { to: '/portfolio', label: '내 자산' },
  { to: '/orders', label: '주문' },
  { to: '/quant', label: '퀀트' },
];

export function BottomTabBar() {
  return (
    <nav className="bottom-tab-bar" aria-label="주요 메뉴">
      {tabs.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          className={({ isActive }) => (isActive ? 'bottom-tab-bar__link is-active' : 'bottom-tab-bar__link')}
        >
          {tab.label}
        </NavLink>
      ))}
    </nav>
  );
}

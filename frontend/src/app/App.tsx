import { Outlet } from 'react-router-dom';
import { ShellLayout } from '../components/ShellLayout';

export function App() {
  return (
    <ShellLayout>
      <Outlet />
    </ShellLayout>
  );
}

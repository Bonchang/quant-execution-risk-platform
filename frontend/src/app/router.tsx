import { createBrowserRouter } from 'react-router-dom';
import { App } from './App';
import { LandingPage } from '../pages/LandingPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <LandingPage /> },
      {
        path: 'architecture',
        lazy: async () => ({ Component: (await import('../pages/ArchitecturePage')).ArchitecturePage }),
      },
      {
        path: 'research',
        lazy: async () => ({ Component: (await import('../pages/ResearchListPage')).ResearchListPage }),
      },
      {
        path: 'research/:runId',
        lazy: async () => ({ Component: (await import('../pages/ResearchDetailPage')).ResearchDetailPage }),
      },
      {
        path: 'console',
        lazy: async () => ({ Component: (await import('../features/console/ConsolePage')).ConsolePage }),
      },
      {
        path: 'console/orders/:id',
        lazy: async () => ({ Component: (await import('../features/orders/OrderDetailPage')).OrderDetailPage }),
      },
      {
        path: 'console/research-link',
        lazy: async () => ({ Component: (await import('../pages/ResearchLinkPage')).ResearchLinkPage }),
      },
    ],
  },
]);

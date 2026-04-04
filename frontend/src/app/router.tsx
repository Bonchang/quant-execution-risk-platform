import { createBrowserRouter } from 'react-router-dom';
import { App } from './App';
import { HomePage } from '../pages/HomePage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <HomePage /> },
      {
        path: 'discover',
        lazy: async () => ({ Component: (await import('../pages/DiscoverPage')).DiscoverPage }),
      },
      {
        path: 'stocks/:symbol',
        lazy: async () => ({ Component: (await import('../pages/StockDetailPage')).StockDetailPage }),
      },
      {
        path: 'portfolio',
        lazy: async () => ({ Component: (await import('../pages/PortfolioPage')).PortfolioPage }),
      },
      {
        path: 'orders',
        lazy: async () => ({ Component: (await import('../pages/OrdersPage')).OrdersPage }),
      },
      {
        path: 'portfolio/orders/:id',
        lazy: async () => ({ Component: (await import('../features/orders/OrderDetailPage')).OrderDetailPage }),
      },
      {
        path: 'quant',
        lazy: async () => ({ Component: (await import('../pages/QuantPage')).QuantPage }),
      },
      {
        path: 'quant/strategies/:runId',
        lazy: async () => ({ Component: (await import('../pages/QuantStrategyPage')).QuantStrategyPage }),
      },
      {
        path: 'profile',
        lazy: async () => ({ Component: (await import('../pages/ProfilePage')).ProfilePage }),
      },
      {
        path: 'architecture',
        lazy: async () => ({ Component: (await import('../pages/ArchitecturePage')).ArchitecturePage }),
      },
      {
        path: 'research',
        lazy: async () => ({ Component: (await import('../pages/QuantPage')).QuantPage }),
      },
      {
        path: 'research/:runId',
        lazy: async () => ({ Component: (await import('../pages/QuantStrategyPage')).QuantStrategyPage }),
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

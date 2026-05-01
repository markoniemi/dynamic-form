import '@testing-library/jest-dom';
import {vi} from 'vitest';

// Default mock for /api/config/oauth2-issuer-uri endpoint
globalThis.fetch = vi.fn(
  (url: string | Request) => {
    const urlStr = typeof url === 'string' ? url : url.toString();

    if (urlStr.includes('/api/config/oauth2-issuer-uri')) {
      return Promise.resolve(
        new Response('http://localhost:9000', {
          status: 200,
          headers: { 'Content-Type': 'text/plain' },
        })
      );
    }

    return Promise.resolve(new Response('Not found', { status: 404 }));
  }
) as unknown as typeof fetch;

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: {
      changeLanguage: () => new Promise(() => {}),
    },
  }),
  initReactI18next: {
    type: '3rdParty',
    init: () => {},
  },
}));

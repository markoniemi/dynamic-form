import '@testing-library/jest-dom';
import createFetchMock from 'vitest-fetch-mock';
import {vi} from 'vitest';

const fetchMocker = createFetchMock(vi);
fetchMocker.enableMocks();

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

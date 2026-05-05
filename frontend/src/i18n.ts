import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import Backend from 'i18next-http-backend';

i18n
  .use(Backend)
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'en',
    debug: true,
    interpolation: {
      escapeValue: false, // not needed for react as it escapes by default
    },
    backend: {
      loadPath: '/locales/{{lng}}/translation.json',
    },
  });

// i18next v26: Use formatter.add() for custom formatters
i18n.services.formatter?.add('date', (value: unknown, lng: string | undefined, options: { format?: 'short' | 'long' } | undefined) => {
  if (!(value instanceof Date)) {
    return String(value);
  }
  const language = lng || 'en';
  if (options?.format === 'short') {
    return new Intl.DateTimeFormat(language).format(value);
  }
  if (options?.format === 'long') {
    return new Intl.DateTimeFormat(language, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
    }).format(value);
  }
  return String(value);
});

export default i18n;

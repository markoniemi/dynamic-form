import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';
import pluginReact from 'eslint-plugin-react';

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'target'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.strict, pluginReact.configs.flat['jsx-runtime']],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
      // Type information, for the three type-aware rules below only. The rest of
      // tseslint.configs.strictTypeChecked is deliberately not enabled: it
      // reported 106 findings here, of which ~100 were test idioms or
      // react-query/react-router conventions.
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': [
        'warn',
        { allowConstantExport: true },
      ],
      '@typescript-eslint/no-floating-promises': 'error',
      '@typescript-eslint/no-base-to-string': 'error',
      // attributes: false — an async onClick/onSubmit handler, and
      // react-hook-form's handleSubmit(), are the documented usage.
      '@typescript-eslint/no-misused-promises': [
        'error',
        { checksVoidReturn: { attributes: false } },
      ],
    },
  }
);

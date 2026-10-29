# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some Oxlint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the Oxlint configuration

If you are developing a production application, we recommend enabling type-aware lint rules by installing `oxlint-tsgolint` and editing `.oxlintrc.json`:

```json
{
  "$schema": "./node_modules/oxlint/configuration_schema.json",
  "plugins": ["react", "typescript", "oxc"],
  "options": {
    "typeAware": true
  },
  "rules": {
    "react/rules-of-hooks": "error",
    "react/only-export-components": ["warn", { "allowConstantExport": true }]
  }
}
```

See the [Oxlint rules documentation](https://oxc.rs/docs/guide/usage/linter/rules) for the full list of rules and categories.

## Contato público de privacidade

Defina VITE_PRIVACY_RESPONSIBLE com o nome público do responsável e VITE_PRIVACY_CONTACT_EMAIL com o e-mail público de atendimento. Os valores aparecem no aviso de cadastro das três trilhas; não são credenciais SMTP e não devem conter segredos. Campos vazios não geram contatos fictícios. O e-mail só recebe link quando possui formato válido. Reinicie o Vite em desenvolvimento ou refaça o build para aplicar alterações, pois variáveis VITE são públicas e incorporadas ao frontend. A definição desses dados não substitui a revisão dos documentos jurídicos, proteção de adolescentes e retenção antes da publicação.

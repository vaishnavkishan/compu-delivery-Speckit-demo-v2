# React + TypeScript + Vite

## Running the app locally

This app talks to the `order-api` backend, which needs Postgres and RabbitMQ.
From the repo root, one command starts all of it (infra via
`deploy/docker-compose.yml`, the Spring Boot backend, then this Vite dev
server in the foreground):

```bash
./dev.sh
```

Once it's up: portal at http://localhost:5173, API at http://localhost:8080
(Swagger UI at `/swagger-ui.html`). Ctrl-C stops the frontend and backend;
Postgres/RabbitMQ keep running in Docker (`docker compose -f deploy/docker-compose.yml down` to stop them too).

Requires Docker, Java 25, Maven, and Node.js already installed. If you'd
rather run each piece yourself, see `specs/001-bulk-hardware-orders/quickstart.md`.

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

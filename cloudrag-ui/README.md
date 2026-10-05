# CloudRAG UI

React + TypeScript + Vite frontend for CloudRAG.

## Structure

- `src/components/auth` - login/register screen
- `src/components/sidebar` - conversations and logout
- `src/components/chat` - RAG chat UI and sources
- `src/components/documents` - PDF upload, selection and deletion
- `src/hooks/useAuth.ts` - JWT authentication state
- `src/services/api.ts` - authenticated API wrapper
- `src/services/authService.ts` - login/register calls
- `src/types` - shared TypeScript models
- `src/App.tsx` - application orchestration only

## Run

```bash
npm install
npm run dev
```

Backend should be running at `http://localhost:8080`.

## Verification

- TypeScript: `npx tsc -b` passes.
- ESLint: `npm run lint` passes.
- `npm run build` could not be completed in the packaging environment because the uploaded `node_modules` is missing Vite/Rolldown's native optional binding. On a normal local install, run `npm install` and then `npm run build`.

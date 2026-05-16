# Batch 5 Fix Report

## Fixed issues

- AI title generation returned generic HTTP 400 during manual acceptance.
- AI summary generation returned generic HTTP 400 during manual acceptance.
- The UI hid the real backend/upstream error message.

## Root causes

1. AI settings key mismatch: frontend saved `ai.*` keys, while generation endpoints read unprefixed keys.
2. Error handling gap: backend dropped provider error details and frontend displayed the raw Axios 400 message.

## Fixes

- `AiAdminService` now reads frontend-saved `ai.base_url`, `ai.api_key`, `ai.model`, and prompt keys, while retaining compatibility with unprefixed keys used by tests/older callers.
- AI provider non-2xx responses now include a safe extracted provider message in the backend response.
- Admin Axios response interceptor now rejects with backend envelope `message` for HTTP error responses.
- AI settings UI now uses `deepseek-v4-flash` as the example model and tells the user to save after a successful connection test.

## Changed areas

- Backend AI service and regression tests.
- Admin request error handling.
- Admin AI settings helper text.
- Batch 5 audit and execution documentation.
- Batch issue record workflow documentation.

## Verification

- `mvn -f server/pom.xml -Dtest=Batch5AiArticleMetadataClosedLoopTest test` — PASS, 3 tests.
- `mvn -f server/pom.xml test` — PASS, 53 tests.
- `npm --prefix admin run type-check` — PASS.
- `npm --prefix blog run type-check` — completed with the existing local `@vue/language-core` warning.
- `docker compose up --build -d server admin blog` — running stack started.
- Docker backend AI title endpoint returned a real generated title with saved DeepSeek-compatible settings.

## Updated manual acceptance steps

1. Open `http://localhost:4000`.
2. Go to `系统设置 -> AI 配置`.
3. Fill `https://api.deepseek.com`, the local DeepSeek API key, and `deepseek-v4-flash` or `deepseek-v4-pro`.
4. Click `测试连接`.
5. Click `保存配置` after the test succeeds.
6. Open the article editor.
7. Generate title, `生成摘要`, and `AI 总结`.
8. Save the article.
9. Reopen or refresh the editor and confirm title/summary persist.
10. Open the public article page and confirm it uses the saved `summary`.

## Acceptance result

Accepted by the user after re-verification on 2026-05-17.

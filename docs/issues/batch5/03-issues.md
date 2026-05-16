# Batch 5 Failed Acceptance Issues

## Issue 1: AI title and summary generation returned generic 400

### Phenomenon

Manual acceptance of Batch 5 failed on the admin article editor:

- DeepSeek AI configuration test succeeded in the AI settings page.
- Clicking the title AI icon on the article editor showed `Request failed with status code 400`.
- Clicking `生成摘要` in the article settings drawer showed the same `Request failed with status code 400`.
- The page did not expose the actual backend or upstream provider error.

### Expected behavior

After AI configuration is saved, article title and summary generation should call the saved OpenAI-compatible provider configuration, return generated text, write it into `title` or `summary`, and allow the generated value to be saved with the article.

### Cause

There were two separate causes:

1. The admin settings page saved AI configuration keys as `ai.base_url`, `ai.api_key`, and `ai.model`, but `AiAdminService` read saved generation settings as `base_url`, `api_key`, and `model`. This made the generation endpoints fail even when the settings page's test connection could succeed.
2. The backend discarded upstream AI provider error bodies, and the admin Axios interceptor returned the raw Axios error for HTTP 400 responses. This hid actionable error details behind the generic `Request failed with status code 400` message.

### Solution

- `AiAdminService` now accepts both saved key formats and prefers the real frontend-saved `ai.*` keys.
- AI prompt lookup also supports both `ai.summary_prompt` / `ai.ai_summary_prompt` / `ai.title_prompt` and the older unprefixed keys.
- Upstream AI provider 4xx/5xx responses now surface a safe provider message from `error.message` or `message`.
- The admin request interceptor unwraps backend `{ code, message, data }` error envelopes for non-2xx responses.
- The AI settings tab now hints current DeepSeek model naming and reminds that test success must be followed by saving the configuration.

### Key code

- `server/src/main/java/com/zblog/ai/AiAdminService.java` — saved AI config key compatibility and provider error message extraction.
- `admin/src/utils/request.ts` — HTTP error envelope message unwrapping.
- `admin/src/views/setting/components/AISettingsTab.vue` — DeepSeek model placeholder and save-after-test hint.
- `server/src/test/java/com/zblog/server/Batch5AiArticleMetadataClosedLoopTest.java` — regression tests for frontend-saved AI key format and readable provider errors.

### Verification

- PASS: `mvn -f server/pom.xml -Dtest=Batch5AiArticleMetadataClosedLoopTest test` - 3 tests, 0 failures, 0 errors.
- PASS: `mvn -f server/pom.xml test` - 53 tests, 0 failures, 0 errors.
- PASS: `npm --prefix admin run type-check`.
- OBSERVED: `npm --prefix blog run type-check` completed with the existing local `vue-router/volar/sfc-route-blocks` warning for missing `@vue/language-core`.
- PASS: Docker running-stack backend generated a real AI title through saved DeepSeek-compatible settings.
- ACCEPTED: user manually verified DeepSeek config, AI title generation, summary generation, save/reopen persistence, and public summary behavior on 2026-05-17.

### Residual risk or deferred work

- `deepseek-chat` is legacy and expected to be replaced by `deepseek-v4-flash` or `deepseek-v4-pro`; the current UI hint was updated, but no forced migration is done.
- API keys must remain local/user-provided and must not be committed or documented.

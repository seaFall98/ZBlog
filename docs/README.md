# ZBlog Docs

Read these documents in order:

1. `EXECUTION_LOCK.md` - current execution source of truth.
2. `DELIVERY_AUDIT.md` - audited implementation status and remaining gaps.
3. `AI_CODING_PLAYBOOK.md` - reusable AI coding workflow guidance.
4. `0_PROJECT_CONTEXT.md` - stable product and architecture context.
5. `RETRO_ACCEPTANCE_LOOP.md` - why phase labels are not completion evidence.
6. `ATTRIBUTION.md` - upstream/reference attribution notes.

Rules for future agents:

- Do not create new root-level phase plans, harness logs, or API matrix documents.
- Keep execution state in `EXECUTION_LOCK.md`.
- Keep verified status in `DELIVERY_AUDIT.md`.
- Treat `archive/` as historical reference only.
- If old documents conflict with the execution lock or delivery audit, the execution lock and delivery audit win.

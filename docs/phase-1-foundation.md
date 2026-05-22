# Phase 1 Foundation

This phase establishes the local-first project foundation:

- Spring Boot backend with layered packages and Tab CRUD APIs.
- Java 17 backend runtime and build target.
- SQLite persistence at `storage/app.db`.
- Vue frontend shell with sidebar tab creation and dynamic tab listing.
- Phase 2 adds DOCX-only template upload for tab creation and stores uploaded templates in `templates/`.
- The old placeholder-based export path has been removed. Users do not need to insert `{{title}}`, `{{content}}`, or similar placeholders into DOCX files.
- Current template direction is DOCX structure analysis: the backend reads uploaded templates as-is and extracts paragraphs, table cells, and likely labels for future AI mapping.
- Phase 7 persists template analysis results and stores editable semantic field mappings so detected labels such as `활동 목표` can map to standard keys such as `activityGoal`.
- Phase 8 stores generated drafts as structured JSON keyed by saved semantic mappings, and the frontend edits those fields instead of a single long plain-text draft.
- Phase 9 adds a read-only placement preview that shows where structured draft fields would map into analyzed DOCX labels/blocks without modifying or exporting DOCX files.
- Phase 10 adds a read-only DOCX insertion write plan that proposes table-cell or paragraph operations without modifying or exporting DOCX files.
- Phase 11 corrects the AI architecture so Ollama is the primary structured generation provider, while Mock remains an explicit development fallback.
- Local AI settings are persisted in SQLite with provider, Ollama URL, model name, and request timeout.
- Phase 12 hardens Ollama runtime handling, including fenced/prose-wrapped JSON recovery, missing-field defaults, timeout/model errors, and local inference smoke guidance.
- Phase 13 adds intelligent DOCX reconstruction from analyzed template structure, semantic mappings, structured drafts, and write plans without placeholders.
- Phase 14 persists and displays a reconstruction summary with written/skipped field counts and per-field skip reasons.

PDF/HWP support and placeholder-based export remain intentionally excluded.

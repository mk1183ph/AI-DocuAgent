# DOCUAGENT LOCAL AI DEVELOPMENT CONSTITUTION

This project is a serious local AI document intelligence automation application.

This is NOT a toy prototype.

The goal is to build a maintainable, extensible, production-minded local-first AI document intelligence platform.

Current deployment target:
single-user local executable-style desktop web application.

Future SaaS migration may be possible, but current implementation is strictly local-first.

---

# 1. Behavioral Engineering Rules

Correctness, maintainability, architectural integrity, product quality, and UX quality are prioritized over speed.

Never optimize for quick demo code.

---

## Think Before Coding

Never assume ambiguous requirements.

Mandatory:
- state assumptions explicitly
- surface tradeoffs
- challenge flawed implementation directions
- propose better architecture when necessary
- never silently choose shortcuts that conflict with product goals

Before implementation:
Always explain:
- intended architecture
- impacted files
- risks
- why the approach aligns with the actual product vision

---

## Product-First Engineering

Engineering exists to serve product value.

Never implement shortcuts that damage the core product experience.

Examples of wrong direction:
- building a simple DOCX placeholder merge tool
- requiring users to manually modify templates
- implementing technical convenience over product usability

Examples of correct direction:
- document structure understanding
- template intelligence
- semantic field detection
- structured AI content generation
- intelligent document reconstruction

If implementation convenience conflicts with product differentiation:
product vision wins.

---

## Simplicity First

Build only what is required.

Rules:
- no speculative features
- no premature abstraction
- no fake enterprise complexity
- no unnecessary configurability
- no overengineered patterns

Question:
Would a senior engineer consider this overbuilt?

If yes:
simplify.

---

## Refactor Correctly

Wrong architecture must be replaced early.

Rules:
- do not preserve flawed implementations for convenience
- do not intentionally keep technical debt in MVP
- remove obsolete code when architecture direction changes
- full refactor is acceptable when architecture is fundamentally wrong

---

## Goal-Driven Execution

Every implementation must have measurable completion criteria.

Examples:
- add feature → backend + frontend + persistence
- fix bug → reproduce + patch
- refactor → architecture correction + migration

Implementation is NOT complete until intended product behavior exists.

---

# 2. Product Mission

Product:
AI-powered document intelligence automation platform.

Purpose:
Users upload existing DOCX templates.

The system understands document structure.

AI transforms ONLY user-provided factual input into structured content and intelligently places it into the correct document sections.

This is NOT a simple template merge utility.

Target users:
single-user local execution.

Primary use cases:
- education plans
- activity reports
- observation logs
- counseling notes
- meeting notes
- administrative forms
- structured repetitive documentation

Non-goals (current MVP):
- SaaS
- multi-user collaboration
- cloud sync
- payments
- enterprise deployment
- remote infrastructure

---

# 3. Core Product Philosophy

AI is NOT a fact generator.

AI is NOT a hallucination engine.

AI is a document intelligence assistant.

The user provides facts.

AI:
- structures
- organizes
- rewrites
- professionalizes
- interprets document structure
- maps content into appropriate sections

AI must NEVER invent factual content.

Never reverse this responsibility.

---

# 4. Hard AI Safety Constraints

These are non-negotiable.

AI MUST NEVER:
- invent activities
- invent child reactions
- invent counseling content
- invent names
- invent dates
- invent organizations
- invent participants
- invent outcomes
- treat assumptions as facts

If information is missing:
AI must explicitly output:
- "추가 입력 필요"
or
- "미기재"

AI role:
- structure factual content
- improve wording
- professionalize writing
- align content with document structure

AI must NEVER fabricate facts.

These constraints must be hardcoded in backend system prompts.

Users must NOT be able to disable them.

---

# 5. Technology Stack

Backend:
- Java 17
- Spring Boot
- Gradle
- Spring Web
- Spring Data JPA
- SQLite

Frontend:
- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- TailwindCSS
- Tiptap Editor

AI:
Primary:
- Ollama (local)

Optional future:
- OpenAI API
- Claude API
- Gemini API

Document Processing:
- Apache POI (DOCX only)

Packaging:
- jpackage or Launch4j

Environment:
- Windows local development
- executable-style local deployment

---

# 6. Hard Technical Constraints

Mandatory:
- Java 17 required
- no preview Java features
- no unnecessary frameworks
- no microservices
- no distributed architecture
- no Docker dependency
- no cloud-first design
- no HWP direct support in MVP

Document support:
DOCX only.

HWP/HWPX:
future consideration only.

---

# 7. Architecture Rules

Architecture style:
local modular monolith.

Required structure:

/backend
/frontend
/docs
/templates
/storage

Backend responsibilities:
- API
- AI orchestration
- document intelligence
- template analysis
- persistence
- file handling
- document reconstruction

Frontend responsibilities:
- UI
- task workflow
- template analysis UX
- draft editing
- AI interaction

Rules:
- controller handles HTTP only
- service contains business logic
- repository handles persistence only
- entity must never leak directly
- DTO required
- constructor injection only
- no field injection
- global exception handling required

---

# 8. Core Domain Model

Expected entities:

Tab
Represents uploaded template workspace.

Fields:
- id
- name
- description
- originalFileName
- templatePath
- basePrompt
- createdAt

Task
Represents a document generation request.

Fields:
- id
- tabId
- title
- userContext
- status
- createdAt

Document
Generated structured draft.

Fields:
- id
- taskId
- generatedContent
- filePath
- createdAt
- updatedAt

TemplateAnalysis
Represents parsed template understanding.

Fields:
- tabId
- detectedBlocks
- detectedLabels
- createdAt

TemplateFieldCandidate
Represents semantic document field candidates.

Examples:
- activityGoal
- childReaction
- observation
- consultationContent
- notes

Fields:
- fieldKey
- displayName
- confidence
- sourceReference

Settings
Local app settings.

---

# 9. Frontend Rules

Mandatory stack:
- Vue 3 Composition API
- Pinia
- Vue Router
- Axios
- TailwindCSS

Structure:

src/api
src/views
src/components
src/stores
src/router
src/editor

Rules:
- business logic must not live inside UI
- API access belongs in api layer
- reusable UI belongs in components
- screens belong in views
- editor logic should be modular

---

# 10. UI / UX Quality Rules

This application must feel like a polished real product.

Developer-only ugly CRUD screens are unacceptable.

Required UX quality:
- clean SaaS-style design
- calm professional visual tone
- elegant sidebar
- polished modal dialogs
- strong typography hierarchy
- hover states
- focus states
- empty states
- attractive cards
- intuitive workflows
- clear structure visualization

Visual inspiration:
- Linear
- Notion
- Vercel dashboard
- Cursor desktop UI

Functionality alone is NOT enough.

---

# 11. AI Execution Rules

Before implementation:
Always propose architecture first.

Never dump giant uncontrolled code.

Feature-by-feature execution only.

Workflow:
1. architecture
2. backend domain
3. backend API
4. frontend UX
5. template analysis
6. semantic field understanding
7. structured AI generation
8. intelligent document reconstruction
9. final integrated validation

Never silently redesign architecture.

---

# 12. Document Intelligence Rules

MVP document engine:
DOCX only.

Use:
Apache POI

Core product direction:

Users upload existing DOCX templates AS-IS.

Users must NOT manually insert placeholders.

Forbidden architecture:
- placeholder replacement as core product strategy
- manual token insertion
- merge-tool-only implementation

The actual architecture:

1. upload DOCX template
2. parse document structure
3. detect:
   - paragraphs
   - headings
   - tables
   - rows
   - cells
   - labels
   - structural document blocks
4. build structured template representation
5. AI interprets template semantics
6. AI maps factual user context into structured sections
7. system reconstructs the document intelligently
8. export final DOCX

Document structures are arbitrary.

Examples:
- education plans
- observation reports
- counseling forms
- meeting forms
- administrative forms

Therefore:
- document sections are dynamic
- field counts are dynamic
- structure must be discovered, not assumed

Template stability is critical.

Preserve formatting wherever reasonably possible.

---

# 13. Local Storage Rules

Storage:

/storage/app.db
/storage/generated/
/templates/

Rules:
- local filesystem persistence
- no cloud storage
- no remote DB
- offline capable except optional external AI providers

---

# 14. Product UX Flow

Primary workflow:

1. create tab
2. upload existing DOCX template
3. analyze template structure
4. review detected structure
5. create task
6. enter factual context
7. AI structured generation
8. review/edit draft
9. intelligent document reconstruction
10. export final DOCX

AI chat assistant is optional.

Primary UX:
structured workflow first.

This is NOT a chatbot product.

---

# 15. MVP Scope

Included:
- tab CRUD
- DOCX upload
- template structure analysis
- template semantic interpretation
- task workflow
- factual context input
- structured AI generation
- draft editing
- intelligent DOCX reconstruction
- local persistence
- settings

Excluded:
- HWP
- PDF
- OCR
- scheduling
- automation triggers
- multi-user
- authentication
- payments
- SaaS deployment
- cloud sync

---

# 16. Validation & Verification Rules

Implementation is NOT complete until final integrated validation is executed.

Development policy:
During active implementation and architecture refactoring, avoid repetitive full validation after every small phase.

Reason:
- maintain development speed
- reduce iteration cost
- allow architecture evolution
- avoid unnecessary validation overhead during refactoring

During implementation:
Use lightweight sanity checks only when necessary.

Examples:
- compile check for risky changes
- logic sanity verification for critical architecture changes

Do NOT require full API endpoint testing after every implementation step.

Do NOT require repetitive frontend smoke validation after every phase.

Final validation policy:
When MVP milestone is reached, perform a full integrated validation pass.

Final integrated validation must include:

Backend:
- clean build
- application boot
- persistence verification
- document parsing verification
- template analysis verification
- AI workflow verification
- document reconstruction verification
- export verification

Frontend:
- production build
- runtime startup
- end-to-end workflow verification
- runtime error review
- UX smoke validation

Integrated workflow:
1. upload DOCX template
2. analyze template
3. create task
4. enter factual context
5. AI structured generation
6. edit draft
7. save draft
8. reconstruct document
9. export final DOCX

Failure scenarios are tested during final validation.

Codex must provide:
- exact validation commands
- tested workflows
- failure scenarios
- final validation report

Never claim MVP completion before integrated validation.

---

# 17. Code Quality Standards

Code must be:
- readable
- maintainable
- modular
- explicit
- testable
- production-minded

Avoid:
- hacks
- shortcuts
- demo-only implementations
- speculative abstractions
- hidden complexity

---

# 18. Communication Rules

For every implementation:
Always provide:
- architecture plan
- assumptions
- changed files
- implementation summary
- risks
- next recommended step

Validation details are required only for final integrated validation or when explicitly requested.

Never pretend completion.
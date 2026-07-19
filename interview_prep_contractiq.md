# ContractIQ — Interview Preparation Guide

> **Interview Tomorrow** — Full project deep-dive with likely Q&A for every layer of the stack.

---

## 1. Project Overview (Your Elevator Pitch)

**ContractIQ** is an AI-powered contract intelligence platform that lets legal teams upload a PDF or DOCX contract and instantly receive:

- A full 10-stage structured analysis pipeline (classification → summary → clause extraction → red flags → risk scoring → obligation tracking → NER → compliance → negotiation suggestions → persistence)
- A RAG (Retrieval-Augmented Generation) chatbot to ask questions about a specific contract
- FAISS-backed semantic search across contract chunks
- Side-by-side contract comparison (diff)
- A cross-contract clause library
- An executive analytics dashboard
- PDF/DOCX report export

**Stack at a glance:**
| Layer | Technology |
|---|---|
| Backend | FastAPI + Uvicorn (Python) |
| LLM Inference | Groq API → Llama 3.3 70B |
| Embeddings | `sentence-transformers` (all-MiniLM-L6-v2) |
| Vector Store | FAISS (IndexFlatIP — cosine similarity) |
| Database | SQLAlchemy → SQLite (local) / PostgreSQL (production) |
| Doc Parsing | PyMuPDF (PDF) + python-docx (DOCX) |
| Frontend | Next.js 16 (App Router) + React 18 + TypeScript + Tailwind CSS 4 |
| State Mgmt | React Context API (`ContractContext`) |
| Charts | Recharts |
| Streaming | Server-Sent Events (SSE) for live pipeline progress |

---

## 2. Architecture Deep Dive

### 2.1 Backend (`main.py` — FastAPI)

The backend is a single FastAPI application with clearly grouped endpoints:

```
POST   /api/contracts/upload          → upload, parse, chunk, build FAISS index
GET    /api/contracts/{id}/analyze    → SSE stream: run 10-stage pipeline
GET    /api/contracts                 → list all user contracts
GET    /api/contracts/{id}            → full contract detail (with mapping to frontend shape)
DELETE /api/contracts/{id}            → delete record + files + vector store
PATCH  /api/contracts/{id}/status    → update workflow status
PATCH  /api/contracts/{id}/notes     → update reviewer notes
POST   /api/contracts/{id}/chat      → RAG Q&A (persists chat history)
GET    /api/contracts/{id}/chat/history
DELETE /api/contracts/{id}/chat/history
POST   /api/contracts/{id}/search    → FAISS semantic search
POST   /api/contracts/compare        → side-by-side diff
GET    /api/clauses/library          → get/filter clause library
POST   /api/clauses/library          → save clause manually
DELETE /api/clauses/library/{id}
GET    /api/dashboard/stats          → risk distribution, clause frequency, summary table
GET    /api/contracts/{id}/pdf       → stream PDF report
POST   /api/auth/register
POST   /api/auth/login
```

### 2.2 The 10-Stage Analysis Pipeline

The pipeline runs **sequentially** (not in parallel) for consistency — later stages depend on earlier results:

```
1. Classification      → contract_type (NDA, SaaS, Employment, etc.)
2. Summary             → narrative + key_points (Term, Payment, Liability…)
3. Clause Extraction   → exhaustive clause-by-clause list with categories
4. Red Flag Detection  → severity-ranked flags (Low/Medium/High/Critical)
5. Risk Analysis       → uses red_flags_context → composite score 0–100
6. Obligation Tracking → deadlines, recurring obligations, responsible parties
7. NER (Named Entities)→ parties, dates, amounts, jurisdictions, durations
8. Compliance Check    → GDPR / HIPAA / PCI DSS (applicability-gated)
9. Negotiation Suggest → uses red_flags_context → current vs. suggested wording
10. Persist to DB      → saves full JSON bundle; auto-populates clause library
```

> **Why run Red Flags before Risk?** So the risk score and category severities are *consistent* with identified red flags — the LLM is given the red flag output as context when computing risk, preventing contradictions.

### 2.3 RAG Pipeline (Chat)

```
User Question
    ↓
generate_single_embedding()   [all-MiniLM-L6-v2]
    ↓
FAISS IndexFlatIP.search(top_k=6)   [cosine similarity via normalized embeddings]
    ↓
Build context string with [Section N] headers + relevance scores
    ↓
citation_chat_prompt(context, question)   [Groq API → Llama 3.3]
    ↓
Answer with inline [Source N] citations
    ↓
Persist Q&A pair to contract.chat_history in DB
```

### 2.4 Vector Store

- **Index type**: `faiss.IndexFlatIP` (Inner Product — equivalent to cosine similarity when embeddings are L2-normalized)
- **Embedding model**: `all-MiniLM-L6-v2` (384-dimensional)
- **Chunking**: `RecursiveCharacterTextSplitter` — chunk_size=1000 chars, overlap=200 chars
- **Separators priority**: `["\n\n", "\n", ". ", " ", ""]`
- **Persistence**: FAISS index saved as `vector_store/{contract_id}/faiss.index` + chunks pickled as `chunks.pkl`
- **Auto-rebuild**: If the index is missing (ephemeral server restart), it automatically rebuilds from the cached raw text at `contract_texts/{contract_id}.txt`

### 2.5 LLM Client (Groq)

- **Model**: `llama-3.3-70b-versatile`
- **Temperature**: `0.2` (low for determinism in structured outputs)
- **JSON Mode**: All pipeline stages use `response_format={"type": "json_object"}`
- **Retry logic**: 3 retries with exponential backoff `[1.0s, 2.0s, 4.0s]`
- **Validation retry**: If Pydantic `model_validate_json()` fails, re-prompts once with the validation error appended

### 2.6 Database Schema

**`ContractRecord` table** (SQLAlchemy ORM):
```
id, filename, upload_time, summary(JSON Text), clauses(JSON Text),
risk_analysis(JSON Text), risk_score(Float), contract_type,
obligations(JSON Text), entities(JSON Text), red_flags(JSON Text),
compliance(JSON Text), negotiation_suggestions(JSON Text),
workflow_status, username, storage_url, chat_history(JSON Text), notes
```

**`UserRecord` table**: `id, username, password (plain-text!), role, name, initials, email`

**`ClauseRecord` table**: `id, clause_type, clause_text, source_contract, created_at`

### 2.7 Frontend Architecture (Next.js App Router)

```
src/
├── app/            # Pages: login, upload, dashboard, summary,
│                   # clause-analysis, compliance, comparison,
│                   # template-comparison, search, knowledge
├── components/     # Dashboard widgets, clause UI, layout, shared UI
├── context/
│   └── ContractContext.tsx   # Global state (contracts, auth, notifications)
├── lib/
│   └── api.ts                # All fetch calls to FastAPI backend
├── data/
│   └── mockData.ts           # Dev-time mock contracts
└── types/
    └── index.ts              # Shared TypeScript interfaces
```

**`ContractContext`** is the nerve-center:
- Holds `contracts[]`, `selectedContractId`, `role`, `userProfile`, `notifications`
- Exposes `login`, `logout`, `signup`, `refreshContracts`, `deleteContractById`, `updateContractStatus`, `updateContractClauseById`
- Auth state persisted in `localStorage` (role, username, profile)

---

## 3. Key Design Decisions & Tradeoffs

| Decision | Why | Tradeoff |
|---|---|---|
| Sequential pipeline | Ensures risk score ↔ red flags consistency | Slower than parallel; each stage is a Groq API call |
| FAISS IndexFlatIP + normalized embeddings | Cosine similarity without HNSW complexity | No approximate search; brute-force O(n) scan |
| Groq API (Llama 3.3) over OpenAI | Very fast inference, free tier, JSON mode | Rate limits on free tier can exhaust during heavy testing |
| SQLite locally, PostgreSQL in prod | Zero-config local dev | SQLite is single-writer; migrations are raw `ALTER TABLE` strings |
| SSE for pipeline progress | Live UI updates without polling | One-directional; client can't cancel mid-stream |
| all-MiniLM-L6-v2 | General-purpose, fast, 384-dim | Not legal-domain-specific; may miss legal jargon nuances |
| Plain-text passwords | Simple for prototyping | Not production-safe; needs bcrypt/JWT before public deployment |
| X-Username header for auth | Simple per-user data scoping | Not secure (no token/JWT); easily spoofed |
| `lru_cache(maxsize=1)` for model/client | Load model once per process | Model stays in memory; good for server, wasteful if only occasional use |

---

## 4. Prompt Engineering Highlights

Every pipeline stage has a dedicated prompt function in `models/prompts.py`.  Key patterns:

- **JSON-mode enforcement**: Every prompt ends with *"Respond with ONLY a JSON object matching this schema. Do not wrap in markdown code blocks. Do not add conversational text."*
- **Consistency injection**: Red flags JSON is injected into both the risk prompt and negotiation prompt to ensure the model doesn't contradict its own earlier findings.
- **Exhaustiveness instruction**: Clause extraction explicitly tells the LLM to count numbered provisions and match the count — prevents summarization instead of full extraction.
- **Applicability-gating for compliance**: GDPR/HIPAA/PCI DSS are only flagged as applicable if the contract *text itself* provides a textual basis — prevents hallucinated compliance gaps.
- **Score derivation rule for risk**: The risk score prompt gives explicit per-severity contribution weights and a sanity-check rule (Critical item → score ≥ 60) to prevent the score from being inconsistent with the items.
- **Citation-based chat**: The citation prompt explicitly requires `[Source N]` notation and a "Sources Used:" footer.

---

## 5. Likely Interview Questions & Strong Answers

### 🔹 General / Project Understanding

**Q: Can you walk me through what ContractIQ does end-to-end?**
> A user uploads a PDF/DOCX contract. The backend parses it, chunks the text (1000 chars, 200 overlap), builds a per-contract FAISS vector index, and stores raw text locally. When the user triggers analysis, a 10-stage LLM pipeline runs sequentially via the Groq API, streaming progress via SSE. Each stage returns Pydantic-validated JSON that gets persisted to SQLite/Postgres. The frontend (Next.js) polls the SSE stream and updates the UI in real time. After analysis, the user can ask questions via a RAG chat, search semantically, compare two contracts, or export a PDF report.

**Q: Why did you choose FastAPI over Flask or Django?**
> FastAPI gives automatic OpenAPI docs (`/docs`), built-in Pydantic integration for request validation, async support (needed for SSE streaming), and significantly better performance than Flask for I/O-bound workloads. Django would have been overkill for a purely API-driven backend.

**Q: What does the frontend state management look like?**
> A React Context (`ContractContext`) wraps the entire app and holds all contract data, auth state, and notifications. It uses `useEffect` to fetch contracts on mount, and exposes actions (`login`, `refreshContracts`, `updateContractClauseById`, etc.). Auth is persisted in `localStorage`. This avoids a heavier library like Redux for what is essentially a linear workflow.

---

### 🔹 LLM / AI Pipeline

**Q: How does the RAG pipeline work?**
> When a user asks a question, the question is embedded using `all-MiniLM-L6-v2`. The embedding is queried against the contract's FAISS index (top-6 chunks). Those chunks are assembled into a numbered context string with relevance scores. A prompt is built with the citation instructions and sent to Groq (Llama 3.3). The response includes inline `[Source N]` citations. Both Q and A are persisted to the contract's `chat_history` JSON column in the database.

**Q: Why does the red flag stage run before risk analysis?**
> Risk analysis uses red flags as context. If you run them independently, the LLM might assign a "Medium" risk score while also identifying a "Critical" uncapped indemnification flag — those would contradict each other. By passing the red flag JSON into the risk prompt, you force the model to derive a score consistent with what was already found.

**Q: How does JSON-mode structured output work with Groq?**
> We pass `response_format={"type": "json_object"}` to the Groq completions API. The model is constrained to output valid JSON. Then `model_class.model_validate_json(content)` parses and validates it against the Pydantic schema. If validation fails, we re-prompt once, appending the validation error so the model can self-correct.

**Q: What is the embedding model and why that specific one?**
> `all-MiniLM-L6-v2` from `sentence-transformers`. It produces 384-dimensional embeddings and is very fast to run on CPU. It's general-purpose — a known limitation is that it's not legal-domain-specific. A domain-tuned legal embedding model (e.g., `legal-bert`) would likely give better semantic retrieval for legal jargon.

**Q: How does FAISS find similar chunks?**
> We use `IndexFlatIP` (Inner Product index). Since embeddings are L2-normalized before being added (`normalize_embeddings=True`), inner product == cosine similarity. For a query, we embed it (also normalized), call `index.search(query_vector, top_k)`, and get back indices and similarity scores. It's a brute-force exact search — O(n×d) — fine for contract-scale data (typically hundreds of chunks).

**Q: How is the risk score calculated? Is it a model output or computed?**
> It's an LLM output, but the prompt constrains it via explicit rules: Critical items contribute ~20-30 pts, High ~10-18 pts, Medium ~4-8 pts, Low ~1-3 pts. A sanity-check rule is enforced in the prompt: if any Critical flag exists, the score must be ≥ 60. The `get_risk_level()` function then converts the 0-100 score to Low/Medium/High/Critical buckets.

---

### 🔹 Backend / API Design

**Q: How does Server-Sent Events (SSE) streaming work in this project?**
> The `/api/contracts/{id}/analyze` endpoint returns a `StreamingResponse` with `media_type="text/event-stream"`. The `run_pipeline()` async generator `yield`s JSON-encoded `data:` messages after each pipeline stage completes. The frontend opens an `EventSource` connection and renders progress updates in real time without polling.

**Q: How does the FAISS index auto-rebuild work?**
> In `load_vector_store()`, if the FAISS `.index` and `.pkl` files are missing (e.g., after an ephemeral server restart on Render), the code falls back to reading the raw contract text from `contract_texts/{id}.txt`, re-chunking it, and rebuilding the index. If that file is also missing, it raises a `FileNotFoundError` asking the user to re-upload.

**Q: How is user data scoped per user?**
> Every protected endpoint accepts an `X-Username` header (set automatically by the frontend after login via `localStorage`). Database queries filter by `username`. For ownership checks, the backend fetches the contract and compares its `username` field to the header — if they don't match, a 404 is returned (not a 403, to avoid information leakage).

**Q: How does contract deletion work?**
> Deleting a contract removes: (1) the DB record, (2) the FAISS `.index` + `.pkl` files via `clear_vector_store()`, (3) the raw cached text at `contract_texts/{id}.txt`, (4) the original uploaded file at `uploads/{id}.pdf`.

**Q: What's the migration strategy for the database?**
> `init_database()` runs on startup and calls `Base.metadata.create_all(engine)` (creates tables if not exists). Additional columns are added via raw `ALTER TABLE … ADD COLUMN IF NOT EXISTS` SQL strings executed in a loop — a simple but idempotent migration approach. A proper Alembic setup would be the next evolution.

---

### 🔹 Frontend / React

**Q: How does the ContractContext work?**
> It's a React Context wrapping the entire app. On mount, it calls `refreshContracts()` which fetches all contracts from the FastAPI backend. Auth state (role, username, profile) is saved to/read from `localStorage`. The context exposes `updateContractClauseById` which, in real mode, calls the backend API and then refreshes contracts; in mock mode, it updates local state directly with an audit trail entry.

**Q: How does the frontend handle the case where the backend is unavailable?**
> In `refreshContracts()`, if the fetch fails, it catches the error and sets `contracts = []` and `isUsingMockData = false` — showing an empty state rather than crashing or showing stale mock data. Previously the app fell back to `mockData.ts`, but this was removed for a cleaner empty-state UX.

**Q: What is Recharts used for?**
> Recharts renders the executive dashboard charts: risk distribution (pie/bar), clause frequency, and the contracts summary table. These pull from the `/api/dashboard/stats` endpoint which aggregates data from all user contracts using `analytics.py`.

---

### 🔹 Compliance & Legal Domain

**Q: How does the compliance checker avoid false positives?**
> The compliance prompt explicitly tells the LLM to first determine **applicability** from the contract text itself before evaluating any framework. GDPR is only applicable if the contract references EU personal data. HIPAA only if it references PHI or healthcare. PCI DSS only if it references payment card data (plain payment terms don't trigger it). If not applicable, the item is still included with `applicable: false` and `overall_status: "N/A"` — no fabricated gaps.

**Q: What clause categories does the system recognize?**
> Nine categories: `Term & Renewal`, `Payment`, `Liability`, `Indemnification`, `Confidentiality`, `Termination`, `IP & Ownership`, `Dispute Resolution`, `General`.

**Q: What are red flags in this context?**
> Problematic or risky contract provisions. The prompt scans for: uncapped/asymmetric indemnification, unilateral price changes, asymmetric termination rights, auto-renewal with short notice windows, broad third-party data-sharing carve-outs, one-sided assignment rights, non-refundable payment terms, IP ownership imbalances, and narrow liability caps. Severity is ranked Low / Medium / High / Critical.

---

### 🔹 Known Limitations (Show Self-Awareness)

**Q: What would you improve if you had more time?**
> Several areas:
> 1. **JWT-based auth** — current plain-text passwords and X-Username header are not production-safe.
> 2. **Domain-specific embeddings** — `all-MiniLM-L6-v2` is general purpose; a legal-trained model would improve RAG quality.
> 3. **Parallel pipeline stages** — stages 6–9 don't depend on each other; they could run concurrently with `asyncio.gather()` to reduce total latency.
> 4. **Alembic migrations** — replace raw SQL `ALTER TABLE` with proper versioned migrations.
> 5. **Streaming LLM responses** — currently each stage waits for the full LLM response before yielding; token-level streaming per stage would feel faster.
> 6. **Rate limit handling** — on Groq free tier, heavy usage can exhaust daily quotas; an exponential backoff + queue system would help.

---

## 6. Data Flow Diagram

```
PDF/DOCX Upload
     │
     ▼
utils/pdf_utils.extract_text()        ← PyMuPDF / python-docx
     │
     ▼
services/chunker.chunk_text()         ← RecursiveCharacterTextSplitter (1000/200)
     │            │
     ▼            ▼
services/vector_store                 contract_texts/{id}.txt
  build_vector_store()                (raw text cache)
  → IndexFlatIP + chunks.pkl
     │
     ▼
/api/contracts/{id}/analyze  (SSE)
     │
     ├─ 1. classify_contract()
     ├─ 2. summarize_contract()
     ├─ 3. extract_clauses()
     ├─ 4. detect_red_flags()
     ├─ 5. analyze_risks(red_flags_context)
     ├─ 6. extract_obligations()
     ├─ 7. extract_entities()
     ├─ 8. check_compliance()
     ├─ 9. suggest_negotiations(red_flags_context)
     └─ 10. ContractRecord saved to SQLite/Postgres
            + Clause library auto-populated
```

---

## 7. Quick-Reference Facts

| Fact | Value |
|---|---|
| LLM Model | `llama-3.3-70b-versatile` via Groq |
| Embedding Model | `all-MiniLM-L6-v2` (384-dim) |
| Chunk Size / Overlap | 1000 chars / 200 chars |
| FAISS Index Type | `IndexFlatIP` (cosine via normalized embeddings) |
| RAG top-k | 6 chunks for chat, 3 for search |
| LLM Temperature | 0.2 |
| Retry delays | 1s → 2s → 4s (exponential backoff) |
| Database (local) | SQLite at `database/contracts.db` |
| Pipeline stages | 10 (sequential, SSE-streamed) |
| Auth method | X-Username header + localStorage (no JWT) |
| Risk score range | 0–100 (Critical > 75, High > 50, Medium > 25) |
| Clause categories | 9 fixed categories |
| Compliance frameworks | GDPR, HIPAA, PCI DSS |
| Frontend framework | Next.js 16 App Router + React 18 + TypeScript |
| CSS framework | Tailwind CSS 4 |
| Report export | FPDF2 (PDF) + python-docx (DOCX) |

---

## 8. Rapid-Fire Q&A Cheat Sheet

| Question | One-liner Answer |
|---|---|
| What is FAISS? | Facebook AI Similarity Search — in-memory vector index for fast nearest-neighbor search |
| What is RAG? | Retrieval-Augmented Generation — retrieve relevant context, inject it into LLM prompt |
| What is Pydantic used for? | Schema validation of every LLM JSON output; ensures structured, type-safe responses |
| What is SSE? | Server-Sent Events — one-directional HTTP stream from server to browser for live updates |
| Why `lru_cache` on the Groq client? | Load and cache the client once per process — avoids re-initializing on every request |
| Why `normalize_embeddings=True`? | L2-normalizes vectors so `IndexFlatIP` (inner product) equals cosine similarity |
| What is `RecursiveCharacterTextSplitter`? | LangChain splitter that tries `\n\n`, `\n`, `. `, ` ` separators in order to keep semantic units intact |
| What is `model_dump_json()`? | Pydantic v2 method — serializes a Pydantic model instance to a JSON string |
| What happens on FAISS index miss? | Auto-rebuild from `contract_texts/{id}.txt`; if that's also missing → 404, re-upload required |
| How is counterparty extracted? | From NER entities: first `Organization` type entity is used as counterparty name |

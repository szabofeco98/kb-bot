# kb-bot

A retrieval-augmented (RAG) question-answering backend on top of a bank's internal knowledge base.
Bank experts ask a question, optionally filtered by product (mortgage, personal loan, bank account), and get an
answer generated **only** from the bank's own documents – together with the exact documents and sections it is
based on.

> Interview take-home project. The sample documents describe a fictional bank ("Példa Bank") in Hungarian.

## How it works

```
Ingestion:  .md upload ─► frontmatter validation ─► structure-aware chunking ─► embedding (bge-m3) ─► Chroma
Question:   question (+ optional product) ─► filtered similarity search ─► relevance threshold
            ─► LLM answers from the chunks only ─► answer + code-built citations
```

## Technologies

| Concern            | Choice                                                                    |
|--------------------|---------------------------------------------------------------------------|
| Language / runtime | Java 25                                                                   |
| Framework          | Spring Boot 4.1 (Web MVC, Bean Validation)                                |
| AI integration     | Spring AI 2.0.1 (`ChatClient`, `VectorStore`, structured output)          |
| Chat model         | Ollama – `qwen2.5:7b` (temperature 0, 16k context)                        |
| Embedding model    | Ollama – `bge-m3` (multilingual, needed for Hungarian)                    |
| Vector store       | Chroma 1.0.0 (Docker)                                                     |
| Build              | Maven (wrapper included)                                                  |
| Tests              | JUnit 5, AssertJ, Mockito, MockMvc                                        |

The chat model is selected purely by configuration (`spring.ai.model.chat`), so it can be swapped to e.g. Anthropic
Claude without code changes.

## Running locally

### Prerequisites

- **JDK 25**
- **Docker** (Docker Desktop on Windows/macOS) – runs Chroma
- **Ollama** – runs the chat and embedding models: <https://ollama.com/download>

### 1. Start Chroma

```bash
docker compose up -d
```

Chroma listens on `http://localhost:8000` and keeps its data in the `chroma-data` volume.

### 2. Start Ollama and pull the models

```bash
ollama pull qwen2.5:7b
ollama pull bge-m3
```

Check that it is running: <http://localhost:11434> should respond with `Ollama is running`.

> Ollama runs natively rather than in Docker because GPUs (AMD in particular) are not passed through to containers
> on Windows. On Linux with an NVIDIA GPU, or if CPU speed is acceptable, it can also run in a container:
> `docker run -d -p 11434:11434 -v ollama:/root/.ollama --name ollama ollama/ollama`, then
> `docker exec ollama ollama pull qwen2.5:7b` and `docker exec ollama ollama pull bge-m3`.

### 3. Start the application

```bash
./mvnw spring-boot:run        # macOS / Linux
.\mvnw spring-boot:run        # Windows PowerShell
```

The API is available at `http://localhost:8080`.

### 4. Run the tests

```bash
./mvnw test
```

The tests need neither Chroma nor Ollama (the vector store and models are mocked).

## Sending HTTP requests

The repository contains ready-made request files for the IntelliJ IDEA HTTP client (also supported by the VS Code
*REST Client* extension):

| File           | What it does                                                                               |
|----------------|--------------------------------------------------------------------------------------------|
| `samples.http` | Uploads the six sample documents. **Run this first.** The last request is intentionally invalid and must return `400`. |
| `search.http`  | Retrieval only (no LLM) – shows which chunks are found and their scores.                   |
| `ask.http`     | Full question answering with citations, including "not in any document" and invalid cases. |

In IntelliJ: open the file and click **Run All Requests in File** (or the ▶ icon next to a single request).


## API

| Method & path         | Description                                                                                       |
|-----------------------|---------------------------------------------------------------------------------------------------|
| `POST /api/documents` | Multipart upload: `file` (`.md`) + `product`. Returns `201` with `docId`, `version`, `product`, `chunkCount`. |
| `GET /api/search`     | `question` (required), `product` (optional). Returns the relevant documents and chunks with scores. |
| `POST /api/ask`       | JSON `{ "question": "...", "product": "LAKASHITEL" \| null }`. Returns `status`, `answer`, `product`, `sources`. |

- `status` is `ANSWERED` or `NO_RELEVANT_SOURCE` (`/api/search`: `SOURCES_FOUND` or `NO_RELEVANT_SOURCE`).
- Without a `product`, every product is searched separately and the results are merged.
- Invalid input returns `400` as an RFC 9457 `ProblemDetail`; an LLM failure returns `502`.

### Document format

Markdown with a flat frontmatter; `id` and `version` are required, and `product` (if present) must match the upload
parameter:

```markdown
---
id: LH-002
title: Lakáshitel – Kondíciós lista
product: LAKASHITEL
version: 2026-10
effective_from: 2026-10-01
owner: Lakossági hitelezés
---

# Lakáshitel – Kondíciós lista

## Díjak
...
```

Documents are split at `##` headings (long sections at blank lines, so tables and lists are never cut in half), and
each chunk is prefixed with a `[Title > Section]` breadcrumb. Sample documents live in `src/main/resources/documents/`.

## Configuration

`src/main/resources/application.yml` holds the model and Chroma settings. RAG tuning lives under `rag.*`
(validated at startup):

| Property                   | Default | Meaning                                         |
|----------------------------|---------|-------------------------------------------------|
| `rag.top-k`                | 5       | Chunks retrieved per product                    |
| `rag.similarity-threshold` | 0.5     | Minimum similarity for a chunk to be used       |
| `rag.chunk.max-tokens`     | 500     | Section size above which a section is split     |

Changing the embedding model invalidates the stored vectors: wipe the volume (`docker compose down -v`) or use a new
collection name, then re-upload the documents.

## Project structure

```
com.szabofeco.kb_bot
├── common/      Product, DocumentRef, metadata keys, RagProperties, error handling
├── ingestion/   POST /api/documents – frontmatter validation, Markdown chunking, versioned storage
├── search/      GET /api/search – filtered similarity search + relevance gate (shared by ask)
└── ask/         POST /api/ask – grounded generation, S1..Sn labelled context, code-built citations
```

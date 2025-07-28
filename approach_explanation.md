# Adobe Hackathon Round 1B – Approach Explanation

## Challenge Theme:
**Persona-Driven Document Intelligence** – Extracting and prioritizing document sections based on a user's persona and their job-to-be-done.

---

## Problem Understanding

In this round, the goal is to simulate a document analyst that reads multiple PDFs, understands a persona’s context and goals, and ranks the most relevant sections accordingly.

The core challenge is to build a system that generalizes across domains, document styles, and user intents — and does it offline, within tight resource and runtime constraints.

---

## Solution Overview

We designed a pipeline that works in 3 key stages:

### 1. **Text Extraction with PyMuPDF**
- We use PyMuPDF (`fitz`) to extract raw blocks of text from each PDF.
- Short/irrelevant fragments are filtered out based on length.
- For each section, we record:
  - Text content
  - Source document name
  - Page number

### 2. **Semantic Relevance Matching**
- We use `sentence-transformers` (specifically the `all-MiniLM-L6-v2` model) to encode:
  - Each extracted text block
  - The persona's job-to-be-done
- Cosine similarity scores are computed between each block and the job description.
- The top-k most relevant sections are ranked and selected as output.

This method is layout-agnostic and performs well even without explicit heading structures, making it generalizable across PDFs like research papers, textbooks, and financial reports.

---

## Why Sentence Embeddings?
- Traditional keyword search isn't enough for understanding nuanced job descriptions.
- Semantic embeddings capture contextual meaning — enabling better ranking even when terminology varies between the persona and the document.

---

## Output Structure

We generate a JSON file with:
- **Metadata** (documents, persona, job, timestamp)
- **Top extracted sections** with page numbers and rank
- **Subsection analysis** for refined review

This structure aligns with the challenge spec and allows downstream UIs to easily consume and visualize results.

---

## Constraints & Optimizations

- **Offline execution**: All models are preloaded and Dockerized.
- **Model size**: `all-MiniLM-L6-v2` is under 100MB, ideal for fast CPU inference.
- **Runtime**: Benchmarking shows <10s for 3–5 documents, well within limits.

---

## Generalizability

The pipeline is built modularly — swapping in a domain-specific embedding model or expanding the persona input is straightforward. It's designed to handle diverse document types and tasks without hardcoding rules or assumptions.

---

## Future Improvements

- Add multilingual embeddings for bonus scoring
- Use heading hierarchy from Round 1A to improve context segmentation
- Integrate topic classification for even deeper persona modeling

---

## Summary

Our system reads PDFs like a smart analyst: it understands context, ranks relevance, and outputs clear, actionable summaries — all offline, fast, and accurately.


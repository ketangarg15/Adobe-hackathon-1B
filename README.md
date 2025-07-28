# Persona-Driven Document Intelligence – Adobe Hackathon Round 1B

This repository contains our submission for **Round 1B** of the Adobe India Hackathon 2025: *"Connecting the Dots"*.

---

## Challenge Overview

You are given:
- A set of 3–10 PDF documents
- A **persona** (e.g., Analyst, Student, Researcher)
- A **job-to-be-done** (e.g., analyze financials, prep for an exam, summarize papers)

Your task is to extract and rank the **most relevant document sections** that help the persona complete their job — all offline and under strict resource constraints.

---

## Our Approach

1. **Text Extraction**  
   Using `PyMuPDF` to pull structured text from all PDFs, page by page. We exclude excessively short or long noise blocks.

2. **Semantic Relevance Matching**  
   Using `sentence-transformers` (`all-MiniLM-L6-v2`) to encode:
   - Each section of the PDF
   - The job-to-be-done
   Then we compute cosine similarity and rank the top-k sections.

3. **Structured Output**  
   Generates a clean JSON file (`challenge1b_output.json`) with:
   - Metadata
   - Ranked relevant sections
   - Refined text analysis

---

## Directory Structure
project-root/
├── pdfs_for_1b/ # Input PDFs
│ ├── report_1.pdf
│ └── report_2.pdf
├── challenge1b_output.json # Final output (auto-generated)
├── persona_document_intelligence.py
├── requirements.txt
├── Dockerfile
├── README.md
└── approach_explanation.md


---

## How to Run (Locally)

### 1. Install dependencies:
pip install -r requirements.txt

### 2. Run the script:
Edit the bottom of persona_document_intelligence.py to set:


persona = "Investment Analyst"
job_to_be_done = "Analyze revenue trends, R&D investments, and market positioning strategies"
## Then run:

python persona_document_intelligence.py
Output is saved as challenge1b_output.json.

How to Run (Using Docker)

##1. Build the image:

docker build --platform linux/amd64 -t persona-intel:v1 .

##2. Run the container:

docker run --rm \
  -v $(pwd)/pdfs_for_1b:/app/pdfs_for_1b \
  -v $(pwd):/app \
  --network none \
  persona-intel:v1

Constraints Met
Constraint	Status
Model Size	Under 100MB
Offline Execution	No internet
CPU Only	Yes
Runtime	< 60 sec

Tech Stack
Python 3.10

PyMuPDF (fitz)

sentence-transformers

Docker (Linux/amd64)

Bonus Ideas (Future Work)
Add multilingual support (e.g., Japanese)

Use 1A’s heading hierarchy to segment text more cleanly

Perform sub-topic clustering for better sub-sectioning
import os
import json
import fitz 
import pandas as pd
from datetime import datetime
from sentence_transformers import SentenceTransformer, util

model = SentenceTransformer('all-MiniLM-L6-v2')

def extract_candidate_sections(pdf_path):
    sections = []
    doc = fitz.open(pdf_path)

    for page_num in range(len(doc)):
        page = doc[page_num]
        blocks = page.get_text("blocks")
        
        for block in blocks:
            text = block[4].strip()
            if 50 < len(text) < 500: 
                sections.append({
                    "document": os.path.basename(pdf_path),
                    "page": page_num + 1,
                    "text": text
                })

    doc.close()
    return sections

def rank_sections(sections, job_description, top_k=5):
    job_embed = model.encode(job_description, convert_to_tensor=True)
    texts = [sec["text"] for sec in sections]
    text_embeds = model.encode(texts, convert_to_tensor=True)

    scores = util.pytorch_cos_sim(job_embed, text_embeds)[0]
    ranked_indices = scores.argsort(descending=True)[:top_k]

    top_sections = []
    sub_sections = []

    for rank, idx in enumerate(ranked_indices):
        section = sections[idx]
        top_sections.append({
            "document": section["document"],
            "page_number": section["page"],
            "section_title": section["text"][:80] + "...",
            "importance_rank": rank + 1
        })
        sub_sections.append({
            "document": section["document"],
            "page_number": section["page"],
            "refined_text": section["text"]
        })

    return top_sections, sub_sections

def process_documents(input_dir, persona, job_to_be_done, output_path):
    print(f"Processing PDFs from: {input_dir}")
    all_sections = []

    for filename in os.listdir(input_dir):
        if filename.endswith(".pdf"):
            pdf_path = os.path.join(input_dir, filename)
            print(f"🔍 Extracting from: {filename}")
            sections = extract_candidate_sections(pdf_path)
            all_sections.extend(sections)

    if not all_sections:
        print("⚠️ No sections extracted.")
        return

    print(f"🔎 Ranking sections for job: {job_to_be_done}")
    top_sections, sub_sections = rank_sections(all_sections, job_to_be_done)

    output = {
        "metadata": {
            "documents": list(set(sec["document"] for sec in top_sections)),
            "persona": persona,
            "job_to_be_done": job_to_be_done,
            "timestamp": datetime.now().isoformat()
        },
        "extracted_sections": top_sections,
        "sub_section_analysis": sub_sections
    }

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(output, f, indent=2, ensure_ascii=False)

    print(f"Output saved to {output_path}")

if __name__ == "__main__":
    input_folder = "pdfs_for_1b"
    persona = "Investment Analyst"
    job_to_be_done = "Analyze revenue trends, R&D investments, and market positioning strategies"
    output_file = "challenge1b_output.json"

    process_documents(input_folder, persona, job_to_be_done, output_file)
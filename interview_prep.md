# 🎯 Hindi Fake News Detection — Interview Preparation Guide

---

## 📌 One-Line Pitch
> "A multi-layered machine learning system that detects fake news in Hindi using a stacking ensemble of three NLP-based models, enhanced by social media user credibility scoring, and explained via LIME and SHAP."

---

## 🏗️ Complete Architecture Overview

```
Input (Hindi Article Text)
         │
    ┌────┴────────────────────┐
    │                         │
    ▼                         ▼
[Base Models — Text Analysis]      [User Reliability Model]
    │                                        │
    ├── Semantic (SBERT)                     ├── followers_count
    │   paraphrase-multilingual-MiniLM-L12   ├── statuses_count
    │   → RandomForestClassifier             ├── friends_count
    │   → prob_text                          ├── verified flag
    │                                        ├── name digit count
    ├── Stylistic                            ├── has_location
    │   9 linguistic features                └── RandomForestClassifier
    │   → RandomForestClassifier                 → prob_user (fake user?)
    │   → prob_style
    │
    └── TF-IDF (N-gram 1,2)
        max_features=20,000
        → LogisticRegression (balanced)
        → prob_tfidf

         │
         ▼
┌─────────────────────────────────────────────────────┐
│             STACKING / META LAYER                   │
│                                                     │
│  Website Mode:  [prob_text, prob_style, prob_tfidf] │
│  Social Mode:   [prob_text, prob_style, prob_tfidf, │
│                  prob_user]                         │
│                                                     │
│  → StandardScaler → LogisticRegression (C=0.3)     │
│  → Final: FAKE or REAL + confidence score           │
└─────────────────────────────────────────────────────┘
         │
         ▼
  Explainability Layer
    - LIME (TF-IDF model) → word-level importances
    - LIME (Semantic model) → contextual word importances  
    - SHAP TreeExplainer (Style model) → feature contributions
    - SHAP Linear (Meta model) → sub-model contributions
```

---

## 🧩 Component Deep-Dive

### 1. Semantic Model (SBERT)
- **Model**: `paraphrase-multilingual-MiniLM-L12-v2` from `sentence-transformers`
- **What it does**: Converts Hindi text into a 384-dimensional dense embedding that captures **deep semantic meaning**
- **Classifier**: `RandomForestClassifier(n_estimators=200, random_state=42)`
- **Why multilingual?** Hindi text can't be handled by English-only BERT variants; multilingual models share embedding space across 50+ languages
- **Saved as**: `text_model.pkl`, `embedding_model.pkl`

### 2. Stylistic Model
- **Input**: 9 hand-crafted linguistic features
- **Classifier**: `RandomForestClassifier(n_estimators=100, random_state=42)`

| Feature | Description |
|---|---|
| `word_count` | Total words in article |
| `sentence_count` | Number of sentences (split by '.') |
| `avg_sentence_len` | word_count / sentence_count |
| `exclamations` | Count of '!' |
| `questions` | Count of '?' |
| `punctuation` | Count of all non-word, non-space chars |
| `uppercase_ratio` | Uppercase chars / total chars |
| `type_token_ratio (TTR)` | Unique words / total words (vocabulary richness) |
| `stopword_ratio` | Hindi stopwords / total words |

- **Intuition**: Fake news often has more exclamation marks, lower TTR (repetitive language), and unusual punctuation patterns
- **Saved as**: `style_model.pkl`

### 3. TF-IDF Model
- **Vectorizer**: `TfidfVectorizer(max_features=20000, ngram_range=(1,2))`
- **Classifier**: `LogisticRegression(max_iter=1000, class_weight='balanced')`
- **Why `class_weight='balanced'`?** Handles potential class imbalance in the dataset automatically
- **What it captures**: Specific keyword patterns, bigrams like "breaking news", suspicious phrases
- **Saved as**: `tfidf_model.pkl`, `tfidf_vectorizer.pkl`

### 4. User Reliability Model
- **Trained on**: `users.csv` (real users) + `fusers.csv` (fake/bot users)
- **12 User Features**:
  - `statuses_count`, `followers_count`, `friends_count`, `favourites_count`, `listed_count`
  - `verified` (boolean → 0/1)
  - `default_profile` (boolean → 0/1)
  - `name_len`, `name_digits`, `screen_name_len`, `screen_name_digits`
  - `has_location` (boolean → 0/1)
- **Classifier**: `RandomForestClassifier(n_estimators=100, random_state=42)`
- **Output**: Probability that the user is a **fake/bot user**
- **Saved as**: `user_reliability_model.pkl`

### 5. Meta Models (Stacking)
#### Standard Meta Model (Website Mode)
- **Input**: `[prob_text, prob_style, prob_tfidf]` — 3 features
- **Pipeline**: `StandardScaler → LogisticRegression(C=0.3)`
- **C=0.3** → Stronger regularization, prevents overfitting on just 3 features
- **Saved as**: `meta_model.pkl`, `meta_scaler.pkl`

#### Social Meta Model (Social Media Mode)
- **Input**: `[prob_text, prob_style, prob_tfidf, prob_user]` — 4 features
- **Pipeline**: `StandardScaler → LogisticRegression()`
- **Saved as**: `social_meta_model.pkl`, `social_meta_scaler.pkl`

---

## 🔄 Complete Data Flow (Prediction)

```python
# Website Mode
text → lowercase → embed (SBERT) → text_prob
text → 9 features → style_prob
text → tfidf transform → tfidf_prob
[text_prob, style_prob, tfidf_prob] → scale → meta_model → FAKE/REAL + confidence

# Social Media Mode
(same above) + user_data → 12 features → user_prob
[text_prob, style_prob, tfidf_prob, user_prob] → scale → social_meta_model → FAKE/REAL
```

---

## 🎓 Explainability (XAI)

### LIME (Local Interpretable Model-agnostic Explanations)
- **Used on**: TF-IDF model & Semantic model
- **How it works**: Creates slightly perturbed versions of the input text, runs them through the model, and fits a simple linear model on the results to identify which words drove the prediction
- **Output**: List of (word, weight) pairs — positive weight = pushes towards FAKE

### SHAP (SHapley Additive exPlanations)
- **On Style model**: `TreeExplainer` (exact for tree-based models)
- **On Meta model**: Linear approximation using `coef * feature_value`
- **Output**: Attribution values for each feature — how much each sub-model/feature contributed to the final decision

---

## 📂 Training Workflow (Sequential)

```
1. create_dataset.py       → train.csv (80%) + test.csv (20%), stratified split
2. create_user_split.py    → train_users.csv + test_users.csv
3. train_models.py         → text_model, style_model, tfidf_model, embedder
4. train_user_model.py     → user_reliability_model
5. train_meta_model.py     → meta_model + meta_scaler
6. train_social_meta_model.py → social_meta_model + social_meta_scaler
7. evaluate_models.py      → Accuracy, Precision, Recall, F1, ROC-AUC, Confusion Matrix
```

---

## 🌐 Web Application (Flask)

- **Framework**: Flask (Python)
- **Endpoint**: `POST /predict`
- **Two Modes**:
  - `"website"` → text only
  - `"social_media"` → text + user profile (name, screen_name, followers_count, verified, etc.)
- **Response JSON**:
```json
{
  "prediction": 1,
  "label": "FAKE",
  "confidence": 0.87,
  "mode": "social_media",
  "model_scores": {
    "text_model": 0.72,
    "style_model": 0.61,
    "tfidf_model": 0.80,
    "user_reliability": 0.65
  }
}
```

---

## ❓ Likely Interview Questions & Strong Answers

---

### 🔵 Project Understanding

**Q1: Explain the project in simple terms.**
> "The project detects whether a Hindi news article is fake or real. It uses three different machine learning approaches — understanding the meaning of text (semantic), analyzing writing style, and finding suspicious keyword patterns. These three models vote together through a meta-model. For social media content, we also consider *who* is sharing the news — if the sharing account itself looks like a bot, that increases the fake score."

---

**Q2: Why did you use a stacking ensemble instead of just one model?**
> "Each model captures a different aspect of fake news. The semantic model understands context and meaning. The style model catches sensationalist writing patterns. The TF-IDF model identifies suspicious keyword patterns specific to Hindi fake news. No single model is best at everything — stacking lets the meta-model learn the optimal combination of their outputs."

---

**Q3: Why use Logistic Regression as the meta-model and not another Random Forest?**
> "The meta-model only receives 3 or 4 features (probabilities from base models). Logistic Regression is ideal for this because: (1) it's interpretable — its coefficients show us exactly how much each base model is trusted, (2) it generalizes well with very few features, and (3) adding a Random Forest here with only 3 inputs would add complexity without benefit and would be prone to overfitting."

---

**Q4: What is `paraphrase-multilingual-MiniLM-L12-v2` and why did you choose it?**
> "It's a Sentence-BERT (SBERT) model from HuggingFace fine-tuned on multilingual data for paraphrase detection. It generates 384-dimensional dense vector embeddings. I chose it because: (1) it supports Hindi natively, (2) it's much faster than full BERT while retaining good quality embeddings, (3) it works well for classification tasks when combined with a downstream classifier like Random Forest."

---

**Q5: What is TTR (Type-Token Ratio)? Why would it differ between fake and real news?**
> "TTR = unique_words / total_words. It measures vocabulary richness. A low TTR means many words are repeated — common in fake news that hammers the same claim repeatedly or uses very simple repetitive language. Real news tends to be more informationally dense with higher vocabulary variety."

---

**Q6: Why did you use `class_weight='balanced'` in the TF-IDF Logistic Regression?**
> "The dataset may not have a perfect 50-50 split between fake and real news. `class_weight='balanced'` automatically adjusts the weight of each class inversely proportional to its frequency, preventing the model from being biased towards the majority class."

---

**Q7: Why do you use `StandardScaler` before the meta-model?**
> "The base model outputs are all probabilities (0-1 range), so they're already on a similar scale. However, StandardScaler still helps Logistic Regression converge faster and find the right decision boundary, especially with regularization (C=0.3). It's a best practice for any linear model."

---

**Q8: Why is C=0.3 used in the meta LogisticRegression?**
> "C is the inverse of regularization strength — lower C means stronger regularization. With only 3 input features in the meta-model, there's a risk of overfitting to the training data. C=0.3 adds stronger L2 penalty to keep the model generalizable."

---

**Q9: What are your evaluation metrics and what do they mean?**
> "We use:
> - **Accuracy**: Overall correct predictions / total predictions
> - **Precision**: Of articles flagged as FAKE, how many were actually FAKE (minimize false alarms)
> - **Recall**: Of all actual FAKE articles, how many did we catch (minimize missed fakes)
> - **F1-Score**: Harmonic mean of Precision and Recall — best for imbalanced datasets
> - **ROC-AUC**: Model's ability to distinguish between classes across all thresholds
> - **Confusion Matrix**: Shows TP, TN, FP, FN breakdown"

---

**Q10: How does LIME work? How did you apply it here?**
> "LIME explains individual predictions by: (1) taking the input text, (2) creating many slightly altered versions (randomly removing words), (3) running all versions through the model, (4) fitting a simple linear model on these results. The linear model's coefficients tell us which words influenced the prediction most. I applied it to both the TF-IDF model and the SBERT semantic model. The key parameter was `num_samples` — I used 500 for TF-IDF and 200 for SBERT (since encoding is expensive)."

---

**Q11: How does SHAP work? How did you apply it here?**
> "SHAP is based on game theory's Shapley values — it measures each feature's contribution by calculating how much each feature changes the prediction when added to every possible subset. For the style model (Random Forest), I used `TreeExplainer` which is exact and efficient for tree-based models. For the meta model (Logistic Regression), I used a linear approximation: `shap_value = coefficient × feature_value`."

---

**Q12: How do you detect fake *users* (bots)?**
> "We trained a Random Forest classifier on two datasets — real Twitter users (`users.csv`) and known fake/bot users (`fusers.csv`). Features include: follower-to-friend ratio behavior, total tweet count, whether the account is verified, whether the profile has a location, whether the username/screen name contains digits (bots often have random digit suffixes), and profile completeness (default_profile flag). The model outputs a probability of the account being fake."

---

**Q13: How do you integrate user credibility with news content?**
> "The output probability from the user reliability model is used as a 4th feature in the social meta model. So the final prediction is: meta_model([text_prob, style_prob, tfidf_prob, user_prob]). This means if the user is highly likely a bot AND the content looks suspicious, the combined fake score is much higher than either alone."

---

**Q14: What is the `stratify=news_df['label']` parameter in train_test_split?**
> "Stratified split ensures the train and test sets have the same proportion of fake vs real news as the original dataset. Without this, random splitting could accidentally put most fake news in the training set, making evaluation unreliable."

---

**Q15: How did you handle Hindi-specific NLP challenges?**
> "Hindi text has unique challenges: no capitalization distinction, different script (Devanagari), and different word boundaries. I addressed them with: (1) a multilingual SBERT model trained on Hindi, (2) custom Hindi stopwords list (35 words) for TF-IDF and style features, (3) lowercasing text before processing to normalize input."

---

**Q16: What were the limitations of your approach?**
> "A few limitations:
> 1. The user credibility integration is simulated — in evaluation, user scores are sampled based on news label, not actual linked user data
> 2. The Hindi stopword list is manually curated and may miss some words
> 3. The style features are language-agnostic and don't capture Hindi-specific linguistic patterns
> 4. No real-time data ingestion or API integration"

---

**Q17: How would you improve this project?**
> "1. Use Hindi BERT (`ai4bharat/indic-bert`) instead of multilingual MiniLM for better Hindi-specific understanding
> 2. Add real news source credibility checking (URL verification)
> 3. Cross-reference with a knowledge base (Wikidata) for factual verification
> 4. Train on more data — the dataset size could impact generalizability
> 5. Build a real social media API integration instead of simulated user pairing"

---

**Q18: Why Flask for the web interface?**
> "Flask is lightweight, easy to integrate with Python ML models, and suitable for building REST APIs quickly. The `/predict` endpoint accepts JSON, calls the `FakeNewsPipeline`, and returns a structured JSON response. No complex frontend framework was needed for this demo."

---

**Q19: How does `joblib.dump` and `joblib.load` work? Why joblib over pickle?**
> "Both serialize Python objects to disk. `joblib` is preferred for NumPy arrays and scikit-learn models because it's much faster and more memory-efficient for large array serialization (uses memory-mapped files). It's the scikit-learn recommended method for model persistence."

---

**Q20: What is the difference between `fit_transform` and `transform`?**
> "- `fit_transform(X_train)`: Learns parameters (mean, std for scaler; vocabulary for TF-IDF) FROM the data AND applies the transformation  
> - `transform(X_test)`: Only applies the transformation using parameters learned from training data  
> **Critical**: You must NEVER fit on test data — that would cause data leakage and inflate performance metrics."

---

### 🔴 Potential Trick Questions

**Q: Isn't using `text.lower()` harmful for Hindi? Hindi doesn't use upper/lowercase.**
> "Correct — Devanagari script doesn't have case. The lowercasing is applied but has no effect on Hindi words. It only normalizes any English words or mixed-language content that might appear in Hindi articles."

**Q: Your style feature `sentence_count = text.split('.')` — doesn't that break on Hindi's purna viram (।)?**
> "That's a valid limitation. Hindi uses '।' as the sentence-ending punctuation, not '.'. Splitting by '.' would mostly count English-style sentences. This could be improved by also splitting on '।' for more accurate sentence counting in Hindi text."

**Q: How do you evaluate the social meta model fairly if user pairing is simulated?**
> "This is a genuine limitation. In evaluation, we randomly sample from real/fake user score distributions based on the news label. This is a controlled approximation — in a real deployment, you'd link each news post to its actual author's account data. The evaluation gives a theoretical upper bound on performance with good user data."

---

## 📋 Quick-Reference Cheat Sheet

| Component | Algorithm | Input | Output |
|-----------|-----------|-------|--------|
| Semantic Model | RandomForest (n=200) | 384-dim SBERT embedding | P(fake) |
| Style Model | RandomForest (n=100) | 9 linguistic features | P(fake) |
| TF-IDF Model | LogisticRegression | 20K TF-IDF features (unigram+bigram) | P(fake) |
| User Model | RandomForest (n=100) | 12 user profile features | P(fake user) |
| Meta Model (Website) | LogisticRegression (C=0.3) | 3 base model probs | FAKE/REAL |
| Social Meta Model | LogisticRegression | 4 probs (incl. user) | FAKE/REAL |

| XAI Method | Used On | Type |
|------------|---------|------|
| LIME | TF-IDF model | Word-level importance |
| LIME | SBERT/Semantic model | Word-level importance |
| SHAP TreeExplainer | Style model (RF) | Feature attribution |
| SHAP Linear | Meta model (LR) | Sub-model attribution |

---

## 🧰 Tech Stack Summary

| Library | Purpose |
|---------|---------|
| `sentence-transformers` | Multilingual SBERT embeddings |
| `scikit-learn` | RF, LR, TF-IDF, metrics, scaling |
| `pandas` + `numpy` | Data processing |
| `flask` | Web API |
| `lime` | Text explainability |
| `shap` | Feature attribution |
| `matplotlib` | Visualization |
| `joblib` | Model serialization |

---

> [!TIP]
> **Best way to answer "Tell me about your project"**: Start with the problem (Hindi fake news), then the solution approach (stacking ensemble), then the unique aspect (user credibility + XAI), then the result (Flask web app). Keep it to 2-3 minutes.

> [!IMPORTANT]
> Be ready to draw the architecture on a whiteboard. The key diagram is: 3 base models → probabilities → meta model → final prediction.

> [!NOTE]
> If asked about accuracy numbers and you don't remember exact values, say: "The standard meta model achieved high accuracy on the test set. The social meta model further improved performance when user data was available, demonstrating that user credibility is a meaningful signal."

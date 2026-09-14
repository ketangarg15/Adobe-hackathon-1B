# 🔍 TriForge Image Forgery Detection — Complete Notebook Breakdown

> **Notebook**: `notebook4d86e045b9 (6).ipynb`  
> **Framework**: PyTorch 2.2 + CUDA 11.8  
> **GPU**: Tesla P100-PCIE-16GB (15.9 GB VRAM)  
> **Platform**: Kaggle  
> **Total Cells**: 137

---

## 📋 Table of Contents
1. [Project Goal](#goal)
2. [Dataset & Data Loading](#dataset)
3. [Data Preprocessing & Augmentation](#preprocessing)
4. [Dataset Class (`TriForgeDataset`)](#datasetclass)
5. [Model Architecture (All 6 Components)](#architecture)
6. [Loss Functions](#loss)
7. [Tensor Shapes — The Data Flow](#tensors)
8. [Training Pipeline](#training)
9. [Checkpointing & Resuming](#checkpointing)
10. [Results & Evaluation](#results)

---

## 1. 🎯 Project Goal <a name="goal"></a>

This is a **dual-task image forgery detection** system called **TriForge** that solves two problems simultaneously:

| Task | What it does | Output |
|------|-------------|--------|
| **Detection** | Is this image forged? (binary classification) | 1 logit per image (scalar) |
| **Localization** | *Where* exactly was it forged? (segmentation) | Full-resolution mask (512×512) |

The model is called "TriForge" because it uses **three parallel branches**:
- **Spatial Branch** — What does the image visually look like?
- **Frequency Branch** — What does it look like in the FFT frequency domain?
- **Edge Branch** — What do the edge patterns reveal?

---

## 2. 📦 Dataset & Data Loading <a name="dataset"></a>

### Dataset: CASIA v2
- **Source**: `/kaggle/input/datasets/ketangarg/casia-v2/`
- **Type**: Image forgery benchmark dataset with authentic and tampered images

### CSV Structure
Each sample has 4 columns:
```
image_path | mask_path | label | has_mask
```

| Column | Meaning |
|--------|---------|
| `image_path` | Path to the RGB image |
| `mask_path` | Path to the ground truth forgery mask (or `NaN` for authentic) |
| `label` | `1` = forged, `0` = authentic |
| `has_mask` | `1` = has ground truth mask, `0` = no mask (same as label) |

### Split Statistics
| Split | Total | Authentic (0) | Forged (1) |
|-------|-------|---------------|-----------|
| **Train** | 10,091 | 5,993 (59.4%) | 4,098 (40.6%) |
| **Validation** | 2,523 | 1,498 (59.4%) | 1,025 (40.6%) |

> [!NOTE]
> `label` and `has_mask` are perfectly correlated — every forged image has a mask, no authentic image has one. This is a class-imbalanced dataset (roughly 60/40 split).

### DataLoader Configuration
```python
BATCH_SIZE = 2
NUM_WORKERS = 2
# Training: shuffle=True, drop_last=True
# Validation: shuffle=False
```

> [!IMPORTANT]
> `batch_size=2` is very small due to the large image size (512×512) and GPU VRAM constraints. Gradient accumulation is used to simulate larger effective batch sizes.

---

## 3. 🔄 Data Preprocessing & Augmentation <a name="preprocessing"></a>

Using the **Albumentations** library.

### Training Transforms
```python
train_transform = A.Compose([
    A.RandomResizedCrop(size=(512, 512), scale=(0.5, 1.0), p=1.0),
    A.HorizontalFlip(p=0.5),
    A.VerticalFlip(p=0.3),
    A.RandomRotate90(p=0.3),
    A.ColorJitter(brightness=0.4, contrast=0.4, saturation=0.4, hue=0.1, p=0.4),
    A.GaussianBlur(blur_limit=(3, 7), p=0.2),
    A.Normalize(mean=(0.485, 0.456, 0.406), std=(0.229, 0.224, 0.225)),
], additional_targets={"mask": "mask"})
```

| Augmentation | Purpose | Probability |
|-------------|---------|-------------|
| `RandomResizedCrop` | Scale/zoom variation | Always |
| `HorizontalFlip` | Mirror images | 50% |
| `VerticalFlip` | Upside-down | 30% |
| `RandomRotate90` | 90° rotations | 30% |
| `ColorJitter` | Brightness/contrast/saturation changes | 40% |
| `GaussianBlur` | Simulate slight blur | 20% |
| `Normalize` | ImageNet mean/std normalization | Always |

> [!NOTE]
> `additional_targets={"mask": "mask"}` ensures the **same spatial transform is applied to both the image AND the mask**. So if an image is flipped, its forgery mask is also flipped — they remain aligned.

### Validation Transforms (No augmentation, just resize + normalize)
```python
val_transform = A.Compose([
    A.Resize(512, 512),
    A.Normalize(mean=(0.485, 0.456, 0.406), std=(0.229, 0.224, 0.225)),
], additional_targets={"mask": "mask"})
```

### Normalization (ImageNet stats)
```
pixel_normalized = (pixel / 255 - mean) / std
mean = [0.485, 0.456, 0.406]
std  = [0.229, 0.224, 0.225]
```
This brings pixel values from `[0, 255]` → approximately `[-2.1, 2.6]`.

---

## 4. 🗂️ Dataset Class: `TriForgeDataset` <a name="datasetclass"></a>

```python
class TriForgeDataset(Dataset):
    def __getitem__(self, idx):
        # 1. Load image with OpenCV (BGR → RGB)
        # 2. Load mask as grayscale (or zeros if authentic)
        # 3. Binarize mask: (pixel > 127) → {0, 1}
        # 4. Resize mask if dimensions mismatch
        # 5. Apply Albumentations transforms (sync image+mask)
        # 6. Convert to PyTorch tensors
        
        return image, mask, label
```

### What each return value is:

| Output | Shape | Dtype | Range | Meaning |
|--------|-------|-------|-------|---------|
| `image` | `[3, 512, 512]` | float32 | ~[-2.1, 2.6] | Normalized RGB tensor |
| `mask` | `[1, 512, 512]` | float32 | {0.0, 1.0} | Binary forgery mask |
| `label` | scalar `[]` | float32 | {0.0, 1.0} | 0=authentic, 1=forged |

**Mask binarization**: `mask = (mask > 127).astype(np.uint8)` — grayscale pixels above 127 become 1, rest 0.

---

## 5. 🏗️ Model Architecture <a name="architecture"></a>

The TriForge model has **6 modules**, assembled in 3 stages:

```
INPUT IMAGE [B, 3, 512, 512]
     │
     ├──── SpatialBranch ──→ [B, 256, 256] (Swin Transformer)
     │
     ├──── FrequencyBranch → [B, 256, 256] (FFT + Projection)
     │
     └──── EdgeBranch ──────→ [B, 256, 256] (Canny + CNN + Projection)
                │
                ▼
         TriForgeFusion
      (Cross-Attention Fusion)
                │
                ▼
       fused_tokens [B, 256, 256]
          │              │
          ▼              ▼
  DetectionHead   LocalizationHead
  [B] (scalar)    [B, 1, 512, 512]
```

---

### 🔵 Module 1: `SpatialBranch`

**Purpose**: Extract rich visual features from the image using a pretrained Vision Transformer.

```python
class SpatialBranch(nn.Module):
    def __init__(self, swin_model, embed_dim=256):
        self.swin = swin_model          # Swin-Tiny pretrained
        self.projection = nn.Linear(768, embed_dim)  # 768→256

    def forward(self, x):
        features = self.swin.forward_features(x)   # [B, 16, 16, 768]
        B, H, W, C = features.shape
        tokens = features.reshape(B, H * W, C)     # [B, 256, 768]
        tokens = self.projection(tokens)            # [B, 256, 256]
        return tokens
```

**Model**: `swin_tiny_patch4_window7_224` from `timm`, with `img_size=512`

**How Swin Transformer works here**:
- Splits 512×512 image into patches (4×4 pixels each = 128×128 patches initially)
- Applies hierarchical shifted-window self-attention
- After all 4 stages, outputs a feature map of shape `[B, 16, 16, 768]`
- The 16×16 spatial grid with 768 channels is reshaped to 256 tokens of 768 dims
- Linear projection reduces each token from 768→256 dims

**Tensor flow**: `[B, 3, 512, 512]` → `[B, 16, 16, 768]` → `[B, 256, 768]` → **`[B, 256, 256]`**

---

### 🟡 Module 2: `FrequencyBranch`

**Purpose**: Detect statistical fingerprints in the image frequency domain — forgeries often disturb frequency distributions.

```python
class FrequencyBranch(nn.Module):
    def forward(self, x):
        # Step 1: RGB to grayscale (weighted sum)
        gray = 0.299 * x[:,0] + 0.587 * x[:,1] + 0.114 * x[:,2]
        # gray: [B, 512, 512]
        
        # Step 2: 2D FFT (real FFT)
        fft = torch.fft.rfft2(gray, norm="ortho")
        # fft: [B, 512, 257]  (complex tensor, rfft halves width+1)
        
        # Step 3: Magnitude spectrum
        magnitude = torch.abs(fft)
        # magnitude: [B, 512, 257]
        
        # Step 4: Log scaling (compress dynamic range)
        magnitude = torch.log1p(magnitude)
        # log1p(x) = log(1+x) — avoids log(0) issues
        
        # Step 5: Resize back to 512×512
        magnitude = F.interpolate(magnitude.unsqueeze(1),
                                  size=(512, 512), mode="bilinear")
        # magnitude: [B, 1, 512, 512] → squeeze → [B, 512, 512]
        
        # Step 6: Divide into 16×16 non-overlapping patches
        patches = magnitude.unfold(1, 16, 16).unfold(2, 16, 16)
        # patches: [B, 32, 32, 16, 16]
        
        # Step 7: Flatten to tokens
        patches = patches.contiguous().view(B, 1024, 256)
        # [B, 1024, 256]  (32×32=1024 patches, each 16×16=256 values)
        
        # Step 8: Linear projection
        tokens = self.projection(patches)  # [B, 1024, 256]
        return tokens
```

> [!NOTE]
> This branch produces **1024 tokens** but the Spatial branch produces only **256 tokens**. A `TokenReducer` is used to downsample this to 256 via `adaptive_avg_pool1d`.

**Why FFT?** Forged regions often have different compression artifacts, noise patterns, or JPEG block structures that are invisible in pixel space but clearly show up in the frequency domain.

**Tensor flow**: `[B, 3, 512, 512]` → `[B, 512, 512]` (gray) → FFT → `[B, 512, 257]` → resize → `[B, 512, 512]` → patches → `[B, 1024, 256]` → projection → **`[B, 1024, 256]`** → (TokenReducer) → **`[B, 256, 256]`**

---

### 🟢 Module 3: `EdgeBranch`

**Purpose**: Detect inconsistencies in edge patterns — forgeries often have unnatural sharp or blurry edges at manipulation boundaries.

```python
class EdgeBranch(nn.Module):
    def forward(self, x):
        # Step 1: RGB to grayscale
        gray = 0.299*x[:,0:1] + 0.587*x[:,1:2] + 0.114*x[:,2:3]
        # gray: [B, 1, 512, 512]
        
        # Step 2: Canny edge detection (from kornia)
        _, edges = self.canny(gray)
        # edges: [B, 1, 512, 512]  (binary edge map)
        
        # Step 3: CNN refinement
        edge_features = self.cnn(edges)
        # CNN: 1→32→64→1 channels (3×3 conv, BN, ReLU)
        # edge_features: [B, 1, 512, 512]
        
        # Step 4: Patch tokenization (same as FrequencyBranch)
        patches = edge_features.unfold(2, 16, 16).unfold(3, 16, 16)
        patches = patches.permute(0, 2, 3, 1, 4, 5)
        patches = patches.contiguous().view(B, 1024, 16*16)
        # [B, 1024, 256]
        
        # Step 5: Projection
        tokens = self.projection(patches)  # [B, 1024, 256]
        return tokens
```

The CNN refinement `1→32→64→1` learns **which edges are suspicious** rather than treating all edges equally.

**Tensor flow**: `[B, 3, 512, 512]` → gray `[B, 1, 512, 512]` → Canny → `[B, 1, 512, 512]` → CNN → `[B, 1, 512, 512]` → patches → `[B, 1024, 256]` → projection → **`[B, 1024, 256]`** → (TokenReducer) → **`[B, 256, 256]`**

---

### 🔴 Module 4: `TokenReducer`

Simple but important utility to align token counts:

```python
class TokenReducer(nn.Module):
    def forward(self, x):
        # x: [B, N, C]
        x = x.transpose(1, 2)          # [B, C, N]
        x = F.adaptive_avg_pool1d(x, 256)  # [B, C, 256]
        x = x.transpose(1, 2)          # [B, 256, C]
        return x
```

Applied to Frequency and Edge branches to reduce from 1024 → 256 tokens (pooling along the token dimension). This makes all 3 branches the same shape `[B, 256, 256]` for fusion.

---

### 🟠 Module 5: `TriForgeFusion`

**Purpose**: Cross-modal attention — let each branch "look at" what the other branches know.

```python
class TriForgeFusion(nn.Module):
    def forward(self, spatial, frequency, edge):
        # Each is [B, 256, 256]
        
        # Spatial attends to Frequency
        s, _ = self.spatial_attn(spatial, frequency, frequency)
        # (query=spatial, key=frequency, value=frequency)
        
        # Frequency attends to Edge
        f, _ = self.freq_attn(frequency, edge, edge)
        
        # Edge attends to Spatial
        e, _ = self.edge_attn(edge, spatial, spatial)
        
        # Residual connections + LayerNorm
        spatial   = self.norm_s(spatial + s)
        frequency = self.norm_f(frequency + f)
        edge      = self.norm_e(edge + e)
        
        # Averaging all 3 fused views
        fused = (spatial + frequency + edge) / 3.0  # [B, 256, 256]
        
        # FFN (Feed-Forward Network)
        fused = self.norm_out(fused + self.ffn(fused))
        
        return fused  # [B, 256, 256]
```

**Cross-attention pattern**: `q=A, k=B, v=B` means "A queries B" — each modality refines itself using information from another.

The FFN is `256→1024→256` (4x expansion) with GELU activation.

**Multi-head attention**: 8 heads, each 32-dimensional (256/8=32).

---

### 🔵 Module 6a: `DetectionHead`

**Purpose**: Binary classification — forged or authentic?

```python
class DetectionHead(nn.Module):
    def forward(self, x):
        # x: [B, 256, 256]
        x = x.mean(dim=1)  # Global Average Pooling → [B, 256]
        return self.classifier(x).squeeze(-1)  # [B]
        
# classifier: Linear(256→128) → GELU → Dropout(0.2) → Linear(128→1)
```

- **Global Average Pooling** over the 256 tokens: averages all spatial positions → condenses the full image into one 256-dim vector
- **Output**: A single logit per image (no sigmoid — raw score)
- **Output tensor**: `[B]` (one number per image in the batch)

---

### 🔵 Module 6b: `LocalizationHead`

**Purpose**: Pixel-wise segmentation — which pixels were forged?

```python
class LocalizationHead(nn.Module):
    def forward(self, x):
        # x: [B, 256, 256] (fused tokens)
        B, N, C = x.shape
        
        # Reshape token sequence to 2D spatial map
        x = x.transpose(1, 2).reshape(B, C, 16, 16)
        # [B, 256, 16, 16]
        
        # Progressive upsampling via transposed convolutions
        return self.decoder(x)
        # [B, 1, 512, 512]
```

**Decoder (transposed convolution upsampling)**:
```
[B, 256, 16, 16]
     │  Conv2d(256→128, 3×3) + BN + ReLU
[B, 128, 16, 16]
     │  ConvTranspose2d(128→64, 4×4, stride=2) → ×2 upsample
[B, 64, 32, 32]
     │  ConvTranspose2d(64→32, 4×4, stride=2) → ×2 upsample
[B, 32, 64, 64]
     │  ConvTranspose2d(32→16, 4×4, stride=2) → ×2 upsample
[B, 16, 128, 128]
     │  ConvTranspose2d(16→8, 4×4, stride=2) → ×2 upsample
[B, 8, 256, 256]
     │  ConvTranspose2d(8→1, 4×4, stride=2) → ×2 upsample
[B, 1, 512, 512]
```
Five rounds of 2× upsampling: 16 → 32 → 64 → 128 → 256 → **512** ✅

**Output**: Raw logits `[B, 1, 512, 512]` (no sigmoid — applied in loss function).

---

## 6. ⚡ Loss Functions <a name="loss"></a>

### DiceLoss

Measures overlap between predicted mask and ground truth mask:

```python
class DiceLoss(nn.Module):
    def forward(self, logits, targets):
        probs = torch.sigmoid(logits)     # Raw logits → [0,1] probabilities
        
        # Flatten spatial dims: [B, 1, 512, 512] → [B, 262144]
        probs   = probs.flatten(1)
        targets = targets.flatten(1)
        
        # Intersection = sum of product (per image)
        intersection = (probs * targets).sum(dim=1)   # [B]
        
        # Dice score
        dice = (2 * intersection + smooth) / (probs.sum(dim=1) + targets.sum(dim=1) + smooth)
        # smooth=1.0 prevents division by zero
        
        return 1 - dice.mean()  # Dice Loss = 1 - Dice Score
```

| Scenario | Dice Score | Dice Loss |
|----------|-----------|-----------|
| Perfect overlap | ~1.0 | ~0.0 |
| No overlap | ~0.0 | ~1.0 |
| Authentic image | Numerator≈smooth, denominator≈2·smooth | ~0.0 (correct) |

### TriForgeLoss (Combined Loss)

```python
class TriForgeLoss(nn.Module):
    def forward(self, detection_logits, localization_logits, labels, masks):
        
        # Task 1: Binary cross-entropy for classification
        detection_loss = BCEWithLogitsLoss(detection_logits, labels)
        # detection_logits: [B],  labels: [B]
        
        # Task 2: BCE for segmentation
        localization_bce = BCEWithLogitsLoss(localization_logits, masks)
        # Both [B, 1, 512, 512]
        
        # Task 3: Dice loss for segmentation
        dice_loss = DiceLoss(localization_logits, masks)
        
        # Combined segmentation loss
        localization_loss = localization_bce + dice_weight * dice_loss
        
        # Final total loss
        total_loss = detection_weight * detection_loss + 
                     localization_weight * localization_loss
        
        return total_loss, detection_loss, localization_loss
```

**Why BCE + Dice?**
- **BCE alone**: Treats every pixel independently. Works well but struggles with class imbalance (authentic images have all-zero masks → 262,144 negative pixels, 0 positives).
- **Dice alone**: Focuses on overlap but can be unstable.
- **BCE + Dice**: BCE ensures per-pixel accuracy, Dice ensures good region-level overlap. The combination is standard for medical/forgery segmentation.

**Default weights**: `detection_weight=1.0`, `localization_weight=1.0`, `dice_weight=1.0`

**BCEWithLogitsLoss formula**:
```
BCE(logit, label) = -[label·log(σ(logit)) + (1-label)·log(1-σ(logit))]
```
Where `σ` is the sigmoid function. This is numerically more stable than applying sigmoid first then BCE.

---

## 7. 📐 Tensor Shapes — Complete Data Flow <a name="tensors"></a>

```
INPUT:
  image:  [B=2, 3, 512, 512]   float32, normalized
  mask:   [B=2, 1, 512, 512]   float32, {0.0, 1.0}
  label:  [B=2]                 float32, {0.0, 1.0}

SPATIAL BRANCH:
  x → Swin-Tiny (4 stages):
    Stage 1: [B, 128, 128, 96]
    Stage 2: [B, 64, 64, 192]
    Stage 3: [B, 32, 32, 384]
    Stage 4: [B, 16, 16, 768]
  → reshape: [B, 256, 768]
  → Linear(768→256): [B, 256, 256]  ✅

FREQUENCY BRANCH:
  x → gray: [B, 512, 512]
  → rfft2: [B, 512, 257] complex
  → abs: [B, 512, 257] float
  → log1p: [B, 512, 257]
  → interpolate(→512×512): [B, 1, 512, 512]
  → unfold patches: [B, 1024, 256]
  → Linear(256→256): [B, 1024, 256]
  → TokenReducer: [B, 256, 256]  ✅

EDGE BRANCH:
  x → gray: [B, 1, 512, 512]
  → Canny: [B, 1, 512, 512] binary edges
  → CNN(1→32→64→1): [B, 1, 512, 512]
  → unfold patches: [B, 1024, 256]
  → Linear(256→256): [B, 1024, 256]
  → TokenReducer: [B, 256, 256]  ✅

FUSION (TriForgeFusion):
  spatial:   [B, 256, 256]
  frequency: [B, 256, 256]
  edge:      [B, 256, 256]
  → CrossAttention (8 heads, dim=256)
  → Average + FFN
  → fused: [B, 256, 256]  ✅

DETECTION HEAD:
  fused [B, 256, 256]
  → mean(dim=1): [B, 256]
  → Linear(256→128) + GELU + Dropout
  → Linear(128→1): [B, 1]
  → squeeze(-1): [B]   ← detection logit  ✅

LOCALIZATION HEAD:
  fused [B, 256, 256]
  → transpose+reshape: [B, 256, 16, 16]
  → Conv2d(256→128): [B, 128, 16, 16]
  → ConvTranspose ×5: [B, 1, 512, 512] ← mask logit  ✅

LOSSES:
  Detection BCE:     scalar
  Localization BCE:  scalar
  Dice Loss:         scalar
  Total Loss:        scalar (sum weighted)
```

---

## 8. 🏋️ Training Pipeline <a name="training"></a>

### Optimizer
```python
optimizer = torch.optim.AdamW(
    params=[all 6 modules combined],
    lr=1e-4,
    weight_decay=1e-4
)
```
- **AdamW**: Adam with decoupled weight decay (better regularization than Adam)
- **lr=1e-4**: Standard learning rate for fine-tuning transformers
- **weight_decay=1e-4**: L2 regularization

### Learning Rate Scheduler
```python
scheduler = CosineAnnealingLR(
    optimizer,
    T_max=50,    # Full cosine period = 50 epochs
    eta_min=1e-6 # Never goes below this
)
```
LR follows a cosine curve from `1e-4` down to `1e-6` over 50 epochs.

### Mixed Precision Training (AMP)
```python
scaler = torch.cuda.amp.GradScaler()

with torch.cuda.amp.autocast():
    # Forward pass in float16
    ...

scaler.scale(loss).backward()  # Scaled gradients
scaler.step(optimizer)          # Unscale + step
scaler.update()                 # Update scale factor
```
Speeds up training and reduces VRAM usage by running in FP16 but avoiding numerical underflow.

### Gradient Accumulation
```python
ACCUM_STEPS = 8  # Effective batch size = 2 × 8 = 16
```
With `batch_size=2` and `accum_steps=8`, gradients are accumulated for 8 mini-batches before one optimizer step. This simulates training with `batch_size=16`.

```python
# Inside training loop:
loss_for_backward = loss / accum_steps  # Normalize
scaler.scale(loss_for_backward).backward()

if (step + 1) % accum_steps == 0:
    scaler.step(optimizer)
    scaler.update()
    optimizer.zero_grad(set_to_none=True)
```

### Training Function `train_one_epoch`
Returns `(avg_loss, avg_detection_loss, avg_localization_loss)` averaged over all batches.

### Validation Function `validate`
Returns `(avg_loss, avg_detection_loss, avg_localization_loss, accuracy)`.

**Accuracy calculation**:
```python
predictions = (torch.sigmoid(detection_logits) >= 0.5).float()
correct += (predictions == labels).sum().item()
accuracy = correct / total
```

---

## 9. 💾 Checkpointing & Resuming <a name="checkpointing"></a>

### Training was done in segments:
| Segment | Epochs | Checkpoint |
|---------|--------|-----------|
| 1 | 1–10 | `triforge_epoch_10.pth` |
| 2 | 11–20 | `triforge_epoch_20.pth` |
| 3 | 21–25 | `triforge_epoch_25.pth` |
| 4 | 26–35 | `triforge_epoch_35.pth` |
| 5 | 36–50 | `triforge_epoch_50.pth` + full save |

Total: **50 epochs** of training.

### Checkpoint dictionary contains:
```python
{
    "epoch": int,
    "spatial_branch": state_dict,
    "freq_branch": state_dict,
    "edge_branch": state_dict,
    "fusion": state_dict,
    "detection_head": state_dict,
    "localization_head": state_dict,
    "optimizer": state_dict,
    "scheduler": state_dict,
    "scaler": state_dict,
    "history": list_of_dicts
}
```

### History tracked per epoch:
- `train_loss`, `train_detection_loss`, `train_localization_loss`
- `val_loss`, `val_detection_loss`, `val_localization_loss`
- `val_accuracy`, `learning_rate`

---

## 10. 📊 Results & Evaluation <a name="results"></a>

### Training History
- Combined from segments into `training_history_50_epochs.csv`
- 50 rows (one per epoch), 9 columns

### Graphs Generated:
1. `01_training_validation_loss.png` — Total loss curves
2. `02_detection_loss.png` — Detection task losses
3. `03_localization_loss.png` — Segmentation task losses
4. `04_validation_accuracy.png` — Validation accuracy over time
5. `05_learning_rate.png` — Cosine LR schedule
6. `06_detection_vs_localization.png` — Task comparison
7. `07_all_task_losses.png` — All 4 curves together

### Best Model Selection:
```python
best_loss_epoch    = history_df["val_loss"].idxmin()
best_accuracy_epoch = history_df["val_accuracy"].idxmax()
```

---

## 🗺️ Full Architecture Summary Diagram

```
                        ┌─────────────────────────┐
                        │   Image [B,3,512,512]   │
                        └─────────────────────────┘
                               │         │         │
               ┌───────────────┘         │         └───────────────┐
               ▼                         ▼                         ▼
    ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
    │  SpatialBranch   │    │ FrequencyBranch  │    │   EdgeBranch     │
    │  Swin-T + Linear │    │ FFT+log+patches  │    │ Canny+CNN+patch  │
    │  [B,256,256]     │    │ [B,1024,256]     │    │ [B,1024,256]     │
    └──────────────────┘    └──────────────────┘    └──────────────────┘
               │                         │                         │
               │             ┌───────────┘         ┌──────────────┘
               │             ▼ TokenReducer         ▼ TokenReducer
               │         [B,256,256]           [B,256,256]
               │                         │
               └─────────────┬───────────┘
                             ▼
                  ┌─────────────────────┐
                  │   TriForgeFusion    │
                  │  Cross-Attention    │
                  │  8 heads, dim=256   │
                  │  [B, 256, 256]      │
                  └─────────────────────┘
                        │         │
            ┌───────────┘         └───────────┐
            ▼                                 ▼
  ┌──────────────────┐             ┌──────────────────┐
  │  DetectionHead   │             │LocalizationHead  │
  │  GlobalAvgPool   │             │  5× Deconvolve   │
  │  MLP 256→128→1   │             │  16→512 spatial  │
  │  [B] (logit)     │             │ [B,1,512,512]    │
  └──────────────────┘             └──────────────────┘
            │                                 │
            ▼                                 ▼
   Detection Loss                   Localization Loss
   BCEWithLogits                   BCE + Dice Loss
            │                                 │
            └──────────────┬──────────────────┘
                           ▼
                    Total Loss (scalar)
                    → backward() → AdamW
```

---

## 🔑 Key Design Choices Summary

| Choice | Why |
|--------|-----|
| 3 parallel branches | Each modality catches different forgery types |
| Swin Transformer | Hierarchical attention, great for high-res images |
| FFT branch | Reveals JPEG artifacts, noise inconsistencies |
| Canny edge branch | Catches splicing boundaries, unnatural edges |
| Cross-attention fusion | Each branch benefits from other branches' info |
| Dual output (classify + segment) | Multi-task learning improves both tasks |
| BCE + Dice | Handles class imbalance + optimizes region overlap |
| AMP + Grad Accumulation | Fit large model in 16GB VRAM |
| Cosine LR schedule | Smooth convergence over 50 epochs |

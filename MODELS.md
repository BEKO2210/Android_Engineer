# Model Catalog

## Quantization Standard

All models listed use **Q4_K_M** quantization (GGUF format) unless noted otherwise.
Q4_K_M provides the best balance of quality, size, and performance for mobile devices.

---

## Tier 1: Tiny — Broad Devices (4GB+ RAM)

### SmolLM2 1.7B
- **Size (Q4_K_M):** ~1.1 GB
- **Speed:** 12-15 tok/s on Snapdragon 750G
- **License:** Apache 2.0
- **Device class:** Budget to mid-range (4GB+ RAM)
- **Use case:** Quick responses, simple tasks, constrained devices
- **Strengths:** Small footprint, permissive license, good for first-time users
- **Risks:** Lower quality on complex reasoning tasks
- **MVP Status:** Included

### Llama 3.2 1B
- **Size (Q4_K_M):** ~0.6 GB
- **Speed:** 15-20 tok/s on Snapdragon 750G
- **License:** Meta Community License (requires review for commercial use)
- **Device class:** Budget (4GB+ RAM)
- **Use case:** Fastest responses, most constrained devices
- **Strengths:** Smallest model, fastest inference
- **Risks:** Meta license restrictions, limited capability at 1B parameters
- **MVP Status:** Included (license review pending)

---

## Tier 2: Balanced — Everyday Use (6GB+ RAM)

### Qwen 2.5 3B
- **Size (Q4_K_M):** ~1.9 GB
- **Speed:** 8-10 tok/s on Snapdragon 8 Gen 2
- **License:** Apache 2.0
- **Device class:** Mid-range to flagship (6GB+ RAM)
- **Use case:** General conversation, reasoning, everyday tasks
- **Strengths:** Strong quality for its size, permissive license, good multilingual support
- **Risks:** Requires 6GB+ free RAM for comfortable operation
- **MVP Status:** Included

### Gemma 2B
- **Size (Q4_K_M):** ~1.6 GB
- **Speed:** 10-15 tok/s on Snapdragon 8 Gen 2
- **License:** Gemma Terms of Use (permissive but custom)
- **Device class:** Mid-range (6GB+ RAM)
- **Use case:** General tasks, Google ecosystem alignment
- **Strengths:** Well-optimized, good quality/size ratio
- **Risks:** Custom license terms require review
- **MVP Status:** Deferred to post-MVP (Qwen 2.5 3B preferred for Apache 2.0 license)

---

## Tier 3: Heavy — Flagship Devices (8GB+ RAM)

### Phi-3 Mini 3.8B
- **Size (Q4_K_M):** ~2.3 GB
- **Speed:** 5-8 tok/s on Snapdragon 8 Gen 2
- **License:** MIT
- **Device class:** Flagship (8GB+ RAM)
- **Use case:** Complex reasoning, code generation, technical tasks
- **Strengths:** Strong capability, MIT license, good for technical users
- **Risks:** Slower inference, higher RAM requirement, may thermal throttle on sustained use
- **MVP Status:** Included

### Llama 3.2 3B
- **Size (Q4_K_M):** ~1.9 GB
- **Speed:** 8-10 tok/s on Snapdragon 8 Gen 2
- **License:** Meta Community License
- **Device class:** Flagship (8GB+ RAM)
- **Use case:** General chat, reasoning
- **Strengths:** Good balance of quality and speed
- **Risks:** Meta license restrictions
- **MVP Status:** Deferred (Phi-3 Mini preferred for MIT license)

---

## MVP Model Set

| Model | Tier | Size | License | Priority |
|---|---|---|---|---|
| SmolLM2 1.7B | Tiny | 1.1 GB | Apache 2.0 | P0 |
| Qwen 2.5 3B | Balanced | 1.9 GB | Apache 2.0 | P0 |
| Phi-3 Mini 3.8B | Heavy | 2.3 GB | MIT | P0 |
| Llama 3.2 1B | Tiny | 0.6 GB | Meta CL | P1 |

---

## RAM Heuristics

| Device RAM | Recommended Tier | Max Model Size |
|---|---|---|
| 4 GB | Tiny only | ~1.1 GB |
| 6 GB | Tiny + Balanced | ~1.9 GB |
| 8 GB+ | All tiers | ~2.3 GB |

Rule of thumb: Model requires ~1.3x its file size in available RAM for comfortable inference.

---

## Download Sources

All models are downloaded from Hugging Face using direct URLs:
```
https://huggingface.co/{owner}/{repo}/resolve/main/{filename}
```

No authentication required for public models. Downloads support HTTP Range headers for resume.

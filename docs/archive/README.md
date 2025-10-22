# Archived Documentation

**Last Updated:** 2025-10-22

## What's in This Directory

This directory contains **historical documentation** from earlier phases of the project when features were planned or partially implemented. These documents describe systems that were:

- Not fully implemented
- Simulated/mocked rather than real
- Aspirational rather than actual
- Replaced by more honest documentation

## Why Archive These?

The project is pivoting to:
1. **Honesty first** - Only document what actually works
2. **Real implementations** - No more simulation demos pretending to be real
3. **Clear roadmap** - Planned features go in ROADMAP.md, not mixed with working code

These documents are preserved for historical reference but should **not** be used as current documentation.

## Current Documentation

For accurate, up-to-date documentation, see:

- **[README.md](../../README.md)** - Project overview with honest status
- **[STATUS.md](../../STATUS.md)** - What's working vs what's planned
- **[ROADMAP.md](../../ROADMAP.md)** - Future plans and features
- **[CONCEPTS.md](../../CONCEPTS.md)** - Core concepts (still valid)
- **[STORAGE_ARCHITECTURE.md](../../STORAGE_ARCHITECTURE.md)** - Storage design (still valid)

## Archived Files

| File | What It Described | Why Archived |
|------|-------------------|--------------|
| `FLINK_AGENTS_INTEGRATION.md` | Apache Flink Agents integration | Integration not actually working; Flink Agents still pre-release |
| `INTEGRATION_SUCCESS.md` | Integration "success" report | Overstated success; adapters exist but not tested end-to-end |
| `DEMO_COMPLETE.md` | Interactive demo completion | Demo is simulation, not real implementation |
| `DEMO_GUIDE.md` | Guide to interactive demo | Demo is visualization only |
| `DEMO_QUICK_REF.md` | Quick reference for demo | Demo not representative of real system |
| `CURRENT_STATUS.md` | Old status document | Replaced by consolidated STATUS.md |
| `PRODUCTION_INTEGRATION_STATUS.md` | "Production" status | Overly optimistic; replaced by STATUS.md |
| `DELIVERY_SUMMARY.md` | Delivery report | Historical milestone document |
| `COMPLETE_SESSION_SUMMARY.md` | Session summary | Historical development notes |
| `SESSION_DELIVERY.md` | Session delivery notes | Historical |
| `TEST_SUITE_DELIVERY.md` | Test suite description | No actual test suite exists yet |
| `PLUGGABLE_STORAGE_STATUS.md` | Storage status | Merged into STATUS.md |
| `PHASE1_LANGCHAIN_INTEGRATION.md` | LangChain4J phase 1 | Unclear what was actually completed |

## What To Use Instead

### Want to know what works?
→ See [STATUS.md](../../STATUS.md)

### Want to know what's planned?
→ See [ROADMAP.md](../../ROADMAP.md)

### Want to get started?
→ See [README.md](../../README.md)

### Want to understand concepts?
→ See [CONCEPTS.md](../../CONCEPTS.md) (still accurate)

### Want to learn about storage?
→ See [STORAGE_ARCHITECTURE.md](../../STORAGE_ARCHITECTURE.md) (still accurate)

## Learning from History

**What these documents teach us:**

1. **Don't document aspirations as reality** - Clear separation between working and planned
2. **Tests matter** - Can't claim "delivery" without tests
3. **Simulations aren't implementations** - Pretty demos ≠ working systems
4. **Dependencies matter** - Building on unreleased frameworks is risky
5. **Honesty is better** - Users respect truth over hype

## Using These Documents

Feel free to read these for:
- Understanding project history
- Learning from what didn't work
- Context on architectural decisions
- Examples of what NOT to do in documentation

**But remember:** These do not represent the current state of the project.

---

**Current Project Status:** See [STATUS.md](../../STATUS.md)
**Last Archive Date:** 2025-10-22

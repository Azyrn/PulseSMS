## Pulse Sub-Agent Specialized Mandates

### 1. Security & Protocol Agent (The Cryptographer)
- Protocol: Implement a post-quantum resistant layer using PQXDH and Double Ratchet.
- Fallback Strategy: If the environment does not support PQXDH, implement an automatic fallback to X3DH while maintaining the same interface contract to avoid UI breakage.
- Storage: Secure offline keys using Android Keystore with hardware-backed security where available.
- Identity: Implement business-verified contact flows and 10DLC compliance gates.
- Phase 1 Pre-mortem Focus:
- Validate that the contract surface can support PQXDH and X3DH behind the same repository/service boundary.
- Flag latency, device capability, key-rotation, and storage risks before code is written.

### 2. UI/UX & Design System Agent (M3E Specialist)
- Mandate: Deliver a premium aesthetic using Material 3 Expressive patterns and a `MaterialExpressiveTheme` direction.
- Default Skill Activation: Treat the repo design spec at `docs/skills/jetpack-compose-design.md` as active for any Android UI, screen, flow, or Composable design task in this repository.
- Design Standard: Generic Compose output is a failure state. Every screen must commit to one clear visual point-of-view and follow the repo design spec.
- Performance Budgets:
- Critical UI paths must maintain zero unnecessary recompositions using stable keys and `derivedStateOf` where justified.
- Composable hierarchy depth must stay intentionally flat to minimize measurement overhead.
- Tactics:
- Use expressive motion for message-send moments and standard motion for list scrolling.
- Use typography contrast and variable font axes to reinforce business priority and conversation state.
- Phase 1 Pre-mortem Focus:
- Validate that messaging state contracts support expressive UI without leaking protocol or sync internals into composables.
- Flag state-shape risks that would force deep recomposition or unstable list models.

### 3. Platform & Systems Agent (The Optimization Engineer)
- Performance Veto: Reject code exceeding these budgets: cold start under 1.0 second and fewer than 5% slow frames during 60 FPS scrolling.
- Backpressure & Persistence:
- Implement rate limiting and exponential backoff in the WorkManager sync queue for high-volume message bursts.
- Schema Evolution:
- Every Room entity must support versioned migrations with backward compatibility guarantees.
- State Ownership:
- Enforce the flow `UI -> Intent -> ViewModel -> Encrypt -> Local DB -> Sync -> Network`.
- Phase 1 Pre-mortem Focus:
- Validate that the contracts preserve unidirectional state ownership and can absorb Room migrations, sync retries, and telemetry without breaking callers.
- Flag interfaces that could create allocation churn, backpressure collapse, or migration dead ends.

## Pre-mortem Output Contract

Each agent must review `CONTRACTS.md` and respond with:
- `Verdict`: `approve`, `approve_with_conditions`, or `block`
- `Risks`: concrete implementation or integration failures likely under the current contract
- `Required Changes`: contract changes needed before Phase 2
- `Open Questions`: unknowns that should be resolved during planning

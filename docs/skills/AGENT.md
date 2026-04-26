# AGENT.md — Jetpack Compose UI Subagent Team

## Team Identity

This is a **specialized Jetpack Compose UI engineering team**. Every agent in this team operates under a shared mandate: produce production-grade, visually distinctive, adaptive Android UI using Kotlin and Jetpack Compose. Generic output is a team-wide failure state — not an individual agent failure.

The team is composed of three specialized subagents. Each owns a distinct domain. Each enforces its own non-negotiable standards. No agent operates in isolation — they form a dependency chain. Violating the chain order is an execution error.

---

## Execution Chain — Mandatory Order

```
[1] compose-design  →  [2] compose-layout  →  [3] compose-animate
```

**This order is non-negotiable.**

- `compose-design` establishes the aesthetic foundation — color, typography, shape, component vocabulary. It runs FIRST on every task, without exception.
- `compose-layout` applies spatial structure — spacing system, rhythm, adaptive breakpoints, hierarchy. It runs AFTER design context is locked.
- `compose-animate` layers motion — entrance choreography, micro-interactions, state transitions. It runs LAST, after layout is structurally sound.

No agent skips its predecessor. If `compose-layout` is invoked and `compose-design` has not run, `compose-layout` halts and invokes `compose-design` first. Same rule applies to `compose-animate`.

---

## Shared Non-Negotiables — All Agents

These rules apply to every agent on this team without exception. No agent overrides them. No user instruction overrides them.

**Code Quality:**
- Every reusable Composable exposes a `Modifier` parameter defaulting to `Modifier`. No exceptions.
- All color references flow through `MaterialTheme.colorScheme` tokens. Hardcoded color literals in leaf Composables are a build failure.
- All spacing values are drawn from a defined token scale (multiples of 4dp). Arbitrary `dp` literals are rejected.
- `LazyColumn` / `LazyRow` are mandatory for any list exceeding 4 items.
- Edge-to-edge support via `WindowInsets`, `imePadding()`, `systemBarsPadding()` is baseline — not optional.
- `@Preview` with both light and dark variants is mandatory for every screen-level Composable.
- Material 3 defaults are the minimum viable floor. They are never the destination.

**Accessibility:**
- `LocalReduceMotionEnabled` and system `Animation Scale` settings must be respected in all animation work.
- All interactive Composables meet the 48dp minimum touch target.
- RTL layout mirroring via `LocalLayoutDirection` is verified — never hardcode `left` / `right` directional values.
- Dynamic color via `dynamicColorScheme` is implemented on API 31+, with a custom scheme fallback.

**Performance:**
- Animate only `alpha` and `graphicsLayer` properties. Layout-triggering property animations are engineering violations unless routed through the correct Compose animation APIs.
- 60fps on mid-range hardware (Snapdragon 6xx class) is the performance floor. Jank is a regression, not a known limitation.
- `derivedStateOf` gates expensive scroll-driven recompositions. Unconstrained scroll listeners are rejected.

**Absolute Prohibitions — Team-Wide:**
- Plain `Surface` + `Text` + `Button` with zero design intent.
- Identical card grid patterns repeated across screens.
- Bounce or elastic easing in any animation, anywhere.
- `BoxWithConstraints` as a substitute for `WindowSizeClass`-driven adaptive logic.
- Arbitrary `zIndex` float literals (99f, 999f).
- `Column` inside `verticalScroll` for long content lists.
- Nesting `Card` inside `Card`.
- `Modifier.padding` applied after `clickable` when a full-size touch target is the intent.
- `GridCells.Fixed` without a product-specified rationale.
- `@OptIn` on stable APIs — if it requires an opt-in, verify it is still experimental before shipping.

---

## Agent Roster

---

### Agent 1 — `compose-design`

**Skill file:** `SKILL_compose.md`
**Role:** Foundation. Aesthetic direction. Theme system. Component vocabulary.
**Runs:** First — always.

**Owns:**
- Aesthetic direction selection and commitment (one direction, zero compromise)
- `ColorScheme` definition — `lightColorScheme` / `darkColorScheme`, every token intentional
- `Typography` customization — display `FontFamily`, body `FontFamily`, `letterSpacing`, `lineHeight`, `fontWeight`
- `Shapes` scale — `extraSmall` through `extraLarge`, selected per aesthetic direction
- M3 Expressive adoption — `titleLargeEmphasized`, physics-based `MotionScheme.expressive()`, shape morphing where appropriate
- `Canvas` background system — gradient meshes, organic shapes, geometric patterns, noise overlays
- Component vocabulary — `Surface`, `ElevatedCard`, `OutlinedCard`, `FilledTonalButton`, `FilterChip`, `Scaffold` customization
- `graphicsLayer` depth and layering strategy

**Handoff to `compose-layout`:** Locked aesthetic direction, defined `ColorScheme`, `Typography`, `Shapes`, component vocabulary, and `Canvas` background approach. Layout agent receives this context before touching any spatial decision.

**Failure states this agent prevents:** Generic Material 3 purple defaults. Roboto headlines. Flat white surfaces. Timid, evenly distributed palettes. Identical aesthetic output across separate generation runs.

---

### Agent 2 — `compose-layout`

**Skill file:** `SKILL_compose_layout.md`
**Role:** Spatial structure. Rhythm. Hierarchy. Adaptive breakpoints.
**Runs:** Second — after `compose-design` has locked context.

**Owns:**
- Spacing token object definition — all `padding`, `Arrangement.spacedBy`, `Spacer` values derived from this scale
- Spacing ownership rules — parent owns child gap via `Modifier.padding`; siblings use `Arrangement.spacedBy` or `Spacer`
- Modifier chain order — `padding` before `clickable` vs `clickable` before `padding` is a deliberate, documented decision
- `contentPadding` on lazy containers — never outer `padding` on lazy containers (clips scroll content)
- Layout composable selection — `Column`/`Row` for 1D, `LazyVerticalGrid`/`LazyHorizontalGrid` for 2D, `FlowRow`/`ContextualFlowRow` for dynamic wrapping, custom `Layout` when standard containers are insufficient
- `SubcomposeLayout` — only when a child's composition depends on another child's measured size
- Grid monotony elimination — `GridItemSpan` variation, full-width header items, mixed span patterns
- Visual hierarchy via space — squint test, reading flow, proximity/separation grouping
- Depth and elevation — semantic `zIndex` constant scale, `CardDefaults.cardElevation` semantic parameters, `Modifier.shadow` scale
- Adaptive layout — `WindowSizeClass` via `currentWindowAdaptiveInfo()`, `NavigationSuiteScaffold`, `ListDetailPaneScaffold`, `SupportingPaneScaffold`, foldable posture handling

**Handoff to `compose-animate`:** Structurally sound, spatially rhythmic, adaptive layout. Motion agent receives stable, profiled layout before adding any animation layer.

**Failure states this agent prevents:** Arbitrary `dp` values. Identical spacing everywhere. `Column` inside `verticalScroll`. Ignored `WindowSizeClass`. Nested cards. Centered-everything default. Phantom touch targets from wrong modifier chain order.

---

### Agent 3 — `compose-animate`

**Skill file:** `SKILL_compose_animate.md`
**Role:** Motion. Micro-interactions. State transitions. Delight.
**Runs:** Last — after layout is structurally complete and profiled.

**Owns:**
- Animation API selection — the correct API for every use case is non-negotiable:
  - Single value → `animate*AsState`
  - Multiple synchronized values → `updateTransition`
  - Infinite/ambient → `rememberInfiniteTransition`
  - Coroutine-controlled sequences → `Animatable`
  - Composable enter/exit → `AnimatedVisibility` with explicit specs
  - Content swap → `AnimatedContent` with `transitionSpec`
  - Size change → `animateContentSize`
  - Shared UI element → `SharedTransitionLayout` + `Modifier.sharedElement()` / `Modifier.sharedBounds()`
  - List item insert/remove/reorder → `Modifier.animateItem()`
- Animation strategy — hero moment first, feedback layer second, transition layer third, delight layer last
- Screen entry choreography — one orchestrated `LaunchedEffect` sequence with staggered delays beats scattered micro-animations
- Timing and easing — `spring()` with explicit `dampingRatio` and `stiffness` as the default; `tween()` with deliberate easing for duration-critical sequences; `keyframes` for multi-stage; `snap()` only for intentional instant transitions
- Exit duration — ~75% of enter duration. Exits that overstay degrade perceived performance.
- Shared element key integrity — mismatched keys compile silently and produce no animation. Keys are verified on both ends before shipping.
- Performance — render-thread animation via `graphicsLayer` only; `AnimationSpec` objects hoisted to `remember` or constants; `derivedStateOf` for scroll-driven state; profile on mid-range hardware before shipping
- Accessibility — `LocalReduceMotionEnabled` respected; `Animation Scale 0x` produces fully functional, unbroken UI

**Failure states this agent prevents:** Bounce or elastic easing. Scattered micro-animations with no hero moment. Layout-triggering property animations. Unrespected `Animation Scale`. Shared element key mismatches. Animation fatigue from animating every list item on scroll entry. Blocking user interaction during non-critical animations.

---

## Task Routing — Orchestrator Protocol

When a UI task arrives, the orchestrator follows this routing protocol:

| Task Type | Agents Invoked | Order |
|---|---|---|
| New screen / feature from scratch | All three | design → layout → animate |
| Existing screen looks generic / bland | `compose-design` first, then reassess | design → layout → animate |
| Layout feels off / spacing broken | `compose-design` (context check) → `compose-layout` | design → layout |
| Add animation to existing screen | `compose-design` (context check) → `compose-animate` | design → animate |
| Performance regression in animation | `compose-animate` only | animate |
| Adaptive layout failure on tablet/foldable | `compose-layout` only | layout |
| Full design system refresh | All three, full pass | design → layout → animate |

**Context check:** Before any agent runs except `compose-design`, verify design context is established. If it is not, invoke `compose-design` first regardless of the stated task.

---

## Inter-Agent Communication Protocol

Each agent produces a **handoff summary** before passing control to the next agent. The handoff summary is not a report — it is a structured context block the next agent reads before starting.

**`compose-design` handoff contains:**
- Chosen aesthetic direction (name + 1 sentence rationale)
- `ColorScheme` key decisions (primary, secondary, tertiary hex + intent)
- `FontFamily` selections (display + body, source)
- `Shapes` scale decisions
- `Canvas` background approach
- Component vocabulary overrides from M3 defaults

**`compose-layout` handoff contains:**
- Spacing token scale (all named values)
- Layout composable decisions per screen section
- Adaptive breakpoint behavior per `WindowWidthSizeClass`
- Elevation and `zIndex` semantic scales
- Any layout constraints that affect animation possibilities (e.g., `animateContentSize` already applied)

**`compose-animate` produces no handoff** — it is the terminal agent. It produces a **verification report** instead: animation strategy executed, APIs used per interaction, performance profile results, accessibility compliance confirmation.

---

## Quality Gates — Shared Verification Checklist

Every task is complete only when all of the following pass. No exceptions. No partial completions.

**Design gate (`compose-design`):**
- [ ] Aesthetic direction named and committed — not described as "modern" or "clean" (meaningless)
- [ ] Custom `FontFamily` in use for display roles — Roboto is not present in headlines
- [ ] Every `ColorScheme` token is a deliberate decision — no purple baseline defaults
- [ ] `Canvas` background or visual depth strategy is defined
- [ ] The ONE memorable thing about this screen can be named

**Layout gate (`compose-layout`):**
- [ ] Spacing token object defined — zero arbitrary `dp` literals
- [ ] Squint test passes — hierarchy is legible with blurred vision
- [ ] Modifier chain order documented for every interactive Composable
- [ ] `contentPadding` used on all lazy containers
- [ ] Adaptive behavior verified on Compact, Medium, and Expanded emulators
- [ ] RTL mirroring verified
- [ ] All touch targets ≥ 48dp

**Animation gate (`compose-animate`):**
- [ ] Hero moment identified and implemented
- [ ] Correct API used for every animation — no mismatched API choices
- [ ] Shared element keys verified to match on both ends
- [ ] 60fps confirmed on mid-range hardware profile
- [ ] `Animation Scale 0x` — UI is fully functional, no broken layouts
- [ ] No bounce or elastic easing anywhere in the codebase
- [ ] Every animation justified — decoration alone is not justification

---

## Failure Protocol

If any agent produces output that violates shared non-negotiables or its own quality gate:

1. The violating output is **rejected** — not revised in-place
2. The agent re-executes from its own starting point
3. The handoff is **not passed** to the next agent until the gate passes
4. Repeated violations (2+) on the same task trigger an escalation to the orchestrator for task re-scoping

Partial compliance is not compliance. A screen that passes 9 out of 10 gate checks has failed.

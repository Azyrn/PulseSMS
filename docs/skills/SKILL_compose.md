---
name: jetpack-compose-design
description: Create distinctive, production-grade Jetpack Compose UI with high design quality. Use this skill when the user asks to build Composables, screens, or Android applications. Generates creative, polished Kotlin/Compose implementations that eliminate generic AI aesthetics. Targets Material 3 / Material You with Material 3 Expressive patterns.
---

This skill governs the creation of distinctive, production-grade Jetpack Compose interfaces. Generic output is a failure state. Every screen produced under this skill must demonstrate deliberate, non-negotiable aesthetic and engineering intent.

The user provides UI requirements: a Composable, screen, flow, or full feature. They may supply context on purpose, audience, or technical constraints.

## Design Thinking

Before writing a single line, lock in the context and commit to a BOLD aesthetic direction. Ambiguity is not an excuse for generic defaults.

- **Purpose**: What problem does this screen solve? Who operates it?
- **Tone**: Commit to an extreme. Brutally minimal. Maximalist expressive. Retro-futuristic. Organic/natural. Luxury/refined. Playful/toy-like. Editorial. Brutalist/raw. Art deco/geometric. Soft/pastel. Industrial/utilitarian. These are starting points — execute a direction that is true to the context, not copied from a template.
- **Constraints**: Minimum SDK, Compose BOM version, navigation library, theming system. Know them before designing.
- **Differentiation**: Identify the ONE thing a user will remember about this screen. If you cannot name it, the design is not done.

**NON-NEGOTIABLE**: Commit to a single, clear conceptual direction and execute it with zero compromise. Bold expressiveness and refined minimalism are both valid — the failure mode is indecision producing mediocre middle-ground output.

The resulting implementation must be:
- Production-grade and functional
- Visually striking and immediately memorable
- Cohesive with a singular, unambiguous aesthetic point-of-view
- Meticulously refined across every dimension: spacing, shape, motion, color, typography

---

## Compose Aesthetics — Engineering Directives

### Typography
- `MaterialTheme.typography` is the floor, not the ceiling. It MUST be customized.
- Select characterful display `FontFamily` choices via Google Fonts integration or bundled assets. Roboto as a headline font is a failure.
- Pair a distinctive display font with a refined body font — these must be visually complementary yet distinct.
- `letterSpacing`, `lineHeight`, and `fontWeight` are design instruments. Use them with intention. Default values signal no design thought.

### Color & Theme
- Define a complete, intentional `ColorScheme` using `lightColorScheme` / `darkColorScheme`. Every color token — `primary`, `secondary`, `tertiary`, `surface`, `background`, `onX` variants — must be a deliberate decision.
- Dominant colors with sharp accent colors. Timid, evenly-distributed palettes are categorically rejected.
- All color references in leaf Composables flow through `MaterialTheme.colorScheme` tokens. No exceptions.
- For Material You: implement `dynamicColorScheme` on API 31+, fall back to the custom scheme.
- Semantic color extensions on `ColorScheme` are encouraged when the design vocabulary demands it.

### Motion & Animation
- State transitions are mandatory design moments. `AnimatedVisibility`, `animateContentSize`, `Crossfade`, `AnimatedContent` — pick the right tool per transition.
- Smooth property animations via `animate*AsState` are baseline. Ambient/atmospheric animations via `rememberInfiniteTransition` are used where the aesthetic demands life.
- Screen entry is a high-impact moment. One well-orchestrated entry sequence with staggered delays produces more design value than scattered, unconsidered micro-interactions.
- `spring()` with explicit `dampingRatio` and `stiffness` values is the default. Linear `tween` defaults signal absence of thought.

### Shape & Spatial Composition
- Shape is a design instrument. `RoundedCornerShape`, `CutCornerShape`, and custom `GenericShape` are selected per the aesthetic direction — not defaulted.
- Break rectangular grids. `offset`, `rotate`, `scale` modifiers create spatial tension and memorability.
- Spacing follows a disciplined scale. `Arrangement.spacedBy` and explicit padding tokens are mandatory.
- Negative space is a deliberate choice: generous in minimal designs, controlled density in expressive ones.
- Overlapping elements via `Box` with explicit `Alignment` is preferred over reflexive vertical stacking.

### Backgrounds & Visual Depth
- Flat background fills are rejected. Every background must create atmosphere and depth appropriate to the aesthetic.
- `Canvas` Composable is the primary instrument for: gradient meshes, geometric patterns, organic shapes, noise textures, decorative overlays.
- Linear, radial, and sweep gradient brushes on the `background()` modifier are standard tools.
- Glow, shadow, and bloom effects are achieved via `drawBehind` with blur masking.
- `graphicsLayer` enables layered transparencies, rotation-based depth, and elevation-driven shadow control.

### Component Patterns
- `Surface` with custom `shape` and `color` is preferred over raw `Box` for any card-like element.
- `ElevatedCard`, `OutlinedCard` are starting points. They must be customized to serve the aesthetic — never used as defaults.
- Generic `Button` is replaced with `FilledTonalButton`, `OutlinedButton`, or fully custom Composables when the design demands it.
- Pills and chips use `FilterChip`, `AssistChip`, or custom pill shapes — selected by design intent.
- `Scaffold` receives custom `containerColor` and `contentColor`. Top app bars are chosen — `CenterAlignedTopAppBar`, `LargeTopAppBar` — based on the information architecture, not convenience.

---

## Code Quality — Non-Negotiable Engineering Standards

- Every reusable Composable exposes a `Modifier` parameter defaulting to `Modifier`. No exceptions.
- `remember` / `rememberSaveable` usage must be correct and deliberate.
- `LocalContext`, `LocalDensity`, `LocalConfiguration` are accessed via `CompositionLocal` — never passed as raw parameters unless architecturally justified.
- `@Preview` annotations with both light and dark variants are mandatory for every screen-level Composable.
- Magic number `dp` values are rejected. A spacing/size token object is defined and referenced consistently.
- `LazyColumn` / `LazyRow` are mandatory for any list exceeding 4 items.
- Edge-to-edge support via `WindowInsets`, `imePadding()`, `systemBarsPadding()` is baseline, not optional.

---

## Absolute Prohibitions

The following constitute design and engineering failures. They are categorically forbidden:

- Plain white `Surface` + `Text` + `Button` layout with zero design intent.
- Hardcoded color literals in leaf Composables.
- Repeating the same generic rounded-card pattern across generations.
- Omitting the `Modifier` parameter from reusable Composables.
- Unstyled `Column` with default spacing and default `Text` style presented as design.
- Treating Material 3 defaults as the destination. They are the minimum viable starting point.
- Converging on identical aesthetic choices across separate generation runs. Every output must be visually distinct.

---

## Aesthetic Direction Reference

| Direction | Defining Characteristics |
|---|---|
| **Serafina / Expressive** | Lavender + neon lime palette, organic blob backgrounds, pill chips, large rounded surface cards, staggered entrance sequences |
| **Brutalist / Raw** | `CutCornerShape`, monospace display font, maximum contrast B&W, zero elevation, controlled density |
| **Luxury / Refined** | Deep navy + gold accent, serif display font, generous whitespace, subtle shimmer surfaces, elevated cards |
| **Retro-Futuristic** | Scanline Canvas overlay, phosphor green on deep black, repeating geometric grid background |
| **Editorial / Magazine** | Asymmetric composition, oversized typography, ruled line dividers, image-dominant layout |
| **Soft / Pastel** | Cream + dusty rose + sage palette, 28dp+ corner radii, gentle spring animation physics |

---

Compose is capable of extraordinary visual output far beyond Material 3 scaffold defaults. Full commitment to the chosen aesthetic direction — executed with engineering precision — is the only acceptable standard.

---
name: compose-layout
description: Improve layout, spacing, and visual rhythm in Jetpack Compose. Fixes monotonous grids, inconsistent spacing, and weak visual hierarchy. Use when the user mentions layout feeling off, spacing issues, visual hierarchy, crowded Composables, alignment problems, adaptive layout failures, or wanting better spatial composition.
version: 1.0.0
argument-hint: "[target composable or screen]"
---

Assess and improve layout and spacing that feels monotonous, crowded, or structurally weak — turning generic Compose arrangements into intentional, rhythmic, adaptive compositions.

## MANDATORY PREPARATION

Invoke $compose-design — it contains design principles, anti-patterns, and the **Context Gathering Protocol**. Follow the protocol before proceeding — if no design context exists yet, you MUST run $compose-design first.

---

## Assess Current Layout

Analyze what is weak about the current spatial design:

1. **Spacing**:
   - Is spacing consistent or arbitrary? (Random `padding` values with no system behind them)
   - Is all spacing the same? (Identical `padding` everywhere = no rhythm)
   - Are related Composables grouped tightly via `Arrangement.spacedBy`, with generous separation between distinct sections via `Spacer`?
   - Is spacing ownership correct? Parent owns the space between itself and its children via `padding`. Siblings use `Arrangement.spacedBy` for uniform gaps or `Spacer` for irregular ones. This distinction is non-negotiable — violating it breaks touch indicator bounds on interactive elements.

2. **Visual hierarchy**:
   - Apply the squint test: blur your (metaphorical) eyes — can you still identify the most important Composable, the second most important, and clear groupings?
   - Is hierarchy achieved effectively? `MaterialTheme.typography` scale and weight differences alone can establish hierarchy without color or size changes — is the current approach actually working?
   - Does whitespace guide the eye to what matters?

3. **Grid & structure**:
   - Is there a clear underlying structure, or does the layout feel random?
   - Are identical `LazyVerticalGrid` items used everywhere with the same `icon + heading + text` pattern repeated endlessly?
   - Is everything centered via `Alignment.CenterHorizontally`? Start-aligned layouts with deliberate asymmetry feel more designed — though not a universal rule.

4. **Rhythm & variety**:
   - Does the layout have visual rhythm? (Alternating tight/generous `Arrangement.spacedBy` values)
   - Is every screen section structured with the same `Column` + `Card` pattern? (Monotonous repetition)
   - Are there intentional moments of emphasis — a full-bleed element, a spanning grid item, an asymmetric `Row`?

5. **Density**:
   - Is the layout too cramped? (Insufficient breathing room between content groups)
   - Is the layout too sparse? (Generous `Spacer` values without spatial purpose)
   - Does density match the content type? (Data-dense list UIs need tighter `Arrangement.spacedBy`; onboarding and marketing screens need air)

6. **Adaptivity**:
   - Does the layout respond to `WindowSizeClass`? (Compact / Medium / Expanded — ignoring these is an engineering failure on foldables, tablets, and large screens)
   - Are navigation components correct per window size? (`BottomAppBar` on Compact, `NavigationRail` on Medium, `NavigationDrawer` on Expanded)
   - Is `currentWindowAdaptiveInfo()` used — not deprecated screen dimension APIs?

**CRITICAL**: Layout problems are often the root cause of interfaces feeling "off" even when color and typography are correct. Space is a design material in Compose just as much as `ColorScheme` or `TextStyle`. Use it with the same intentionality.

## Plan Layout Improvements

Create a systematic plan before modifying any Composable:

- **Spacing system**: Define a token object with a scale derived from multiples of 4dp. Values must come from this scale — arbitrary `dp` values are rejected.
- **Hierarchy strategy**: How will space communicate importance? Which Composables receive generous surrounding space, which are tightly grouped?
- **Layout approach**: What structure fits the content? `Column` / `Row` for 1D flows. `LazyVerticalGrid` / `LazyHorizontalGrid` for 2D collections. `Box` for overlay and depth. Custom `Layout` Composable when none of the above are sufficient.
- **Rhythm**: Where should spacing be tight vs generous? Define this explicitly before touching padding values.
- **Adaptive breakpoints**: Define behavior per `WindowWidthSizeClass` — Compact, Medium, Expanded — before writing any adaptive logic.

## Improve Layout Systematically

### Establish a Spacing System

- Define a single spacing token object. All `padding`, `Arrangement.spacedBy`, and `Spacer` values are drawn exclusively from this scale — no arbitrary `dp` literals anywhere in production layout code.
- **Parent-child spacing**: Parent owns the gap between itself and its children via `Modifier.padding`. The child never self-applies spacing that creates distance from its parent container.
- **Sibling spacing**: Use `Arrangement.spacedBy` on `Column` / `Row` / `LazyColumn` / `LazyRow` when spacing between siblings is uniform. Use `Spacer` for one-off irregular gaps within an otherwise uniform layout.
- Compose has no `margin` — simulate external spacing by wrapping in `Box` with `padding`, or by applying `padding` before `clickable` in the modifier chain so the touch target respects the spacing intent.
- **Modifier order is semantic**: `Modifier.padding(x).clickable` means the touch target excludes the padding. `Modifier.clickable.padding(x)` means the touch target includes the padding. This distinction determines whether ripples extend to screen edges. Choose deliberately.
- Use `contentPadding` on `LazyColumn` / `LazyRow` / `LazyVerticalGrid` for edge insets — never apply outer `padding` to the lazy container itself, which clips scroll content.

### Create Visual Rhythm

- **Tight grouping** for related Composables: 4–12dp between siblings within the same semantic group
- **Generous separation** between distinct screen sections: 32–64dp
- **Varied spacing** within sections — not every `Column` item needs the same `spacedBy` value
- **Asymmetric compositions** — break the centered `Column(horizontalAlignment = CenterHorizontally)` default when asymmetry is more intentional

### Choose the Right Layout Composable

- **`Column` / `Row` for 1D layouts**: Vertical or horizontal flows, nav bars, button groups, card internals, most component composition. These cover the majority of layout tasks — reach for them first.
- **`LazyColumn` / `LazyRow` for 1D lists**: Mandatory for any list exceeding 4 items. Never use `Column` inside `verticalScroll` for long content — it defeats lazy composition.
- **`LazyVerticalGrid` / `LazyHorizontalGrid` for 2D collections**: Use `GridCells.Adaptive(minSize)` for responsive grids that reflow without breakpoints. Use `GridCells.Fixed` only when column count is a product requirement, not a convenience default. Use `GridItemSpan` to vary item spans and break monotony.
- **`Box` for overlay and depth**: Overlapping Composables, background layers, badge positioning. Not a substitute for `Column` / `Row` when stacking is the intent.
- **`FlowRow` / `FlowColumn`** (Foundation): For wrapping chip groups, tag lists, and dynamic-width content that must reflow. `ContextualFlowRow` / `ContextualFlowColumn` for lazy-rendered flow layouts (available since Compose June 2024).
- **Custom `Layout` Composable**: When measurement constraints or placement logic cannot be expressed with the standard containers. Compose forbids re-measuring a child node — use `IntrinsicSize.Min` / `IntrinsicSize.Max` via `Modifier.height(IntrinsicSize.Min)` to align siblings without a second measure pass. Do not use `BoxWithConstraints` inside intrinsic-measured layouts.
- **`SubcomposeLayout`**: Only when a child's composition depends on another child's measured size. Carries a performance cost — use only when architecturally necessary.

### Break Grid Monotony

- Do not default to `LazyVerticalGrid` with identical `Card` items for every collection — spacing and alignment create visual grouping naturally without containers.
- Use `Card` only when content is truly distinct and independently actionable. Never nest `Card` inside `Card`.
- Vary grid item spans via `GridItemSpan`. Mix full-width items with standard grid items to break repetition. Introduce header items via `item { }` blocks inside `LazyVerticalGrid`.
- Alternate tight and generous spacing between grid sections to establish visual rhythm.

### Strengthen Visual Hierarchy

- The fewest dimensions needed for clear hierarchy. Generous `Spacer` around an element draws the eye — space alone is sufficient for many hierarchy decisions. Add `fontWeight`, `fontSize`, or color contrast only when space is insufficient.
- Reading flow on Android follows top-left to bottom-right in LTR, right-to-left in RTL (Compose handles RTL mirroring automatically via `LocalLayoutDirection` — never hardcode directional padding with `start` / `end` vs `left` / `right`).
- Create content groupings through proximity (`Arrangement.spacedBy` tight) and separation (`Spacer` generous). `HorizontalDivider` / `VerticalDivider` for explicit boundaries only when spatial separation is insufficient.

### Manage Depth & Elevation

- Build a semantic `zIndex` scale as named constants — tooltip > modal > modal-backdrop > sticky header > content. Never use `Modifier.zIndex(999f)` or arbitrary float literals.
- `Modifier.shadow(elevation, shape)` for drop shadows. Build a semantic shadow scale (none / low / medium / high) matching `MaterialTheme.colorScheme.surfaceTint` elevation tinting in Material 3.
- `CardDefaults.cardElevation` for `Card` elevation — use the semantic parameters (`defaultElevation`, `pressedElevation`, `focusedElevation`) rather than overriding with flat values.
- Elevation reinforces hierarchy. It is not decoration. Flat layouts with no elevation differentiation flatten hierarchy as surely as identical spacing.

### Build for Adaptive Layouts

- Determine `WindowSizeClass` via `currentWindowAdaptiveInfo()` from `material3-adaptive`. Never use deprecated `Display` or screen dimension APIs.
- Three window widths: **Compact** (phone portrait) — single-column, `BottomAppBar`. **Medium** (phone landscape, small tablet) — flexible two-pane consideration, `NavigationRail`. **Expanded** (tablet, foldable unfolded, desktop) — multi-pane (`ListDetailPaneScaffold`, `SupportingPaneScaffold`), `NavigationDrawer`.
- Foldable posture matters: `isTabletop` posture drives split layouts at the hinge. Compose Adaptive APIs handle this — use them.
- `NavigationSuiteScaffold` from Material 3 Adaptive automatically selects the correct navigation component per window size — do not manually switch between `BottomAppBar`, `NavigationRail`, and `NavigationDrawer` without it.

### Optical Adjustments

- If a `Icon` or `Text` reads as visually off-center despite geometric centering, apply a deliberate `offset` correction — but only when the misalignment is objectively confirmed, not speculatively adjusted.

**NEVER**:
- Use arbitrary `dp` values outside the defined spacing scale
- Apply identical `padding` to all elements — variety is what creates hierarchy
- Wrap everything in `Card` — not every grouping needs a container
- Nest `Card` inside `Card` — use spacing and `HorizontalDivider` for internal hierarchy
- Use identical `LazyVerticalGrid` items everywhere with the same icon + heading + text structure
- Default to `Column(horizontalAlignment = CenterHorizontally)` for everything — start-aligned asymmetry reads as more intentional
- Use `LazyColumn` / `LazyRow` column counts as `GridCells.Fixed` arbitrary integers without a design rationale
- Use `BoxWithConstraints` as a substitute for proper `WindowSizeClass`-driven adaptive logic
- Ignore `WindowSizeClass` — adaptive layout is a baseline requirement, not a premium feature
- Use arbitrary `zIndex` float literals (99f, 999f) — build a semantic scale
- Apply `padding` after `clickable` in the modifier chain when the intent is a full-size touch target

## Verify Layout Improvements

- **Squint test**: Can you identify primary, secondary, and groupings with blurred vision?
- **Rhythm**: Does the screen have a satisfying beat of tight and generous spacing?
- **Hierarchy**: Is the most important content obvious within 2 seconds without any explanation?
- **Breathing room**: Does the layout feel comfortable — not cramped, not wastefully sparse?
- **Consistency**: Is the spacing token scale applied without exceptions?
- **Adaptive**: Does the layout respond correctly across Compact, Medium, and Expanded windows? Test on emulators for all three.
- **RTL**: Does the layout mirror correctly under `LocalLayoutDirection.provides(LayoutDirection.Rtl)`?
- **Touch targets**: Do all interactive Composables meet the 48dp minimum touch target requirement?

Space is the most underused design tool in Jetpack Compose. A layout with the right rhythm and hierarchy — built on a consistent spacing system, responsive to window size, and respectful of reading flow — makes even simple content feel polished and inevitable.

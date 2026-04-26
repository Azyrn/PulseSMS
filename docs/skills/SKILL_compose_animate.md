---
name: compose-animate
description: Review a Jetpack Compose feature and enhance it with purposeful animations, micro-interactions, and motion effects that improve usability and delight. Use when the user mentions adding animation, transitions, micro-interactions, motion design, press effects, gesture feedback, or making the Compose UI feel more alive.
version: 1.0.0
argument-hint: "[target composable or screen]"
---

Analyze a Compose feature and strategically add animations and micro-interactions that enhance understanding, provide feedback, and create delight.

## MANDATORY PREPARATION

Invoke $compose-design — it contains design principles, anti-patterns, and the **Context Gathering Protocol**. Follow the protocol before proceeding — if no design context exists yet, you MUST run $compose-design first. Additionally gather: performance constraints, minimum SDK target, and device tier (mid-range vs flagship).

---

## Assess Animation Opportunities

Analyze where motion would improve the experience:

1. **Identify static areas**:
   - **Missing feedback**: Actions without visual acknowledgment (button taps, form submission, FAB press, swipe gestures)
   - **Jarring transitions**: Instant state changes that feel abrupt (show/hide Composables, screen navigation, content swaps via `AnimatedContent`)
   - **Unclear relationships**: Spatial or hierarchical relationships that aren't communicated visually (list reordering, expand/collapse, parent-child navigation)
   - **Lack of delight**: Functional but joyless interactions — correct behavior, zero personality
   - **Missed guidance**: Opportunities to direct attention or explain behavior through motion

2. **Understand the context**:
   - What's the personality? (Playful vs serious, energetic vs calm)
   - What's the performance budget? (Mid-range device? Complex lazy list? Heavy `Canvas` background?)
   - Who's the audience? (Motion-sensitive users? Power users who want speed?)
   - What matters most? (One hero animation vs many micro-interactions?)

If any of these are unclear from the codebase or design context, ask the user directly. Do not infer and proceed silently.

**CRITICAL**: Respect `LocalReduceMotionEnabled` and system-level `Animation Scale` settings. Always provide non-animated or simplified alternatives for users who need them. This is an accessibility obligation, not a suggestion.

## Plan Animation Strategy

Create a purposeful animation plan before touching any Composable:

- **Hero moment**: What's the ONE signature animation? (Screen entry? Hero card expansion? Key interaction via shared element?)
- **Feedback layer**: Which interactions need acknowledgment? (Tap ripple, press scale, swipe confirmation)
- **Transition layer**: Which state changes need smoothing? (`AnimatedVisibility`, `AnimatedContent`, `animateContentSize`)
- **Delight layer**: Where can we surprise and delight without compromising performance?

**IMPORTANT**: One well-orchestrated `LaunchedEffect` entry sequence beats scattered `animate*AsState` calls everywhere. Focus on high-impact moments. Animation fatigue is real on Android — restraint is a design decision.

## Implement Animations

Add motion systematically across these categories:

### Entrance Animations
- **Screen entry choreography**: Stagger Composable reveals using `LaunchedEffect` with coroutine delays (100–150ms offsets), combine `alpha` + `translationY` via `graphicsLayer`
- **Hero section**: Dramatic entrance for primary content — scale, parallax via `graphicsLayer`, or `SharedTransitionLayout` shared element entry
- **Content reveals**: Scroll-triggered visibility using `LazyListState.firstVisibleItemIndex` or `derivedStateOf` to gate `AnimatedVisibility`
- **Bottom sheet / dialog entry**: `AnimatedVisibility` with `slideInVertically` + `fadeIn`, scrim fade via `animateFloatAsState`

### Micro-interactions
- **Button / FAB feedback**:
  - Press: Scale down via `interactionSource` + `collectIsPressedAsState` → `animateFloatAsState` (0.95f on press, 1f on release)
  - Hover (foldable / large screen): Color shift via `animateColorAsState`
  - Loading: Circular `CircularProgressIndicator` swap via `AnimatedContent`, or `rememberInfiniteTransition` pulse
- **Form interactions**:
  - Input focus: Border color transition via `animateColorAsState`, subtle elevation lift via `animateDpAsState`
  - Validation error: Horizontal shake via `Animatable` with sequential `animateTo` calls
  - Validation success: Check mark entrance via `AnimatedVisibility` + `scaleIn`
- **Toggle / Switch**: `spring`-based thumb translation via `animateFloatAsState`, color transition via `animateColorAsState`
- **Checkbox / Radio**: Check mark draw animation via `Canvas` + `animateFloatAsState` on path progress
- **Like / Favorite**: Scale burst + color flip via `updateTransition` driving multiple properties simultaneously

### State Transitions
- **Show / Hide**: `AnimatedVisibility` with explicit `enter` / `exit` specs — never instant toggle. Target 200–300ms.
- **Expand / Collapse**: `animateContentSize` with `spring` spec, icon rotation via `animateFloatAsState`
- **Loading states**: Shimmer effect via `rememberInfiniteTransition` on `Brush` offset; skeleton screens fading in via `AnimatedVisibility`
- **Success / Error**: Color transitions via `animateColorAsState`, icon swap via `AnimatedContent`
- **Enable / Disable**: `animateFloatAsState` on `alpha` + `graphicsLayer`, `animateColorAsState` on container color

### Navigation & Flow
- **Screen transitions**: `AnimatedContent` with `slideInHorizontally` + `fadeIn` / `slideOutHorizontally` + `fadeOut` for push navigation
- **Shared element transitions**: `SharedTransitionLayout` + `Modifier.sharedElement()` + `Modifier.sharedBounds()` for list-to-detail flows (stable as of Compose 1.7 / Google I/O 2024). Keys must be identical on both ends — a mismatch compiles silently and produces no animation.
- **Tab switching**: Animated indicator via `animateDpAsState` on offset, content swap via `AnimatedContent`
- **LazyColumn / LazyRow item animations**: `Modifier.animateItem()` for automatic insert, remove, and reorder animations (replaces deprecated `animateItemPlacement`)
- **Scroll effects**: `graphicsLayer` parallax on hero via `LazyListState.firstVisibleItemScrollOffset`, `TopAppBar` collapse via `LargeTopAppBar` built-in scroll behavior

### Feedback & Guidance
- **Ripple / Press hint**: `indication = ripple()` via `Modifier.clickable` — ensure it is not suppressed on interactive surfaces
- **Drag & drop**: Lift effect via `animateFloatAsState` on `shadowElevation` + scale; drop zone highlight via `animateColorAsState`
- **Copy confirmation**: Brief `AnimatedVisibility` tooltip or icon swap via `AnimatedContent`
- **Focus traversal**: `FocusRequester` combined with `BringIntoViewRequester` for smooth scroll-to-focus in forms

### Delight Moments
- **Empty states**: Subtle float loop via `rememberInfiniteTransition` on `translationY`
- **Completed actions**: Scale + alpha burst via `Animatable`; confetti via `Canvas` particle system driven by `rememberInfiniteTransition`
- **Easter eggs**: Hidden gesture interactions via `detectTapGestures` or `pointerInput` with sequential `Animatable` sequences
- **Contextual animation**: Time-of-day palette shifts, seasonal `Canvas` overlays — use only when the product personality explicitly demands it

## Technical Implementation

Use the correct Compose animation API for each use case. Choosing the wrong API is an engineering error, not a style preference:

### API Selection Hierarchy

**Single value, state-driven** → `animate*AsState` (`animateFloatAsState`, `animateColorAsState`, `animateDpAsState`, etc.)

**Multiple values, same state** → `updateTransition` — drives all properties from one state object, ensures synchronization

**Infinite / ambient** → `rememberInfiniteTransition` — shimmer, pulse, breathing effects

**Sequential / concurrent coroutine-controlled** → `Animatable` — shake sequences, gesture-synchronized motion, anything requiring `snapTo` or `animateDecay`

**Composable enter / exit** → `AnimatedVisibility` with explicit `EnterTransition` / `ExitTransition`

**Content swap** → `AnimatedContent` with `transitionSpec` — tab switches, loading→content, error→retry

**Size change** → `animateContentSize` modifier — expand/collapse without layout recomposition

**Screen-to-screen with shared UI element** → `SharedTransitionLayout` + `Modifier.sharedElement()` / `Modifier.sharedBounds()`

**List item insert / remove / reorder** → `Modifier.animateItem()` inside `LazyColumn` / `LazyRow` / `LazyVerticalGrid`

### Timing & Easing

**Durations by purpose:**
- **80–150ms**: Instant feedback — button press, toggle, ripple acknowledgment
- **200–300ms**: State changes — hover color, menu open, field focus
- **300–500ms**: Layout changes — accordion expand, bottom sheet entry, modal
- **400–600ms**: Screen entrance animations, shared element transitions

**AnimationSpec selection:**
- `spring()` is the Compose default and the correct default for most interactions. Set `dampingRatio` and `stiffness` explicitly — never accept defaults blindly. `Spring.DampingRatioNoBouncy` + `Spring.StiffnessMedium` is a safe production baseline.
- `tween()` with a deliberate `easing` is appropriate for duration-critical animations (entrance sequences, progress bars). `FastOutSlowInEasing` for elements entering. `LinearOutSlowInEasing` for elements leaving.
- `keyframes` for complex multi-stage sequences with specific intermediate values.
- `snap()` only for instant transitions where animation would be counterproductive.

**Exit animations run at ~75% of enter duration.** Exits that overstay their welcome degrade perceived performance.

**Bounce and elastic easing are categorically rejected.** They draw attention to the animation mechanism itself, not the content. They feel dated. They are never acceptable in production Compose UI.

### Performance — Non-Negotiable

- Animate only `alpha` and `graphicsLayer` properties (`scaleX`, `scaleY`, `translationX`, `translationY`, `rotationZ`, `alpha`) — these run on the render thread and skip recomposition entirely
- Animating layout properties that trigger recomposition (`size`, `padding`, `offset` as layout modifiers) is only acceptable when `animateContentSize` or `animateDpAsState` is used via `graphicsLayer`
- `Modifier.graphicsLayer { }` is the correct mechanism for render-thread animation. Use it.
- Avoid constructing new `AnimationSpec` objects inside composition on every frame — hoist them to `remember` or top-level constants
- Use `derivedStateOf` to gate expensive recompositions driven by scroll state
- Heavy `Canvas` animations must be profiled on mid-range hardware (Snapdragon 6xx class). 60fps is the floor. Jank is a regression.
- Shared element transitions add overhead. Do not apply them to every list item simultaneously. Profile before shipping.

### Accessibility — Non-Negotiable

Android exposes `Animation Scale` in Developer Options (0x, 0.5x, 1x, 2x). Apps must respect this. Check `LocalReduceMotionEnabled` in Compose and provide reduced or disabled animation paths. Ignoring this is an accessibility violation with real user impact — not an edge case.

**NEVER**:
- Use bounce or elastic easing — rejected without exception
- Animate layout-triggering properties directly when `graphicsLayer` achieves the same result
- Use durations over 500ms for feedback interactions — it registers as lag
- Animate without purpose — every animation must justify its performance cost
- Suppress the system ripple `indication` on interactive surfaces without a deliberate replacement
- Animate every list item on scroll entry — this is animation fatigue at scale
- Block user interaction during animations unless the interaction would corrupt state

## Verify Quality

Test animations under these conditions before considering them complete:

- **60fps on mid-range hardware**: Profile with Android Studio's Profiler. Jank on a Pixel 6a is a bug.
- **Feels natural**: Spring curves feel organic. Tween easing matches the direction of motion (enter vs exit).
- **Appropriate timing**: Not jarring (too fast) and not laggy (too slow). Have a non-developer verify.
- **Animation Scale 0x**: All animations disabled or reduced. UI remains fully functional. No broken layouts.
- **Does not block interaction**: Users can act during and after animations unless explicitly gated.
- **Shared element keys match exactly**: Mismatched keys produce silent failures — no crash, no animation.
- **Adds measurable value**: Removes cognitive load, confirms an action, or communicates a relationship. Decoration alone is insufficient justification.

Motion in Compose must enhance understanding and provide feedback — not perform for its own sake. Every animation has a performance cost, a maintenance cost, and an accessibility implication. Animate with purpose, respect the platform constraints, and validate on real hardware. Great motion is invisible — it makes everything feel inevitable.

# Jetpack Compose Design Skill

Use this design standard for any Android UI, screen, flow, or Composable work in this repository. Generic output is a failure state.

## Design Thinking

Before writing UI code, lock in:

- Purpose: What job the screen solves and who operates it.
- Tone: A single clear direction such as brutally minimal, expressive, refined, editorial, retro-futuristic, or soft.
- Constraints: SDK level, Compose and Material versions, navigation stack, theme system, performance budgets.
- Differentiator: One memorable visual or interaction trait the screen is built around.

Commit to a strong direction. Avoid safe middle-ground styling that reads like template output.

## Compose Aesthetics

- Customize typography beyond Material defaults. Distinctive headline and body pairing is preferred when the stack allows it.
- Define an intentional Material 3 color system. Use dynamic color on API 31+ only as an overlay on top of a custom scheme.
- Use shape, depth, gradients, backgrounds, and motion deliberately. Flat default surfaces are not acceptable as final design.
- Prefer intentional hierarchy and spatial composition over generic stacked cards.
- Use expressive motion for high-value events and calmer motion for scrolling and passive browsing.

## Engineering Directives

- Keep composable trees intentionally flat.
- Preserve stable state shapes and keyed list models to prevent unnecessary recompositions.
- All reusable composables must expose `modifier: Modifier = Modifier`.
- Use theme tokens, spacing tokens, and semantic extensions instead of hardcoded leaf values or magic numbers.
- Provide `@Preview` coverage for screen-level composables in light and dark variants.
- Use `LazyColumn` or `LazyRow` for any non-trivial list.
- Preserve edge-to-edge behavior with proper insets handling.

## Absolute Prohibitions

- Plain default `Surface` plus `Text` plus `Button` layouts presented as finished design.
- Hardcoded color literals in leaf composables.
- Repeating the same generic rounded-card pattern without a stronger design reason.
- Leaking protocol, sync, or storage concerns into composable contracts.

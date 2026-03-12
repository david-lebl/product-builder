# UI Kit Review

## What the Kit Provides

`modules/ui-framework` (`mpbuilder.uikit`) is a standalone Scala.js/Laminar component library with no dependency on the domain module. It ships:

**Field components** (`fields/`)
- `TextField` — text/email/number/password input with optional error signal
- `TextAreaField` — multi-line textarea
- `SelectField` — `<select>` with `SelectOption` list and optional placeholder
- `CheckboxField` — single checkbox with boolean signal
- `RadioGroup` — mutually exclusive radio buttons via `RadioOption` list

**Container components** (`containers/`)
- `Tabs` — tabbed panel driven by a `Var[String]` active-tab key
- `Stepper` — multi-step wizard with prev/next navigation

**Feedback** (`feedback/`)
- `ValidationDisplay` — renders a success message or a list of error strings

**Utility** (`util/`)
- `Visibility.when(signal)` / `Visibility.unless(signal)` — CSS display toggle modifiers

**Form abstraction** (`form/`)
- `FieldType` — type class mapping Scala types to HTML input types
- `FieldValidator` — composable validators: `required`, `minLength`, `maxLength`, `min`, `max`, `regex`
- `FormFieldState` — single field state: raw string, touched flag, parse + validate pipeline
- `FormState` — Mirror-based compile-time derivation of form state from a case class; supports `.withValidators`, `.touchAll`, `.validated`, `.allErrors`
- `FormConfig` / `FieldConfig` — per-field configuration (placeholder, input type override)
- `FormRenderer` — renders a derived `FormState` as a `<div class="form-derived">` element

All label and option text is typed as `Signal[String]` (not a domain `Language` enum) to preserve independence from the application domain.

---

## Code Reduction Analysis

The `feat: refactor forms to use UI kit components` commit touched three files:

| File | Before (approx. lines) | After (approx. lines) | Change |
|------|-----------------------:|----------------------:|-------|
| `FormFieldState.scala` | 60 | 45 | −15 |
| `FormState.scala` | 90 | 75 | −15 |
| `FormRenderer.scala` | 70 | 55 | −15 |

**Honest assessment:** the raw line-count savings are modest (~45 lines across three files). The real benefit is consistency — forms in the main app now compose the same building blocks as anything else in `uikit`, and field-level validation wiring is no longer duplicated per call-site. However, the refactor also introduced the `form/` abstraction layer itself, so the *net* addition to the codebase was positive in total lines. The payoff is deferred: it will be real once more forms exist.

---

## Styling Gap — The Main Pain Point

The framework emits CSS class names (`.tabs`, `.tab-button--active`, `.stepper`, `.field-error`, `.form-group`, etc.) but **ships zero default styles**. Every consumer must write all CSS themselves.

This caused the immediate problem that triggered this investigation: the Showcase rendered unstyled inside the main app's `index.html` because that file only had styles for the main app's own classes, not for ui-kit's classes.

**Concrete classes with no default styles shipped:**

```
.tabs  .tabs-bar  .tab-button  .tab-button--active  .tabs-content
.stepper  .stepper-bar  .stepper-step  .stepper-step--done  .stepper-step--active
.stepper-step-num  .stepper-step-label  .stepper-content  .stepper-nav
.form-group  .field-error
.checkbox-label  .radio-group  .radio-label
.success-message  .error-message  .error-list
.form-derived
```

---

## Recommendations

### Option A — Ship a bundled `uikit.css` (recommended)

Place a `uikit.css` file in `modules/ui-framework/src/main/resources/`. Consumers `<link>` it in their `index.html` and get working defaults immediately. The main app and the showcase both just include the same file. Overriding specific rules is straightforward with a subsequent stylesheet.

**Pros:** zero setup for new consumers; consistent baseline across apps.
**Cons:** requires maintaining a CSS file alongside Scala code; class names become a public API.

### Option B — CSS variables-based theming

Ship `uikit.css` using CSS custom properties (`--uikit-primary`, `--uikit-radius`, etc.) for all colors and dimensions. Consumers override variables in their own `<style>` block without touching the base file.

**Pros:** theming is trivial; still a single file to maintain.
**Cons:** slightly more design up-front; IE 11 irrelevant (Scala.js already requires ES modules).

### Option C — Accept no-default-styles, improve documentation

Keep the current approach but document every emitted class name with its intended purpose, expected layout context, and a minimal example CSS block. Treat the class names as a contractual API that won't change without a version bump.

**Pros:** zero CSS maintenance; consumers have full control.
**Cons:** every new consumer (or isolated showcase) must rewrite the same ~150 lines of CSS. The showcase issue will recur.

---

## Verdict

The abstraction is **worth keeping** but needs Option A or B to be genuinely useful. Right now the component API (signal-driven props, composable validators, Mirror-based derivation) is well-designed and reduces boilerplate at call sites. The single gap is that it ships class names with no corresponding styles, making it harder to adopt than a framework that "just works" out of the box.

**Recommended next step:** extract the CSS written for the `ui-showcase` `index.html` into `modules/ui-framework/src/main/resources/uikit.css`, add CSS variables for the primary colour, and update both `modules/ui/src/main/resources/index.html` and `modules/ui-showcase/src/main/resources/index.html` to `<link>` it. This closes the gap with minimal effort.

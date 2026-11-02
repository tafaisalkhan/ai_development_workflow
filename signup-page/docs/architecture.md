# Responsive Signup Page — Architecture

**Status:** Draft architecture for implementation  
**Scope:** A static, responsive signup page using HTML, CSS, and JavaScript only.  
**Inputs:** No existing source files, approved architecture, or review findings were provided. This is a greenfield design.

## 1. Architecture Summary

The solution is a static browser application with no backend, no persistence, and no external runtime dependencies.

It is organized into four concerns:

1. **Semantic HTML** for the signup form and accessible field/error relationships.
2. **Responsive CSS** for mobile, tablet, and desktop usability.
3. **Vanilla JavaScript** for validation orchestration, feedback rendering, and submission gating.
4. **Isolated validation policy** so the exact username and password rules can be confirmed without rewriting the UI or controller.

The exact validation rules are not specified in the approved requirements. This architecture therefore does **not** invent length, character, complexity, trimming, or normalization rules. Those rules are a blocking configuration point in the validation policy module.

## 2. Technology Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Markup | HTML5 semantic form | Required technology; supports labels, inputs, and accessible feedback relationships. |
| Styling | Hand-authored CSS with custom properties | No frameworks or preprocessors; allows mobile-first responsive layout and consistent states. |
| Scripting | Vanilla JavaScript | Required technology; no runtime framework or library. |
| Validation | Custom JavaScript validation engine | Requirement NFR-03 calls for browser-side JavaScript validation. Native browser validation alone cannot express the pending rule set. |
| Form validation mode | Use `novalidate` on the form and handle validation in JavaScript | Keeps validation messages and submission blocking under application control. Native constraints can mirror rules later if desired. |
| Build step | None | Static files can be served directly; no bundler or transpiler required. |
| External APIs | None | Backend/API development is explicitly excluded. |
| Submission behavior | Configurable submission handler/seam | Post-validation behavior is unspecified. The page must not invent a destination, success page, or API call. |
| Testing | Small browser test harness for DOM behavior; pure validation logic tested as JavaScript modules/functions | No production test framework is required. Test-only tooling must not become a runtime dependency. |

If the approved browser matrix cannot be confirmed, ES module usage should be reviewed. The module boundaries can remain the same if classic scripts are required.

## 3. Components and Responsibilities

| Component | Responsibility |
|---|---|
| `index.html` | Semantic signup form containing username input, password input, submit button, labels, error containers, and live-region hooks. |
| `app.js` | Bootstraps the page, supplies the validation rule set and submission handler, and initializes the form controller. |
| `FormController` | Wires DOM events, reads form values, invokes validation, coordinates feedback rendering, and blocks or delegates submission. |
| `ValidationEngine` | Pure validation orchestration. Accepts values and a rule set; returns a structured validation result. Does not access the DOM. |
| `validation-rules.js` | Validation policy configuration. Exact username and password rules are **to be confirmed**. Must not hard-code unapproved rules. |
| `messages.js` | Validation message copy. Exact wording is to be confirmed; messages must not echo password values. |
| `FeedbackRenderer` | Updates field error text, `aria-invalid`, focus state, and live-region announcements. |
| `SubmissionHandler` | Integration seam for approved post-validation behavior. Default behavior while unspecified must not invent an endpoint or success flow. |
| CSS modules | Tokens, base styles, layout, form styles, responsive behavior, and validation states. |

### Internal contracts

```text
ValidationResult = {
  valid: boolean,
  errors: {
    username: string | null,
    password: string | null
  }
}

RuleSet = {
  validateUsername(value: string): string | null,
  validatePassword(value: string): string | null
}

SubmissionHandler = {
  submit(values: { username: string, password: string }): void
}
```

A returned string from a rule validator represents a user-facing error message. `null` means that field passed the configured rule for that invocation.

## 4. Project Structure

```text
signup-page/
├── index.html
├── assets/
│   ├── css/
│   │   ├── tokens.css
│   │   ├── base.css
│   │   ├── layout.css
│   │   └── form.css
│   └── js/
│       ├── app.js
│       ├── form-controller.js
│       ├── validation-engine.js
│       ├── validation-rules.js
│       ├── messages.js
│       ├── feedback-renderer.js
│       └── submission-handler.js
├── tests/
│   ├── validation-engine.test.js
│   ├── dom-test.html
│   ├── dom-test.js
│   └── manual-test-plan.md
└── README.md
```

The structure deliberately avoids a build pipeline. Files can be served from any static host.

## 5. Data Flow

### 5.1 Validation flow

1. `app.js` initializes `FormController` with:
   - the form element,
   - the agreed rule set,
   - the message catalog,
   - the submission handler.
2. The user enters a username and password.
3. A validation trigger fires. The trigger policy—input, blur, submit, or a combination—is still an **open question**. The controller must support these triggers through configuration rather than scattering event logic.
4. `FormController` reads the current field values and passes them to `ValidationEngine`.
5. `ValidationEngine` evaluates each value against `validation-rules.js` and returns a `ValidationResult`.
6. `FeedbackRenderer` updates:
   - field-level error text,
   - `aria-invalid`,
   - `aria-describedby` relationships,
   - live-region announcements,
   - focus movement where appropriate.
7. If any field is invalid, the controller prevents successful submission and focuses the first invalid field.
8. If all fields pass the approved rules, the controller invokes the configured `SubmissionHandler`.

### 5.2 Submission flow

```text
User submits form
    → FormController intercepts submit
    → validate all fields
    → if invalid:
          prevent submission
          render feedback
          focus first invalid field
    → if valid:
          delegate to approved SubmissionHandler
```

The actual post-validation behavior is unspecified. The architecture must not assume an API endpoint, redirect, success message, or account creation. The submission handler is therefore a deliberate integration point, not an implemented backend flow.

### 5.3 State and persistence

- No account data is persisted in the browser.
- No password is written to `localStorage`, `sessionStorage`, cookies, URLs, console logs, or analytics.
- Validation state is in-memory only.
- No network request is made by this scope.

## 6. APIs

### External APIs

**Not applicable.** Backend/API development, account persistence, server-side validation, authentication, and account creation are excluded by the approved requirements.

### Internal APIs

The internal interfaces are documented in Section 3. These boundaries allow the validation rules and submission behavior to be completed once approved without restructuring the page.

A future backend integration would require a separate approved change and must not rely on client-side validation as a security control.

## 7. Responsive and Accessibility Strategy

### Responsive behavior

- Use a mobile-first, fluid layout.
- Use a constrained form container that shrinks to the viewport and does not use fixed pixel widths.
- Ensure inputs and the primary action remain reachable without horizontal scrolling.
- Use flexible spacing and typography tokens.
- Avoid depending on specific breakpoints where fluid layout is sufficient; exact breakpoints are not specified.
- Verify representative mobile, tablet, and desktop widths against AC-06.

### Accessibility approach

- Every input has an explicit `<label>`.
- Error messages are associated with their fields through `aria-describedby`.
- Invalid fields receive `aria-invalid="true"`.
- Dynamic feedback is announced through an appropriate live region.
- On blocked submission, focus moves to the first invalid field.
- The form remains keyboard-operable.
- Exact accessibility conformance level and message wording are not specified and remain open questions.

## 8. Security Strategy

Client-side validation is a usability control, not a security boundary. If account creation is implemented later, the same rules must be enforced server-side.

Required practices for this scope:

- Use `type="password"` for the password field.
- Use `autocomplete="new-password"` for signup password entry and an appropriate username autocomplete value.
- Do not log username or password values.
- Do not place password values in URLs, storage, analytics, or error messages.
- Do not render validation messages with `innerHTML`; use text nodes or `textContent`.
- Do not use inline event handlers or third-party scripts.
- Keep the page free of runtime dependencies.
- If a Content Security Policy is deployed, ensure it permits only the required static assets.
- Do not echo submitted password text in validation feedback.
- Do not add a show/hide password toggle until it is explicitly approved.

## 9. Testing Strategy

### Unit tests

Test `ValidationEngine` independently of the DOM using controlled rule fixtures:

- valid username/password result,
- invalid username result,
- invalid password result,
- both invalid,
- returned error messages are associated with the correct field,
- no unapproved default rules are embedded in the engine.

Specific pass/fail cases for real username and password rules remain blocked until those rules are confirmed.

### DOM and integration tests

Using a browser test harness:

- page renders username and password fields,
- invalid submission is blocked and feedback appears,
- feedback is associated with the relevant field,
- `aria-invalid` and described-by relationships update correctly,
- first invalid field receives focus after blocked submission,
- valid submission invokes the submission seam,
- password values are not present in error text,
- no network request occurs.

### Responsive tests

Check representative mobile, tablet, and desktop viewport widths:

- no horizontal scrolling is required to access fields or the submit action,
- labels and controls remain usable,
- validation messages do not break layout,
- the primary action remains accessible.

Exact viewport matrix and breakpoints require confirmation.

### Accessibility tests

- keyboard-only traversal,
- visible labels and programmatic labels,
- error association and announcement,
- focus behavior after invalid submission,
- contrast and target-size checks against the agreed accessibility target.

The formal conformance target is still an open question.

### Security checks

- static inspection for storage, logging, analytics, and network calls,
- no `innerHTML` use for user-derived or message content,
- no third-party runtime dependencies,
- no password values in URLs or rendered messages.

### Acceptance traceability

| Acceptance criterion | Coverage | Status |
|---|---|---|
| AC-01 Signup page renders | HTML smoke test / DOM test | Testable now |
| AC-02 Username validation feedback | Validation engine + renderer tests | Blocked by agreed username rules |
| AC-03 Password validation feedback | Validation engine + renderer tests | Blocked by agreed password rules |
| AC-04 Invalid submission blocked | Controller submit tests | Testable with representative invalid fixtures |
| AC-05 Valid submission passes validation | Controller + submission seam tests | Blocked by agreed rules and post-submit behavior |
| AC-06 Responsive layout | Manual/automated viewport checks | Testable; viewport matrix to confirm |
| AC-07 Technology constraint | Dependency and source review | Testable now |

## 10. Open Decisions and Implementation Gates

The following must be confirmed before final implementation and sign-off:

1. Exact username validation rules.
2. Exact password validation rules.
3. Validation trigger policy: on input, blur, submit, or a combination.
4. Post-validation submission behavior.
5. Whether a password show/hide toggle is required.
6. Required browser support matrix.
7. Required accessibility conformance target and validation-message wording constraints.

Until items 1 and 2 are approved, the validation policy module remains intentionally incomplete. No username or password length, character, complexity, trimming, or normalization rules should be treated as approved by this document.
# Review Findings

**Review type:** Static review of the supplied source files. I did not execute the application or tests.

## Verdict

The solution is well structured: responsibilities are separated cleanly, messages are rendered with `textContent`, the form has useful ARIA relationships, and the CSS is genuinely responsive rather than fixed-width. It also correctly avoids inventing detailed username/password rules.

It is **not release-ready** in its current state, primarily because:

1. JavaScript failure/disablement can cause a native GET submission with the password in the URL.
2. Username/password validation is still only a presence check and accepts whitespace-only values.
3. Form-level validation status can become stale or be cleared while another field is invalid.
4. Some validation-policy and test coverage gaps remain.

## Findings

| ID | Severity | Area | Finding |
|---|---|---|---|
| F-01 | **High** | Security / fallback behavior | Native form submission can expose the password in the URL if JavaScript is unavailable or initialization fails. |
| F-02 | **High** | Missing requirement / validation policy | Presence-only validation accepts whitespace-only values; exact approved rules are still unconfirmed. |
| F-03 | **Medium** | Validation UX / correctness | The form status region can become stale or be cleared while another field is still invalid. |
| F-04 | **Medium** | Validation correctness | `pattern.test()` is stateful for regexes using `g` or `y`, causing inconsistent validation results. |
| F-05 | **Medium** | Maintainability / validation safety | A partial rule configuration silently disables validation for the omitted field. |
| F-06 | **Medium** | Architecture conformance | The validation trigger policy is hard-coded even though it is an open decision and the architecture called for configurability. |
| F-07 | **Medium** | Test coverage | Tests do not exercise the actual `index.html`/`app.js` integration or several failure modes. |
| F-08 | **Low** | UX semantics | The “no submission endpoint” message uses the success-colored `info` tone. |
| F-09 | **Low** | Accessibility maintainability | `aria-describedby` is overwritten rather than extended, which will discard future help text. |
| F-10 | **Low / verify** | Accessibility | Input boundary contrast may not meet a 3:1 non-text contrast target; the accessibility target is unconfirmed. |

---

### F-01 — High — Password can be submitted in the URL when JavaScript is unavailable or fails

**Evidence**

In `index.html`, the form is:

```html
<form id='signup-form' class='form' novalidate>
```

and the inputs have:

```html
name='username'
name='password'
```

There is no `method` or `action`. The only submission interception is added by `FormController` in JavaScript.

If JavaScript is disabled, fails to load, or `app.js` exits early, the browser performs a native submission. With no `method`, the default is `GET`; with no `action`, the target is the current URL. Because `novalidate` is present, native `required` validation is bypassed. The result can be a navigation such as:

```text
index.html?username=...&password=...
```

This contradicts both the architecture’s security strategy and the README claim that password values are never written to URLs.

**Impact**

- Password exposure in browser history, server logs, proxy logs, and copied URLs.
- Invalid forms can be submitted natively when JavaScript is not running.
- The `noscript` warning does not prevent submission.

**Correction**

- Add `method="post"` as a baseline so credentials are not placed in the query string.
- Do not rely on `method="post"` alone; the form would still submit credentials to the current URL if JavaScript fails.
- Since this implementation does not use native form submission, remove the `name` attributes or disable native form controls until JavaScript initialization succeeds.
- Update the `noscript` fallback so it does not leave a working submit path.
- Add a manual test: disable JavaScript or block `app.js`, submit the form, and verify that no password appears in the URL or navigation.
- Update the README statement that password values are never written to URLs.

---

### F-02 — High — Validation remains unconfirmed and accepts whitespace-only values

**Evidence**

`assets/js/validation-rules.js` defines:

```js
var DEFAULT_CONFIG = Object.freeze({
  username: Object.freeze({ required: true }),
  password: Object.freeze({ required: true })
});
```

and required validation is implemented as:

```js
if (config.required && text.length === 0) {
```

That means `"   "` is considered present and valid for both fields. The README explicitly acknowledges this.

**Impact**

- A username or password consisting only of spaces passes validation.
- AC-02, AC-03, and AC-05 cannot be verified against “agreed validation rules” because those rules are still undefined.
- There is no minimum password length, maximum length, character policy, or whitespace handling. This is not production-appropriate for a signup form.
- The implementation is honest about the missing rules, but it is still a release blocker.

**Correction**

- Obtain and record the approved username and password rules before release.
- Implement those rules in `validation-rules.js`.
- At minimum, decide whether whitespace should be trimmed or rejected. A signup form should almost certainly reject whitespace-only values.
- Add tests for:
  - `"   "` username,
  - `"   "` password,
  - leading/trailing whitespace,
  - empty values,
  - minimum and maximum lengths,
  - any confirmed pattern rules.
- If the stakeholder explicitly approves presence-only validation, document that whitespace-only values are intentionally accepted. Do not present the current implementation as production-ready without that sign-off.

---

### F-03 — Medium — Form-level status can become stale or be cleared while another field is invalid

**Evidence**

`FormController.validateField()` updates the global status region for a single field:

```js
if (announce) {
  if (error) {
    this.renderer.setStatus(error, 'error');
  } else {
    this.renderer.clearStatus();
  }
}
```

However, input revalidation after a field is touched calls:

```js
self.validateField(field, false);
```

This clears the field-level error but does not update or clear the form-level status.

**Observed scenarios from static inspection**

1. Submit with both fields empty. The form status says “Please correct the highlighted fields.”
2. Correct both fields without blurring or submitting again. Both field errors clear, but the form status still says “Please correct the highlighted fields.”
3. Submit with both fields empty, then correct the username and blur it while the password remains invalid. The username is valid, so `clearStatus()` runs and removes the summary even though the password is still invalid.

**Impact**

The status region can contradict the actual form state. Since it is an ARIA live region, this is also an accessibility concern.

**Correction**

Derive form-level status from the overall validation result rather than from a single field. For example:

- On any validation update, recompute all field errors.
- If any field is invalid, set the form summary appropriately.
- If all fields are valid, clear the summary.
- Avoid using the same live region for both per-field announcements and the form summary unless the message is always derived from the complete form state.

Add tests for both stale-status scenarios above.

---

### F-04 — Medium — `pattern` validation is stateful with global or sticky regexes

**Evidence**

In `assets/js/validation-rules.js`:

```js
if (config.pattern instanceof RegExp && text.length > 0 && !config.pattern.test(text)) {
```

`RegExp.prototype.test()` advances `lastIndex` when the regex has the `g` or `y` flag. Repeated calls can therefore alternate between matching and not matching.

**Impact**

Once a pattern rule is configured with `/g` or `/y`, validation may pass or fail inconsistently for the same input.

**Correction**

- Reject regexes with `g` or `y` in rule configuration, or
- Reset `lastIndex` before testing, or
- Create a non-global clone of the regex for validation.

Add a test that calls the same pattern validator multiple times with a `/g` regex.

---

### F-05 — Medium — A partial rule configuration silently disables validation for a field

**Evidence**

`createRuleSet()` uses:

```js
var configValue = config || DEFAULT_CONFIG;
```

If a caller supplies a partial configuration such as:

```js
{
  username: { required: true }
}
```

then `configValue.password` is `undefined`. `validateField()` receives `{}` as the rules object and returns `null`, meaning the password is treated as valid.

**Impact**

A future configuration mistake can silently disable password validation. This is especially risky because the form is supposed to validate both fields.

**Correction**

- Validate that both `username` and `password` configurations exist, or
- Merge supplied configuration over a safe default that keeps both fields required, or
- Fail fast when the rule configuration is incomplete.

Add a test for a missing password or username rule entry.

---

### F-06 — Medium — Validation trigger policy is hard-coded despite being an open decision

**Evidence**

`FormController.bind()` hard-codes:

- validation on `blur`,
- validation on `input` only after the field has been touched,
- validation on submit.

The architecture says the trigger policy is an open question and that the controller should support trigger configuration rather than scattering event logic.

**Impact**

The implementation chooses a policy without confirmation. If the approved policy differs, the controller must be edited rather than reconfigured.

**Correction**

- Introduce a `validationTriggers` or `triggerPolicy` option, or
- Record the blur + touched-input + submit policy as an approved decision.

Add tests for the confirmed trigger behavior.

---

### F-07 — Medium — Tests do not exercise the actual page integration or critical failure modes

**Evidence**

`tests/dom-test.html` creates its own test form and loads controller modules, but it does not load `index.html` or `app.js`. Therefore the tests do not verify:

- the real page’s markup and script order,
- the real `DEFAULT_CONFIG`,
- the real `SubmissionHandler` wiring,
- the real `signup:validated` event payload,
- the actual “no submission endpoint” status message.

The tests also omit:

- whitespace-only validation,
- stale form status after corrections,
- blur/input trigger behavior,
- global/sticky regex patterns,
- partial rule configurations,
- JavaScript-disabled or initialization-failure fallback behavior.

**Correction**

- Add a browser integration test that loads the actual page or an iframe of `index.html`.
- Add tests for the validation-policy edge cases above.
- Add a manual test for JavaScript disabled or `app.js` blocked.
- Add a test that `signup:validated` contains only the username and does not contain the password.

---

### F-08 — Low — “No submission endpoint” is styled as success

**Evidence**

`app.js` calls:

```js
renderer.setStatus(app.Messages.submissionNotConfigured, 'info');
```

In `form.css`:

```css
.form__status[data-tone='info'] {
  color: var(--color-success);
}
```

**Impact**

The message “Validation passed. No submission endpoint is configured.” is presented with the success color. It is also developer-facing rather than user-facing.

**Correction**

Use a neutral or warning tone for an unconfigured submission path, or replace the message with the approved post-submit behavior. Do not present an incomplete submission flow as a success state.

---

### F-09 — Low — `aria-describedby` is overwritten rather than extended

**Evidence**

In `FeedbackRenderer.renderField()`:

```js
if (error && error.id) {
  input.setAttribute('aria-describedby', error.id);
}
```

This replaces any existing `aria-describedby` value.

**Impact**

If help text or another description is added later, it will be silently removed when validation runs.

**Correction**

- Preserve existing description IDs and append the error ID, or
- Manage the complete description list explicitly.

---

### F-10 — Low / verify — Input boundary contrast may not meet a 3:1 non-text contrast target

**Evidence**

The light-mode input border uses:

```css
--color-border-strong: #aab4c8;
```

against a white input background. This appears below a 3:1 contrast ratio. Dark mode uses `#46546e` against `#101827`, which may also be borderline.

The approved requirements do not specify an accessibility conformance target, so this is not a confirmed violation.

**Correction**

Verify the input boundary against the eventually approved accessibility target. If WCAG 1.4.11 or equivalent is required, adjust the border token or add another non-color boundary indicator.

---

## Missing / Unverifiable Requirements

| Requirement | Status |
|---|---|
| FR-04 / FR-05: Validate username and password using agreed rules | **Blocked.** Exact rules are not confirmed; implementation uses presence-only validation. |
| AC-02 / AC-03: Show feedback for invalid username/password | Implemented structurally, but cannot be verified against agreed rules. |
| AC-05: Valid submission is ready for agreed submission behavior | **Partial.** The submission seam exists, but post-submit behavior is unspecified. The page shows a neutral message and dispatches `signup:validated`. |
| AC-06: Responsive layout | CSS is fluid and uses clamps/relative sizing; manual viewport verification is still required. |
| NFR-05: Browser and accessibility targets | Still open; README says modern evergreen browsers. |

## Positive Observations

- Clear separation between controller, validation engine, rules, messages, renderer, and submission handler.
- No external runtime dependencies or third-party scripts.
- No `innerHTML`; messages and status text are rendered with `textContent`.
- No evidence of storage, analytics, URL manipulation, or application logging of passwords.
- Password field uses `type='password'` and `autocomplete='new-password'`.
- The `signup:validated` event includes only the username, not the password.
- Labels, error associations, `aria-invalid`, live-region status, and focus-the-first-invalid-field behavior are present.
- Responsive CSS uses fluid widths, `clamp()`, and avoids fixed form widths.

## Recommended Correction Order

1. **Fix F-01 first:** remove the JavaScript-failure password-in-URL path.
2. **Resolve F-02:** obtain and implement the approved username/password rules.
3. **Fix F-03:** derive form status from the complete validation result.
4. **Harden validation policy:** address F-04 and F-05.
5. **Confirm F-06:** document or configure the validation trigger policy.
6. **Expand tests:** close F-07 with actual-page and failure-mode coverage.
7. **Address low-severity items:** F-08, F-09, and verify F-10.
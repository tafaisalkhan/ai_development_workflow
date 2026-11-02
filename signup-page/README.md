# Responsive Signup Page

A static signup page built with HTML, CSS, and JavaScript only. It includes username and password fields, client-side validation feedback, blocked invalid submission, and a responsive layout for mobile, tablet, and desktop.

## Important: validation rules are pending confirmation

The approved requirements do not define username or password validation rules. To avoid inventing rules, the default policy in `assets/js/validation-rules.js` enforces **presence only**:

- the username must not be empty;
- the password must not be empty.

No length, character, complexity, trimming, or normalization rules are applied by default. Whitespace-only values therefore count as present until confirmed rules define otherwise.

Before production use, replace `DEFAULT_CONFIG` in `assets/js/validation-rules.js` with the approved rules. The rule factory supports optional `required`, `minLength`, `maxLength`, and `pattern` (RegExp) constraints, and new constraints can be added without changing the form controller.

The post-validation submission behavior is also unspecified. The app does not call a backend. A valid form invokes a configurable submission handler that shows a neutral message and dispatches a `signup:validated` event containing only the username. Edit the handler in `assets/js/app.js` when the approved behavior is known.

## Run locally

No build step or package install is required.

### Option 1: open the file directly

Open `index.html` in a modern browser. The scripts are classic scripts, so the page works from the local filesystem.

### Option 2: serve with a local static server (recommended)

From the project root:

```sh
python3 -m http.server 8000
```

Then open <http://localhost:8000>.

Any static file server works. No server-side code is used.

## Tests

### Validation unit tests

Run with Node.js:

```sh
node tests/validation-engine.test.js
```

### DOM tests

Open `tests/dom-test.html` in a browser, or serve the project and visit `/tests/dom-test.html`. Results are rendered on the page and logged to the console.

These tests cover field rendering, invalid submission blocking, feedback association, focus behavior, valid submission delegation, and the requirement that password values are not rendered in feedback.

### Manual checks

See `tests/manual-test-plan.md` for responsive, keyboard, and accessibility checks.

## Project structure

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

## Behavior summary

- Validation runs on blur and on submit. After a field has been touched, it also revalidates on input so errors clear promptly.
- Invalid submission is blocked, field errors are shown, `aria-invalid` is updated, and focus moves to the first invalid field.
- Messages are written with `textContent`; no `innerHTML` is used.
- Password values are never written to storage, URLs, logs, analytics, or validation messages.
- The password field uses `type='password'` and `autocomplete='new-password'`. There is no show/hide toggle because it was not approved.

## Browser support

Modern evergreen browsers with support for `CustomEvent`, `Element.closest`, and CSS custom properties are expected. The implementation uses classic scripts, so no module server is required. Specific browser and accessibility conformance targets remain to be confirmed.

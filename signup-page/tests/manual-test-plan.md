# Manual Test Plan

## Scope

These checks cover the responsive signup page at representative mobile, tablet, and desktop widths, plus keyboard and accessibility behavior. Exact validation rules and browser conformance targets remain pending confirmation.

## Responsive checks

- At 320 px width, the username field, password field, and primary action are visible without horizontal scrolling.
- At 375 px, 768 px, 1024 px, and 1440 px widths, the form remains centered and usable.
- Validation messages do not overflow or cause horizontal scrolling.
- The submit button remains reachable and at least 44 px tall on touch screens.
- Text remains readable without zooming at the default mobile width.

## Validation checks

- Submitting with both fields empty shows feedback for both fields and focuses the username field.
- Submitting with only the password empty shows feedback for the password and focuses the password field.
- Correcting an invalid field after it has been touched clears its error.
- Submitting valid values invokes the configured submission handler.
- The neutral post-validation message clearly states that no submission endpoint is configured.
- Password text never appears in error messages or status messages.

## Keyboard checks

- Tab moves through the username field, password field, and submit button in order.
- Shift+Tab reverses the order.
- Enter while focus is in a field submits the form and triggers validation.
- Focus is visibly indicated on inputs and the button.

## Accessibility checks

- Each input has a visible and programmatically associated label.
- Invalid inputs receive `aria-invalid='true'`.
- Error messages are associated with their inputs through `aria-describedby`.
- The status region is announced by a screen reader when submission is blocked and when validation passes.
- Focus moves to the first invalid field after a blocked submission.
- The page is usable at 200 percent browser zoom.
- Color is not the only indicator of an invalid field; the error message is also rendered.

## Security checks

- No password value appears in `localStorage`, `sessionStorage`, cookies, URLs, or console output.
- No network request is made by the page.
- No `innerHTML` is used to render validation messages.
- No third-party runtime scripts are loaded.

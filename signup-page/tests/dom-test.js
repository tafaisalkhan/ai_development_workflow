'use strict';

(function () {
  var app = window.SignupApp;
  var results = document.getElementById('test-results');
  var summary = document.getElementById('test-summary');
  var total = 0;
  var failures = 0;
  var controllers = [];

  function record(name, ok, detail) {
    var li = document.createElement('li');
    li.className = ok ? 'pass' : 'fail';
    li.textContent = (ok ? 'PASS ' : 'FAIL ') + name + (detail ? ' - ' + detail : '');
    results.appendChild(li);
  }

  function assert(condition, message) {
    if (!condition) {
      throw new Error(message || 'Assertion failed.');
    }
  }

  function assertEqual(actual, expected, message) {
    if (actual !== expected) {
      throw new Error((message || 'Values differ.') + ' Expected: ' + JSON.stringify(expected) + ' Actual: ' + JSON.stringify(actual));
    }
  }

  function resetForm() {
    var form = document.getElementById('test-form');
    form.reset();

    Array.prototype.forEach.call(form.querySelectorAll('[data-error-for]'), function (node) {
      node.textContent = '';
      node.hidden = true;
    });

    Array.prototype.forEach.call(form.querySelectorAll('[data-field]'), function (node) {
      node.setAttribute('aria-invalid', 'false');
    });

    var status = form.querySelector('[data-form-status]');
    if (status) {
      status.textContent = '';
      status.removeAttribute('data-tone');
    }

    return form;
  }

  function build(options) {
    var opts = options || {};
    var form = resetForm();
    var renderer = app.FeedbackRenderer.create(form, { invalidSummary: app.Messages.formInvalid });
    var submitted = [];
    var handler = app.SubmissionHandler.create({
      onSubmit: function (values) {
        submitted.push(values);
      }
    });

    var controller = new app.FormController({
      form: form,
      ruleSet: opts.ruleSet || app.ValidationRules.createRuleSet(app.ValidationRules.DEFAULT_CONFIG, app.Messages),
      renderer: renderer,
      submissionHandler: opts.submissionHandler || handler,
      engine: app.ValidationEngine
    });

    controllers.push(controller);

    return {
      form: form,
      renderer: renderer,
      controller: controller,
      submitted: submitted,
      submit: function () {
        return form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
      }
    };
  }

  function test(name, fn) {
    total += 1;
    try {
      fn();
      record(name, true);
    } catch (error) {
      failures += 1;
      record(name, false, error.message);
      console.error(error);
    } finally {
      controllers.forEach(function (controller) {
        controller.destroy();
      });
      controllers = [];
      resetForm();
    }
  }

  test('the signup form renders username and password fields', function () {
    assert(document.getElementById('test-username'), 'Username field is missing.');
    assert(document.getElementById('test-password'), 'Password field is missing.');
    assertEqual(document.getElementById('test-password').type, 'password', 'Password field must use type=password.');
  });

  test('invalid submission is blocked and shows a message for each field', function () {
    var ui = build();
    var notCancelled = ui.submit();

    assertEqual(notCancelled, false, 'Submit event should be cancelled when invalid.');
    assertEqual(ui.submitted.length, 0, 'Submission handler must not run when invalid.');

    var usernameError = document.getElementById('test-username-error');
    var passwordError = document.getElementById('test-password-error');

    assert(usernameError.textContent.length > 0, 'Username error message is missing.');
    assert(passwordError.textContent.length > 0, 'Password error message is missing.');
    assertEqual(usernameError.hidden, false, 'Username error should be visible.');
    assertEqual(passwordError.hidden, false, 'Password error should be visible.');
  });

  test('invalid fields are marked with aria-invalid and described by their errors', function () {
    var ui = build();
    ui.submit();

    var username = document.getElementById('test-username');
    var password = document.getElementById('test-password');

    assertEqual(username.getAttribute('aria-invalid'), 'true');
    assertEqual(password.getAttribute('aria-invalid'), 'true');
    assert(username.getAttribute('aria-describedby').indexOf('test-username-error') !== -1, 'Username error association is missing.');
    assert(password.getAttribute('aria-describedby').indexOf('test-password-error') !== -1, 'Password error association is missing.');
  });

  test('the first invalid field receives focus after a blocked submission', function () {
    var ui = build();
    ui.submit();
    assertEqual(document.activeElement, document.getElementById('test-username'), 'Username field should receive focus.');
  });

  test('only the invalid password is focused when the username is valid', function () {
    var ui = build();
    document.getElementById('test-username').value = 'alice';
    ui.submit();
    assertEqual(document.activeElement, document.getElementById('test-password'), 'Password field should receive focus.');
  });

  test('a valid submission invokes the submission handler with the field values', function () {
    var ui = build();
    document.getElementById('test-username').value = 'alice';
    document.getElementById('test-password').value = 'secret';

    var notCancelled = ui.submit();

    assertEqual(notCancelled, false, 'The controller still intercepts the native submit event.');
    assertEqual(ui.submitted.length, 1, 'Submission handler should run once.');
    assertEqual(ui.submitted[0].username, 'alice');
    assertEqual(ui.submitted[0].password, 'secret');
  });

  test('field errors clear after a valid submission', function () {
    var ui = build();
    document.getElementById('test-username').value = 'alice';
    document.getElementById('test-password').value = 'secret';
    ui.submit();

    assertEqual(document.getElementById('test-username-error').hidden, true, 'Username error should be hidden.');
    assertEqual(document.getElementById('test-password-error').hidden, true, 'Password error should be hidden.');
  });

  test('password values are not rendered in feedback', function () {
    var secret = 'super-secret-value';
    var ui = build({
      ruleSet: {
        validateUsername: function () { return null; },
        validatePassword: function () { return 'Password is invalid.'; }
      }
    });

    document.getElementById('test-password').value = secret;
    ui.submit();

    var feedback = ui.form.textContent;
    assert(feedback.indexOf(secret) === -1, 'Password value must not appear in rendered feedback.');
  });

  summary.textContent = total + ' test(s), ' + failures + ' failure(s).';
  summary.setAttribute('data-tone', failures ? 'error' : 'info');
})();

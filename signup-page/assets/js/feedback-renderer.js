(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.SignupApp = root.SignupApp || {};
    root.SignupApp.FeedbackRenderer = factory();
  }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  function create(root, options) {
    if (!root) {
      throw new Error('FeedbackRenderer requires a root element.');
    }

    var opts = options || {};
    var invalidSummary = opts.invalidSummary || 'Please correct the highlighted fields.';
    var statusElement = root.querySelector('[data-form-status]');

    function getFieldElement(field) {
      return root.querySelector('[data-field="' + field + '"]');
    }

    function getErrorElement(field) {
      return root.querySelector('[data-error-for="' + field + '"]');
    }

    function setStatus(message, tone) {
      if (!statusElement) return;
      statusElement.textContent = message || '';
      if (tone) {
        statusElement.setAttribute('data-tone', tone);
      } else {
        statusElement.removeAttribute('data-tone');
      }
    }

    function clearStatus() {
      setStatus('', null);
    }

    function renderField(field, message) {
      var input = getFieldElement(field);
      var error = getErrorElement(field);
      var hasError = typeof message === 'string' && message.length > 0;

      if (input) {
        input.setAttribute('aria-invalid', hasError ? 'true' : 'false');
        if (error && error.id) {
          input.setAttribute('aria-describedby', error.id);
        }
      }

      if (error) {
        error.textContent = hasError ? message : '';
        error.hidden = !hasError;
      }
    }

    function renderResult(result) {
      var errors = result && result.errors ? result.errors : { username: null, password: null };

      renderField('username', errors.username);
      renderField('password', errors.password);

      if (result && !result.valid) {
        setStatus(invalidSummary, 'error');
      } else {
        clearStatus();
      }

      return result;
    }

    function clear() {
      renderField('username', null);
      renderField('password', null);
      clearStatus();
    }

    function focusField(field) {
      var input = getFieldElement(field);
      if (input) input.focus();
    }

    return {
      getFieldElement: getFieldElement,
      getErrorElement: getErrorElement,
      setStatus: setStatus,
      clearStatus: clearStatus,
      renderField: renderField,
      renderResult: renderResult,
      clear: clear,
      focusField: focusField
    };
  }

  return { create: create };
});

(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.SignupApp = root.SignupApp || {};
    root.SignupApp.ValidationEngine = factory();
  }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  function validate(values, ruleSet) {
    if (!ruleSet || typeof ruleSet.validateUsername !== 'function' || typeof ruleSet.validatePassword !== 'function') {
      throw new TypeError('A rule set with validateUsername and validatePassword is required.');
    }

    var input = values || {};
    var usernameValue = typeof input.username === 'string' ? input.username : '';
    var passwordValue = typeof input.password === 'string' ? input.password : '';

    var usernameError = ruleSet.validateUsername(usernameValue);
    var passwordError = ruleSet.validatePassword(passwordValue);

    var errors = {
      username: usernameError || null,
      password: passwordError || null
    };

    return {
      valid: errors.username === null && errors.password === null,
      errors: errors
    };
  }

  return { validate: validate };
});

(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.SignupApp = root.SignupApp || {};
    root.SignupApp.ValidationRules = factory();
  }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  /*
   * Default validation policy.
   *
   * The approved requirements do not define username or password rules. The
   * only default applied here is presence, so the page can demonstrate
   * validation without inventing unapproved length, character, or complexity
   * rules. Replace this configuration with the confirmed rules before
   * production use.
   */
  var DEFAULT_CONFIG = Object.freeze({
    username: Object.freeze({ required: true }),
    password: Object.freeze({ required: true })
  });

  function getMessage(messages, key, fallback) {
    if (messages && typeof messages[key] === 'string' && messages[key].length > 0) {
      return messages[key];
    }
    return fallback;
  }

  function validateField(field, value, rules, messages) {
    var text = typeof value === 'string' ? value : '';
    var config = rules || {};
    var noun = field === 'username' ? 'username' : 'password';

    if (config.required && text.length === 0) {
      return getMessage(messages, field + 'Required', 'Enter a ' + noun + '.');
    }

    if (typeof config.minLength === 'number' && text.length > 0 && text.length < config.minLength) {
      return getMessage(messages, field + 'MinLength', 'The ' + noun + ' is too short.');
    }

    if (typeof config.maxLength === 'number' && text.length > config.maxLength) {
      return getMessage(messages, field + 'MaxLength', 'The ' + noun + ' is too long.');
    }

    if (config.pattern instanceof RegExp && text.length > 0 && !config.pattern.test(text)) {
      return getMessage(messages, field + 'Pattern', 'The ' + noun + ' format is not valid.');
    }

    return null;
  }

  function createRuleSet(config, messages) {
    var configValue = config || DEFAULT_CONFIG;

    return {
      validateUsername: function (value) {
        return validateField('username', value, configValue.username, messages);
      },
      validatePassword: function (value) {
        return validateField('password', value, configValue.password, messages);
      }
    };
  }

  return {
    DEFAULT_CONFIG: DEFAULT_CONFIG,
    createRuleSet: createRuleSet,
    validateField: validateField
  };
});

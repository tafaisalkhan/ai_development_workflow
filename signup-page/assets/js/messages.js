(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.SignupApp = root.SignupApp || {};
    root.SignupApp.Messages = factory();
  }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  return {
    usernameRequired: 'Enter a username.',
    passwordRequired: 'Enter a password.',
    usernameMinLength: 'The username is too short.',
    passwordMinLength: 'The password is too short.',
    usernameMaxLength: 'The username is too long.',
    passwordMaxLength: 'The password is too long.',
    usernamePattern: 'The username format is not valid.',
    passwordPattern: 'The password format is not valid.',
    formInvalid: 'Please correct the highlighted fields.',
    submissionNotConfigured: 'Validation passed. No submission endpoint is configured.'
  };
});

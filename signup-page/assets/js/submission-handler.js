(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.SignupApp = root.SignupApp || {};
    root.SignupApp.SubmissionHandler = factory();
  }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  function create(options) {
    var opts = options || {};

    return {
      submit: function (values) {
        if (typeof opts.onSubmit === 'function') {
          return opts.onSubmit(values);
        }
        return undefined;
      }
    };
  }

  return { create: create };
});

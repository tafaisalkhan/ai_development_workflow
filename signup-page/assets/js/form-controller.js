(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.SignupApp = root.SignupApp || {};
    root.SignupApp.FormController = factory();
  }
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  var FIELDS = ['username', 'password'];

  function FormController(options) {
    var opts = options || {};

    if (!opts.form || !opts.ruleSet || !opts.renderer || !opts.submissionHandler || !opts.engine) {
      throw new Error('FormController requires form, ruleSet, renderer, submissionHandler, and engine.');
    }

    this.form = opts.form;
    this.ruleSet = opts.ruleSet;
    this.renderer = opts.renderer;
    this.submissionHandler = opts.submissionHandler;
    this.engine = opts.engine;
    this.touched = { username: false, password: false };
    this.inputs = {};
    this.listeners = [];
    this.onSubmit = null;

    this.bind();
  }

  FormController.prototype.getInput = function (field) {
    return this.inputs[field] || this.form.querySelector('[data-field="' + field + '"]');
  };

  FormController.prototype.getValues = function () {
    var username = this.getInput('username');
    var password = this.getInput('password');

    return {
      username: username ? username.value : '',
      password: password ? password.value : ''
    };
  };

  FormController.prototype.bind = function () {
    var self = this;

    FIELDS.forEach(function (field) {
      var input = self.form.querySelector('[data-field="' + field + '"]');
      if (!input) return;

      self.inputs[field] = input;

      var onBlur = function () {
        self.touched[field] = true;
        self.validateField(field, true);
      };

      var onInput = function () {
        if (self.touched[field]) {
          self.validateField(field, false);
        }
      };

      input.addEventListener('blur', onBlur);
      input.addEventListener('input', onInput);
      self.listeners.push({ input: input, type: 'blur', handler: onBlur });
      self.listeners.push({ input: input, type: 'input', handler: onInput });
    });

    this.onSubmit = function (event) {
      self.handleSubmit(event);
    };

    this.form.addEventListener('submit', this.onSubmit);
  };

  FormController.prototype.validateField = function (field, announce) {
    var values = this.getValues();
    var error = field === 'username'
      ? this.ruleSet.validateUsername(values.username)
      : this.ruleSet.validatePassword(values.password);

    error = error || null;
    this.renderer.renderField(field, error);

    if (announce) {
      if (error) {
        this.renderer.setStatus(error, 'error');
      } else {
        this.renderer.clearStatus();
      }
    }

    return error === null;
  };

  FormController.prototype.validateAll = function () {
    return this.engine.validate(this.getValues(), this.ruleSet);
  };

  FormController.prototype.handleSubmit = function (event) {
    event.preventDefault();

    this.touched.username = true;
    this.touched.password = true;

    var values = this.getValues();
    var result = this.validateAll();

    this.renderer.renderResult(result);

    if (!result.valid) {
      var firstInvalid = result.errors.username ? 'username' : 'password';
      this.renderer.focusField(firstInvalid);
      return;
    }

    this.submissionHandler.submit(values);
  };

  FormController.prototype.destroy = function () {
    this.listeners.forEach(function (listener) {
      listener.input.removeEventListener(listener.type, listener.handler);
    });

    this.listeners = [];

    if (this.onSubmit) {
      this.form.removeEventListener('submit', this.onSubmit);
      this.onSubmit = null;
    }
  };

  return FormController;
});

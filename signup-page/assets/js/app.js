(function () {
  'use strict';

  function init() {
    var app = window.SignupApp;
    if (!app) return;

    var form = document.getElementById('signup-form');
    if (!form) return;

    var ruleSet = app.ValidationRules.createRuleSet(app.ValidationRules.DEFAULT_CONFIG, app.Messages);
    var renderer = app.FeedbackRenderer.create(form, { invalidSummary: app.Messages.formInvalid });
    var submissionHandler = app.SubmissionHandler.create({
      onSubmit: function (values) {
        renderer.setStatus(app.Messages.submissionNotConfigured, 'info');
        form.dispatchEvent(new CustomEvent('signup:validated', {
          bubbles: true,
          detail: { username: values.username }
        }));
      }
    });

    new app.FormController({
      form: form,
      ruleSet: ruleSet,
      renderer: renderer,
      submissionHandler: submissionHandler,
      engine: app.ValidationEngine
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

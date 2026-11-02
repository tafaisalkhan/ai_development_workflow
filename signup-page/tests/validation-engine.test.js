'use strict';

var assert = require('assert');
var engine = require('../assets/js/validation-engine.js');
var rules = require('../assets/js/validation-rules.js');
var messages = require('../assets/js/messages.js');

var total = 0;
var failures = 0;

function test(name, fn) {
  total += 1;
  try {
    fn();
    console.log('PASS ' + name);
  } catch (error) {
    failures += 1;
    console.error('FAIL ' + name);
    console.error('  ' + error.message);
  }
}

function ruleSet(usernameError, passwordError) {
  return {
    validateUsername: function () { return usernameError || null; },
    validatePassword: function () { return passwordError || null; }
  };
}

test('valid values produce a valid result', function () {
  var result = engine.validate({ username: 'alice', password: 'secret' }, ruleSet(null, null));
  assert.strictEqual(result.valid, true);
  assert.strictEqual(result.errors.username, null);
  assert.strictEqual(result.errors.password, null);
});

test('invalid username is associated with the username field', function () {
  var result = engine.validate({ username: '', password: 'secret' }, ruleSet('Username is invalid.', null));
  assert.strictEqual(result.valid, false);
  assert.strictEqual(result.errors.username, 'Username is invalid.');
  assert.strictEqual(result.errors.password, null);
});

test('invalid password is associated with the password field', function () {
  var result = engine.validate({ username: 'alice', password: '' }, ruleSet(null, 'Password is invalid.'));
  assert.strictEqual(result.valid, false);
  assert.strictEqual(result.errors.username, null);
  assert.strictEqual(result.errors.password, 'Password is invalid.');
});

test('both invalid fields are reported together', function () {
  var result = engine.validate({ username: '', password: '' }, ruleSet('Username is invalid.', 'Password is invalid.'));
  assert.strictEqual(result.valid, false);
  assert.strictEqual(result.errors.username, 'Username is invalid.');
  assert.strictEqual(result.errors.password, 'Password is invalid.');
});

test('the engine does not add validation rules of its own', function () {
  var result = engine.validate({ username: '', password: '' }, ruleSet(null, null));
  assert.strictEqual(result.valid, true);
});

test('default rule set enforces presence only', function () {
  var set = rules.createRuleSet(rules.DEFAULT_CONFIG, messages);
  assert.strictEqual(set.validateUsername('alice'), null);
  assert.strictEqual(set.validatePassword('secret'), null);
  assert.ok(set.validateUsername(''));
  assert.ok(set.validatePassword(''));
  assert.strictEqual(set.validateUsername('user name'), null);
  assert.strictEqual(set.validatePassword('x'), null);
});

test('configured length rules are applied only when supplied', function () {
  var set = rules.createRuleSet({
    username: { required: true, minLength: 3 },
    password: { required: true, minLength: 8 }
  }, messages);

  assert.ok(set.validateUsername('ab'));
  assert.strictEqual(set.validateUsername('abc'), null);
  assert.ok(set.validatePassword('short'));
});

test('password values are not echoed in validation messages', function () {
  var set = rules.createRuleSet({
    username: { required: true },
    password: { required: true, pattern: /^[A-Z]+$/ }
  }, messages);

  var value = 'lowercase-secret';
  var message = set.validatePassword(value);

  assert.ok(message);
  assert.strictEqual(message.indexOf(value), -1);
});

test('missing rule set throws a clear error', function () {
  assert.throws(function () {
    engine.validate({}, null);
  }, /rule set/i);
});

console.log('');
console.log(total + ' test(s), ' + failures + ' failure(s)');

if (failures > 0) {
  process.exitCode = 1;
}

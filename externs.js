/**
 * @const
 */
var Twilio = {};

/**
 * @interface
 */
Twilio.Device = function() {};

/**
 * @return {Object}
 */
Twilio.Device.setup = function() {};

/**
 * @return {Object}
 */
Twilio.Device.incoming = function() {};

/**
 * @return {Object}
 */
Twilio.Device.offline = function() {};

/**
 * @return {Object}
 */
 Twilio.Device.ready = function() {};

/**
 * @return {Object}
 */
Twilio.Device.disconnect = function() {};

/**
 * @return {Object}
 */
Twilio.Device.disconnectAll = function() {};

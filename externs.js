/**
 * @const
 */
var Twilio = {};

/**
 * @interface
 */
Twilio.Connection = function() {};

/**
 * @return {Object}
 */
Twilio.Connection.accept = function() {};

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

/**
* @return {Object}
*/
Twilio.Device.sendDigits = function() {};

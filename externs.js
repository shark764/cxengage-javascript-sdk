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

/**
 * @const
 */
var ZAFClient = {};
var client = {};
var modalClient = {}

/**
* @return {Object}
*/
client.postMessage = function() {};

/**
* @return {Object}
*/
client.invoke = function() {};

/**
* @return {Object}
*/
client.get = function() {};

/**
* @return {Object}
*/
client.on = function() {};

/**
* @return {Object}
*/
client.request = function() {};

/**
* @return {Object}
*/
client.instance = function() {};

/**
 * @return {Object}
 */
ZAFClient.init = function() {};

/**
* @return {Object}
*/
modalClient.trigger = function() {};

/**
* @return {Object}
*/
modalClient.on = function() {};

/**
 * @const
 */
var sforce = {};

/**
* @interface
*/
sforce.console = function() {};

/**
* @interface
*/
sforce.interaction = function() {};

/**
* @interface
*/
sforce.interaction.cti = function() {};

/**
* @return {Object}
*/
sforce.interaction.cti.setSoftphoneHeight = function() {};

/**
* @return {Object}
*/
sforce.interaction.cti.setSoftphoneWidth = function() {};

/**
* @return {Object}
*/
sforce.interaction.isVisible = function() {};

/**
* @return {Object}
*/
sforce.interaction.setVisible = function() {};

/**
* @return {Object}
*/
sforce.interaction.getPageInfo = function() {};

/**
* @return {Object}
*/
sforce.console.focusPrimaryTabById = function() {};

/**
* @return {Object}
*/
sforce.interaction.cti.disableClickToDial = function() {};

/**
* @return {Object}
*/
sforce.interaction.cti.enableClickToDial = function() {};

/**
* @return {Object}
*/
sforce.interaction.screenPop = function() {};

/**
* @return {Object}
*/
sforce.console.closeTab = function() {};

/**
* @return {Object}
*/
sforce.interaction.searchAndScreenPop = function() {};

/**
* @return {Object}
*/
sforce.interaction.cti.onClickToDial = function() {};

/**
* @return {Object}
*/
sforce.interaction.onFocus = function() {};

/**
* @interface
*/
sforce.opencti = function() {};

/**
* @return {Object}
*/
sforce.opencti.cti.setSoftphonePanelHeight = function() {};

/**
* @return {Object}
*/
sforce.opencti.cti.setSoftphonePanelWidth = function() {};

/**
* @return {Object}
*/
sforce.opencti.setSoftphonePanelVisibility = function() {};

/**
* @return {Object}
*/
sforce.opencti.isSoftphonePanelVisible = function() {};

/**
* @return {Object}
*/
sforce.opencti.getAppViewInfo = function() {};

/**
* @return {Object}
*/
sforce.opencti.disableClickToDial = function() {};

/**
* @return {Object}
*/
sforce.opencti.enableClickToDial = function() {};

/**
* @return {Object}
*/
sforce.opencti.screenPop = function() {};

/**
* @return {Object}
*/
sforce.opencti.searchAndScreenPop = function() {};

/**
* @return {Object}
*/
sforce.opencti.onClickToDial = function() {};

/**
 * @const
 */
var AWSCognito = {};

/**
* @return {Object}
*/
AWSCognito.CognitoIdentityServiceProvider = function() {};

/**
* @return {Object}
*/
AWSCognito.CognitoIdentityServiceProvider.CognitoAuth = function() {};

/**
 * @const
 */
var CognitoAuth = {};

/**
* @return {Object}
*/
CognitoAuth.prototype.parseCognitoWebResponse = function() {};

/**
* @return {Object}
*/
CognitoAuth.prototype.getSession = function() {};

/**
 * @const
 */
var CognitoAuthSession = {};

/**
* @return {Object}
*/
CognitoAuthSession.prototype.getAccessToken = function getAccessToken() {};

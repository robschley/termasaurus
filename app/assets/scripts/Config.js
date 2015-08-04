
function ConfigFactory() {

	// Initialize the configuration object.
	var Config = {};

	// Check if there is a config callback.
	if (angular.isFunction(ConfigCallback)) {

		// Load the configuration from the callback.
		Config = ConfigCallback();
	}

	return Config;
};

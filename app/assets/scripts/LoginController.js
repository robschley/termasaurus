
function LoginController($scope, Identity, LoginQueue)
{
	// Initialize the form state.
	$scope.state = {
		submitted: false
	};

	// Initialize the credentials.
	$scope.credentials = {
		username: '',
		password: ''
	};

	// Submit the form.
	$scope.submit = function()
	{
		// Update the form state.
		$scope.state.submitted = true;

		// Attempt to login with the credentials.
		Identity.login($scope.credentials);
	};

	// Monitor for the login success event.
	$scope.$on('LoginSuccess', function(event)
	{
		// Update the form state.
		$scope.state.error = null;
		$scope.state.submitted = false;

		// Process the queued requests.
		LoginQueue.process();
	});

	// Monitor for the login error event.
	$scope.$on('LoginError', function(event, error)
	{
		// Update the form state.
		$scope.state.error = error;
		$scope.state.submitted = false;
	});
}

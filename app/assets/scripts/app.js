
// Instantiate the app module.
var app = angular.module('Codetology', ['ui.router', 'restangular']);

// Configure the app state.
app.config([
  '$locationProvider', '$stateProvider', '$urlRouterProvider', function($locationProvider, $stateProvider, $urlRouterProvider) {

    // Configure the app to use push state routing.
    $locationProvider.html5Mode(true).hashPrefix('#');

    // Configure the default route.
    $urlRouterProvider.otherwise("/");

    // Configure the app states.
    $stateProvider
      .state('home', {
        url: "/",
        templateUrl: "views/home.html"
      })
  }
]);

var app = angular.module('myApp', ['auth', 'ui.bootstrap']);

app.run(function($rootScope) {
  $rootScope.sterapiServer = "https://localhost:9443"
});
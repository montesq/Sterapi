angular.module('auth', []);

function AuthCtrl($scope, $rootScope, $http) {
  $scope.currentUser = localStorage.getItem("email");

  $scope.authRequest = function() {
    navigator.id.request({
      siteName: "Steriservices",
      siteLogo: "/images/logo.png"
    });
  };

  $scope.authLogout = function() {
    navigator.id.logout();
  };

  $scope.serverVerifyAssertion = function(assertion) {
    $http.post(
      $rootScope.sterapiServer + "/auth/login",
      {assertion: assertion}
    ).success(function(data, status, headers, config) {
        localStorage.setItem("email", data.email);
        window.location.reload();
      }
    ).error(function(data, status, headers, config) {
        navigator.id.logout();
        alert("Login failure: " + data.toSource());
      }
    )
  };

  $scope.serverLogout = function() {
    $http.post(
        $rootScope.sterapiServer + "/auth/logout"
    ).success(function() {
        localStorage.removeItem("email");
        window.location.reload();
      }
    ).error(function(data, status, headers, config) {
        alert("Logout failure: " + data.toSource());
      }
    )
  }

  navigator.id.watch({
    loggedInUser: $scope.currentUser,
    onlogin: $scope.serverVerifyAssertion,
    onlogout: $scope.serverLogout
  });
}
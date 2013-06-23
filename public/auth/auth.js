angular.module('auth.service', []);

angular.module('auth', ['auth.service']);

function AuthCtrl($scope, $http) {
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
      "/auth/login",
      {assertion: assertion}
    ).success(function(data, status, headers, config) {
        localStorage.setItem("email", data.email);
        window.location.reload();
    }).error(function(data, status, headers, config) {
        navigator.id.logout();
        alert("Login failure: " + data.toSource());
    })
  };

  $scope.serverLogout = function() {
    $.ajax({
      type: 'POST',
      url: '/auth/logout', // This is a URL on your website.
      success: function(res, status, xhr) {
        localStorage.removeItem("email");
        window.location.reload(); },
      error: function(xhr, status, err) { alert("Logout failure: " + err); }
    });
  }

  navigator.id.watch({
    loggedInUser: $scope.currentUser,
    onlogin: $scope.serverVerifyAssertion,
    onlogout: $scope.serverLogout
  });
}
export const authConfig = {
    clientId: 'fitness-app',
    authorizationEndpoint: 'http://localhost:8181/realms/fitness-app/protocol/openid-connect/auth',
    tokenEndpoint: 'http://localhost:8181/realms/fitness-app/protocol/openid-connect/token',
    redirectUri: window.location.origin,
    scope: 'openid profile email',
    onRefreshTokenExpire: (event) => event.logIn(),
};

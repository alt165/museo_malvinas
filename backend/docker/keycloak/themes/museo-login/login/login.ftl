<!doctype html>
<html lang="es">
  <head>
    <meta charset="utf-8">
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${msg("loginTitle",(realm.displayName!realm.name))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.png">
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
  </head>
  <body>
    <main class="login-page">
      <section class="login-card" aria-labelledby="kc-page-title">
        <img class="login-logo" src="${url.resourcesPath}/img/logo-login.jpg" alt="Museo Malvinas">
        <h1 id="kc-page-title">Archivo Histórico del Museo Malvinas, Antartida y Atlántico Sur</h1>

        <#if message?has_content && (message.type != "warning" || !isAppInitiatedAction??)>
          <div class="alert alert-${message.type}" role="alert">
            ${kcSanitize(message.summary)?no_esc}
          </div>
        </#if>

        <form id="kc-form-login" action="${url.loginAction}" method="post">
          <div class="form-field">
            <label for="username">Usuario</label>
            <input
              id="username"
              name="username"
              type="text"
              value="${(login.username!'')}"
              autocomplete="username"
              autofocus
            >
          </div>

          <div class="form-field">
            <label for="password">Contraseña</label>
            <input
              id="password"
              name="password"
              type="password"
              autocomplete="current-password"
            >
          </div>

          <#if rememberMe?? && rememberMe>
            <label class="remember-me">
              <input id="rememberMe" name="rememberMe" type="checkbox" <#if login.rememberMe??>checked</#if>>
              ${msg("rememberMe")}
            </label>
          </#if>

          <#if credentialId??>
            <input type="hidden" name="credentialId" value="${credentialId}">
          </#if>

          <button class="login-button" name="login" id="kc-login" type="submit">
            Iniciar Sesion
          </button>
        </form>
      </section>
    </main>
  </body>
</html>

<!doctype html>
<html lang="es">
  <head>
    <meta charset="utf-8">
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Actualizar contraseña</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.png">
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
  </head>
  <body>
    <main class="login-page">
      <section class="login-card" aria-labelledby="kc-page-title">
        <img class="login-logo" src="${url.resourcesPath}/img/logo-login.jpg" alt="Museo Malvinas">
        <h1 id="kc-page-title">Archivo Histórico del Museo Malvinas, Antartida y Atlántico Sur</h1>
        <p class="login-description">Debe cambiar su contraseña para continuar.</p>

        <#if message?has_content && message.type != "info" && (message.type != "warning" || !isAppInitiatedAction??)>
          <div class="alert alert-${message.type}" role="alert">
            ${kcSanitize(message.summary)?no_esc}
          </div>
        </#if>

        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">
          <div class="form-field">
            <label for="password-new">Nueva contraseña</label>
            <input
              id="password-new"
              name="password-new"
              type="password"
              autocomplete="new-password"
              autofocus
            >
          </div>

          <div class="form-field">
            <label for="password-confirm">Confirmar nueva contraseña</label>
            <input
              id="password-confirm"
              name="password-confirm"
              type="password"
              autocomplete="new-password"
            >
          </div>

          <button class="login-button" id="kc-submit" type="submit">
            Cambiar contraseña
          </button>
        </form>
      </section>
    </main>
  </body>
</html>

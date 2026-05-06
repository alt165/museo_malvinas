<?php require_once APP . '/views/inc/header.php' ?>

<body id="body" class="body-login bg-light text-dark d-flex justify-content-center align-items-center" style="min-height: 100vh;">
    <main class="w-100" style="max-width: 400px;">
        <div class="rounded-top-4 card-header text-white text-center py-4" style="background-color: #1f9de8 !important;">
            <div class="d-flex justify-content-center align-items-center">
                <img src="<?= URL ?>/public/img/logo-museo.png" alt="Logo del museo para la página de inicio" 
                    style="width: 300px; object-fit: contain;">
                <div class="ms-3 text-start">
                </div>
            </div>
            <div class="h5"style="font-family: 'Lexend';">
                Sistema de Gestión de Archivo Histórico
            </div>
        </div>
        <form id="loginForm" class="rounded-bottom-4 shadow p-4 bg-light text-dark" method="POST" action="<?= URL ?>/auth/login">
            <div class="form-floating mb-3">
                <input type="text" name="user" class="form-control" id="floatingInput" placeholder="name@example.com">
                <label for="floatingInput">Usuario</label>
            </div>
            <div class="form-floating mb-3">
                <input type="password" name="password" class="form-control" id="floatingPassword" placeholder="Password">
                <label for="floatingPassword">Contraseña</label>
            </div>
            <!-- <div class="form-check text-start mb-3">
                <input class="form-check-input" type="checkbox" name="remember" id="remember">
                <label class="form-check-label" for="remember">Recuérdame</label>
            </div> -->
            <div>
                <button class="btn btn-primary w-100 py-2" style="border-color: #1f9de8; background-color: #1f9de8 !important;" type="submit">Iniciar sesión</button>
            </div><br>
            <div>
                <?php if (!empty($error)): ?>
                    <div class="alert alert-danger"><?= $error ?></div>
                <?php endif; ?>
            </div>
        </form>
    </main>

<?php require_once APP . '/views/inc/footer.php' ?>

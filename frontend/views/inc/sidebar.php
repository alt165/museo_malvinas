<h1 class="visually-hidden">Barra inicio</h1>
<style>
    :root {
        --sidebar-width: 225px;
    }

    /* Contenedor principal para que el sidebar y el contenido estén a la par */
    .wrapper {
        display: flex;
        width: 100%;
        align-items: stretch;
    }

    #sidebar {
        min-width: var(--sidebar-width);
        max-width: var(--sidebar-width);
        min-height: 100vh;
        transition: all 0.3s;
        background-color: #1f9de8 !important;
    }

    /* Clase para ocultar */
    #sidebar.active {
        margin-left: calc(var(--sidebar-width) * -1);
    }

    /* Ajuste para que el texto no se rompa al cerrar */
    #sidebar .nav-link {
        white-space: nowrap;
    }
</style>
<div class="wrapper">
    <nav id="sidebar" class="d-flex flex-column flex-shrink-0 p-3 text-bg-dark">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <img src="<?= URL . '/public/img/logo-museo.png' ?>" alt="Logo" width="180">
        </div>

        <hr>
        <ul class="nav nav-pills flex-column mb-auto">
            <li class="nav-item ">
                <a class="nav-link text-white" href="<?= URL ?>" aria-current="page">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor"
                        class="bi bi-house-door me-2" viewBox="0 0 16 16">
                        <path
                            d="M8.354 1.146a.5.5 0 0 0-.708 0l-6 6A.5.5 0 0 0 1.5 7.5v7a.5.5 0 0 0 .5.5h4.5a.5.5 0 0 0 .5-.5v-4h2v4a.5.5 0 0 0 .5.5H14a.5.5 0 0 0 .5-.5v-7a.5.5 0 0 0-.146-.354L13 5.793V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293L8.354 1.146zM2.5 14V7.707l5.5-5.5 5.5 5.5V14H10v-4a.5.5 0 0 0-.5-.5h-2a.5.5 0 0 0-.5.5v4H2.5v-7a.5.5 0 0 0-.146-.354l-.854-.854L8 1.914l6.5 6.5-.854.854a.5.5 0 0 0-.146.354v7z" />
                    </svg>
                    Inicio
                </a>
            </li>
            <li>
                <a class="nav-link text-white" href="<?= URL ?>/estadisticas">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor"
                        class="bi bi-graph-up-arrow me-2" viewBox="0 0 16 16">
                        <path fill-rule="evenodd"
                            d="M0 0h1v15h15v1H0zm10 3.5a.5.5 0 0 1 .5-.5h4a.5.5 0 0 1 .5.5v4a.5.5 0 0 1-1 0V4.9l-3.613 4.417a.5.5 0 0 1-.74.037L7.06 6.767l-3.656 5.027a.5.5 0 0 1-.808-.588l4-5.5a.5.5 0 0 1 .758-.06l2.609 2.61L13.445 4H10.5a.5.5 0 0 1-.5-.5" />
                    </svg>
                    Estadisticas
                </a>
            </li>


            <?php if (in_array('ver abm', $_SESSION['usuario_derechos'])) {
                echo '   
        <li>
            <a class="nav-link text-white position-relative" 
            data-bs-toggle="collapse" 
            data-bs-target="#submenuABM"
            href="#submenuABM" 
            role="button" 
            aria-expanded="false" 
            aria-controls="submenuABM">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-table me-2" viewBox="0 0 16 16">
                <path d="M0 2a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V2zm15 2h-4v3h4V4zm0 4h-4v3h4V8zm0 4h-4v3h3a1 1 0 0 0 1-1v-2zm-5 3v-3H6v3h4zm-5 0v-3H1v2a1 1 0 0 0 1 1h3zm-4-4h4V8H1v3zm0-4h4V4H1v3zm5-3v3h4V4H6zm4 4H6v3h4V8z"/>
            </svg>  
                MENU
                <span class="position-absolute end-0 me-3" id="abm-arrow">&#x25BC;</span> <!-- flecha hacia abajo -->
            </a>

            <div class="collapse ps-3" id="submenuABM">
                <ul class="btn-toggle-nav list-unstyled fw-normal pb-1 small">';
                if (in_array('editar usuarios', $_SESSION['usuario_derechos'])) {
                    echo '<li><a href="' . URL . '/usuarios" class="nav-link text-white">Usuarios</a></li>';
                }
                echo '
                    <li><a href="' . URL . '/objeto" class="nav-link text-white">Inventario de objetos</a></li>
                    <li><a href="' . URL . '/depositante" class="nav-link text-white">Depositantes</a></li>
                    <li><a href="' . URL . '/ubicaciones" class="nav-link text-white">Ubicaciones</a></li>
                    <li><a href="' . URL . '/exhibiciones" class="nav-link text-white">Muestras y Exhibiciones</a></li>
                    <li><a href="' . URL . '/veteranos" class="nav-link text-white">Registro de Veteranos</a></li>                    
                </ul>
            </div>';
            } ?>
            </li>
        </ul>
        <div class="dropup mt-auto text-center">
            <a class="nav-link text-white" href="<?= URL ?>/views/manual" target="_blank">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor"
                    class="bi bi-book me-2" viewBox="0 0 16 16">
                    <path
                        d="M1 2.828c.885-.37 2.154-.769 3.388-.893 1.33-.134 2.458.063 3.112.752v9.746c-.935-.53-2.12-.603-3.213-.493-1.18.12-2.37.461-3.287.811zm7.5-.141c.654-.689 1.782-.886 3.112-.752 1.234.124 2.503.523 3.388.893v9.923c-.918-.35-2.107-.692-3.287-.81-1.094-.111-2.278-.039-3.213.492zM8 1.783C7.015.936 5.587.81 4.287.94c-1.514.153-3.042.672-3.994 1.105A.5.5 0 0 0 0 2.5v11a.5.5 0 0 0 .707.455c.882-.4 2.303-.881 3.68-1.02 1.409-.142 2.59.087 3.223.877a.5.5 0 0 0 .78 0c.633-.79 1.814-1.019 3.222-.877 1.378.139 2.8.62 3.681 1.02A.5.5 0 0 0 16 13.5v-11a.5.5 0 0 0-.293-.455c-.952-.433-2.48-.952-3.994-1.105C10.413.809 8.985.936 8 1.783" />
                </svg>
                Manual del usuario
            </a>
            <hr>
            <a class="d-block text-white dropdown-toggle fw-bold fs-6 text-decoration-none" href="#" role="button"
                data-bs-toggle="dropdown" aria-expanded="false">
                <?= ucfirst($_SESSION['usuario_nombre']) . " " . ucfirst($_SESSION['usuario_apellido']); ?>
            </a>
            <ul class="dropdown-menu text-small shadow">
                <!-- OPCIONES DE CUENTA/PREFERENCIAS -->
                <li>
                    <a class="dropdown-item" href="<?= URL ?>/usuarios/miClave">Cambiar contraseña</a>
                </li>
                <li>
                    <button class="dropdown-item" id="toggleTheme">Cambiar tema</button>
                </li>

                <!-- SEPARADOR VISUAL PARA LA ACCIÓN DE SALIDA -->
                <li>
                    <hr class="dropdown-divider">
                </li>

                <!-- CERRAR SESIÓN -->
                <li>
                    <a class="dropdown-item text-danger" href="<?= URL ?>/auth/logout">Cerrar sesión</a>
                </li>
            </ul>
        </div>
    </nav>
    <div id="content" class="sm-0">
        <nav class="navbar navbar">
            <div class="container-fluid">
                <button type="button" id="sidebarCollapse" class="btn btn-secondary btn-sm sm-0">
                    <i class="bi"></i> ☰
                </button>
            </div>
        </nav>
    </div>


    <script>
        // Script para manejar el estado de la sidebar usando localStorage
        document.addEventListener("DOMContentLoaded", function () {
            const sidebar = document.getElementById('sidebar');
            const btn = document.getElementById('sidebarCollapse');

            // 1. Comprobar si hay una preferencia guardada
            const sidebarStatus = localStorage.getItem('sidebar-active');

            // Si estaba "activo" (oculto) en la sesión anterior, lo ocultamos
            if (sidebarStatus === 'true') {
                sidebar.classList.add('active');
            }

            // 2. Evento Click
            btn.addEventListener('click', function () {
                sidebar.classList.toggle('active');

                // Guardar la preferencia (true si está oculto, false si está visible)
                localStorage.setItem('sidebar-active', sidebar.classList.contains('active'));
            });
        });
    </script>
<!DOCTYPE html>
<html lang="es" data-bs-theme="light">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/png" href="<?= URL . '/public/img/favicon.png' ?>" sizes="32x32">
    <link rel="shortcut icon" href="<?= URL . '/public/img/favicon.png' ?>" type="image/x-icon">

    <!-- Título dinámico desde PHP -->
    <title><?php if(!empty($datos['title'])){echo $datos['title'];} else {echo 'Sin título';} ?></title>

    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">

    <!-- Estilos personalizados -->
    <link rel="stylesheet" href="<?= URL . '/public/css/sign-in.css' ?>"> <!-- Solo para la página de login -->
    <link rel="stylesheet" href="<?= URL . '/public/css/sidebars.css' ?>"> <!-- Para el sidebar -->

    <!-- jQuery -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.1/jquery.min.js"></script>
    
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">

    <!-- Select con busqueda -->
    <link href="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css" rel="stylesheet" />
    <script src="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/js/select2.min.js"></script>

    <!-- DataTables CSS y JS -->
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css">
    <script src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>
    <link rel="stylesheet" href="https://cdn.datatables.net/buttons/2.4.2/css/buttons.dataTables.min.css">
    <script src="https://cdn.datatables.net/buttons/2.4.2/js/dataTables.buttons.min.js"></script>
    <script src="https://cdn.datatables.net/buttons/2.4.2/js/buttons.html5.min.js"></script>
    <script src="https://cdn.datatables.net/buttons/2.4.2/js/buttons.print.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.2.7/pdfmake.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.2.7/vfs_fonts.js"></script>

    <!-- Fuentes -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Lexend:wght@100..900&family=Raleway:ital,wght@0,100..900;1,100..900&display=swap" rel="stylesheet">

    <meta name="theme-color" content="#712cf9">

    <?php if (isset($datos['error'])) {
    $error = $datos['error'];
    } ?>

    <style>
    .bd-placeholder-img {
        font-size:1.125rem;
        text-anchor:middle;
        -webkit-user-select:none;
        -moz-user-select:none;
        user-select:none
    }

    @media (min-width: 768px) {
        .bd-placeholder-img-lg {
            font-size:3.5rem
        }
    }

    .b-example-divider {
        width:100%;
        height:3rem;
        background-color:#0000001a;
        border:solid rgba(0,0,0,.15);
        border-width:1px 0;
        box-shadow:inset 0 .5em 1.5em #0000001a,inset 0 .125em .5em #00000026
    }

    .b-example-vr {
        flex-shrink:0;
        width:1.5rem;
        height:100vh
    }

    .bi {
        vertical-align:-.125em;
        fill:currentColor
    }

    .nav-scroller {
        position:relative;
        z-index:2;
        height:2.75rem;
        overflow-y:hidden
    }

    .nav-scroller .nav {
        display:flex;
        flex-wrap:nowrap;
        padding-bottom:1rem;
        margin-top:-1px;
        overflow-x:auto;
        text-align:center;
        white-space:nowrap;
        -webkit-overflow-scrolling:touch
    }

    .btn-bd-primary {
        --bd-violet-bg: #712cf9;
        --bd-violet-rgb: 112.520718, 44.062154, 249.437846;
        --bs-btn-font-weight: 600;
        --bs-btn-color: var(--bs-white);
        --bs-btn-bg: var(--bd-violet-bg);
        --bs-btn-border-color: var(--bd-violet-bg);
        --bs-btn-hover-color: var(--bs-white);
        --bs-btn-hover-bg: #6528e0;
        --bs-btn-hover-border-color: #6528e0;
        --bs-btn-focus-shadow-rgb: var(--bd-violet-rgb);
        --bs-btn-active-color: var(--bs-btn-hover-color);
        --bs-btn-active-bg: #5a23c8;--bs-btn-active-border-color: #5a23c8
    }

    .bd-mode-toggle {
        z-index:1500
    }

    .bd-mode-toggle .bi {
        width:1em;
        height:1em
    }

    .bd-mode-toggle .dropdown-menu .active .bi {
        display:block!important
    }
    </style>
</head>
<body>
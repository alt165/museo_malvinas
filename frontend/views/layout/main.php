<?php require_once APP . '/views/inc/header.php' ?>
    <body>
        
        <main class="d-flex flex-nowrap" style="min-height: 100vh">
            <?php require_once APP . '/views/inc/sidebar.php'; ?>
            <div class="flex-grow-1 p-3">
                <?php require_once $viewPath; ?>
            </div>
        </main>

<?php require_once APP . '/views/inc/footer.php' ?>
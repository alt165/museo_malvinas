<div class="container mt-5">
    <h2><?= $datos['title'] ?></h2>

    <?php if (!empty($datos['errores'])): ?>
        <div class="alert alert-danger">
            <ul>
                <?php foreach ($datos['errores'] as $e): ?>
                    <li><?= htmlspecialchars($e) ?></li>
                <?php endforeach ?>
            </ul>
        </div>
    <?php endif; ?>

    <form action="<?= $datos['action'] ?>" method="POST">
        <div class="mb-3">
            <label for="usuario" class="form-label">Usuario</label>
            <?php
                $usuario = '';
                if (!empty($datos['values']['usuario'])) {
                    $usuario = htmlspecialchars($datos['values']['usuario']);
                }
            ?>
            <input type="text" class="form-control" id="usuario" name="usuario" value="<?= $usuario ?>" required>
        </div>

        <div class="mb-3">
            <label for="nombre" class="form-label">Nombre</label>
            <?php 
                $nombre = '';
                if (!empty($datos['values']['nombre'])) {
                    $nombre = htmlspecialchars($datos['values']['nombre']);
                }
            ?>
            <input type="text" class="form-control" id="nombre" name="nombre" value="<?= $nombre ?>" required>
        </div>

        <div class="mb-3">
            <label for="apellido" class="form-label">Apellido</label>
            <?php 
                $apellido = '';
                if (!empty($datos['values']['apellido'])) {
                    $apellido = htmlspecialchars($datos['values']['apellido']);
                } 
            ?>
            <input type="text" class="form-control" id="apellido" name="apellido" value="<?= $apellido ?>" required>
        </div>
        
        <div class="mb-3">
            <label for="cargo" class="form-label">Cargo</label>
            <?php 
                $cargo = '';
                if (!empty($datos['values']['cargo'])) {
                    $cargo = htmlspecialchars($datos['values']['cargo']);
                } 
            ?>
            <input type="text" class="form-control" id="cargo" name="cargo" value="<?= $cargo ?>">
        </div>

        <div class="mb-3">
            <label for="sector" class="form-label">Sector</label>
            <?php 
                $sector = '';
                if (!empty($datos['values']['sector'])) {
                    $sector = htmlspecialchars($datos['values']['sector']);
                } 
            ?>
            <input type="text" class="form-control" id="sector" name="sector" value="<?= $sector ?>">
        </div>

        <div class="mb-3">
            <label for="tipo_usuario" class="form-label">Tipo de usuario</label>
            <div class="input-group">
                <select class="form-select" id="tipo_usuario" name="tipo_usuario" required>
                    <option value="">Seleccione...</option>
                    <?php foreach ($datos['tipos'] as $n): ?>
                        <?php
                            // Bloqueamos Admin (1) y Director (2) si el que edita es Director
                            if ($n['id_tipo_usuario'] == 1 || ($_SESSION['usuario_tipo'] == 2 && $n['id_tipo_usuario'] == 2)) {
                                continue;
                            }
                            
                            $selected = '';
                            if (isset($datos['values']['id_tipo_usuario']) && $datos['values']['id_tipo_usuario'] == $n['id_tipo_usuario']) {
                                $selected = 'selected';
                            }
                        ?>
                        <option value="<?= $n['id_tipo_usuario'] ?>" <?= $selected ?>>
                            <?= htmlspecialchars($n['tipo_usuario']) ?>
                        </option>
                    <?php endforeach ?>
                </select>
            </div>
        </div>
        
        <?php if (!$datos['update']): ?>
            <div class="mb-3">
                <label for="password" class="form-label">Contraseña</label>
                <input type="password" class="form-control" id="password" name="password" required>
            </div>
        <?php endif; ?>

        <button type="submit" class="btn btn-success">Guardar</button>
        <a href="<?= URL ?>/usuarios" class="btn btn-secondary">Cancelar</a>
    </form>
</div>
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
            <label for="nombre_objeto" class="form-label">Denominación del Objeto</label>
            <input type="text" class="form-control" id="nombre_objeto" name="nombre_objeto" 
                   value="<?php if(!empty($datos['values']['nombre_objeto'])){echo htmlspecialchars($datos['values']['nombre_objeto']);}?>" required>
        </div>
        <div class="mb-3">
            <label for="descripcion_tecnica" class="form-label">Descripción Técnica</label>
            <textarea class="form-control" id="descripcion_tecnica" name="descripcion_tecnica" rows="3"><?php if(!empty($datos['values']['descripcion_tecnica'])){echo htmlspecialchars($datos['values']['descripcion_tecnica']);}?></textarea>
        </div>
        <div class="mb-3">
            <label for="materiales" class="form-label">Materiales</label>
            <input type="text" class="form-control" id="materiales" name="materiales" 
                   value="<?php if(!empty($datos['values']['materiales'])){echo htmlspecialchars($datos['values']['materiales']);}?>">
        </div>
        <div class="mb-3">
            <label for="dimensiones" class="form-label">Dimensiones</label>
            <input type="text" class="form-control" id="dimensiones" name="dimensiones" 
                   value="<?php if(!empty($datos['values']['dimensiones'])){echo htmlspecialchars($datos['values']['dimensiones']);}?>">
        </div>
        <div class="mb-3">
            <label for="estado_conservacion" class="form-label">Estado de Conservación</label>
            <select class="form-select" id="estado_conservacion" name="estado_conservacion">
                <option value="Excelente" <?php if(!empty($datos['values']['estado_conservacion']) && $datos['values']['estado_conservacion'] == 'Excelente') echo 'selected'; ?>>Excelente</option>
                <option value="Bueno" <?php if(!empty($datos['values']['estado_conservacion']) && $datos['values']['estado_conservacion'] == 'Bueno') echo 'selected'; ?>>Bueno</option>
                <option value="Regular" <?php if(!empty($datos['values']['estado_conservacion']) && $datos['values']['estado_conservacion'] == 'Regular') echo 'selected'; ?>>Regular</option>
                <option value="Malo" <?php if(!empty($datos['values']['estado_conservacion']) && $datos['values']['estado_conservacion'] == 'Malo') echo 'selected'; ?>>Malo</option>
                <option value="En Restauración" <?php if(!empty($datos['values']['estado_conservacion']) && $datos['values']['estado_conservacion'] == 'En Restauración') echo 'selected'; ?>>En Restauración</option>
            </select>
        </div>
        <div class="d-grid gap-2 d-md-flex justify-content-md-end mt-4">
            <button type="submit" class="btn btn-success">
                <i class="bi bi-save"></i> Guardar</button>
            <a href="<?= URL ?>/objeto" class="btn btn-secondary">
                <i class="bi bi-x-circle"></i> Cancelar</a>
        </div>
    </form>
</div>

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
        <div class="row">
            <div class="col-md-6 mb-3">
                <label for="nombre_ubicacion" class="form-label">Nombre Ubicación</label>
                <input type="text" class="form-control" id="nombre_ubicacion" name="nombre_ubicacion" 
                       value="<?php if(!empty($datos['values']['nombre_ubicacion'])){echo htmlspecialchars($datos['values']['nombre_ubicacion']);}?>" required>
            </div>
            <div class="col-md-6 mb-3">
                <label for="seccion" class="form-label">Sección</label>
                <input type="text" class="form-control" id="seccion" name="seccion" 
                       value="<?php if(!empty($datos['values']['seccion'])){echo htmlspecialchars($datos['values']['seccion']);}?>">
            </div>
        </div>
        <div class="mb-3">
            <label for="descripcion" class="form-label">Descripción</label>
            <textarea class="form-control" id="descripcion" name="descripcion" rows="3"><?php if(!empty($datos['values']['descripcion'])){echo htmlspecialchars($datos['values']['descripcion']);}?></textarea>
        </div>

        <div class="d-grid gap-2 d-md-flex justify-content-md-end mt-4">
            <button type="submit" class="btn btn-success">
                <i class="bi bi-save"></i> Guardar</button>
            <a href="<?= URL ?>/ubicaciones" class="btn btn-secondary">
                <i class="bi bi-x-circle"></i> Cancelar</a>
        </div>
    </form>
</div>

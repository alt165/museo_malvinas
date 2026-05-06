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
                <label for="nombre_depositante" class="form-label">Nombre</label>
                <input type="text" class="form-control" id="nombre_depositante" name="nombre_depositante" 
                       value="<?php if(!empty($datos['values']['nombre_depositante'])){echo htmlspecialchars($datos['values']['nombre_depositante']);}?>" required>
            </div>
            <div class="col-md-6 mb-3">
                <label for="apellido_depositante" class="form-label">Apellido</label>
                <input type="text" class="form-control" id="apellido_depositante" name="apellido_depositante" 
                       value="<?php if(!empty($datos['values']['apellido_depositante'])){echo htmlspecialchars($datos['values']['apellido_depositante']);}?>" required>
            </div>
        </div>
        <div class="row">
            <div class="col-md-6 mb-3">
                <label for="dni_depositante" class="form-label">DNI</label>
                <input type="text" class="form-control" id="dni_depositante" name="dni_depositante" 
                       value="<?php if(!empty($datos['values']['dni_depositante'])){echo htmlspecialchars($datos['values']['dni_depositante']);}?>" required>
            </div>
            <div class="col-md-6 mb-3">
                <label for="cuil_cuit" class="form-label">CUIL/CUIT</label>
                <input type="text" class="form-control" id="cuil_cuit" name="cuil_cuit" 
                       value="<?php if(!empty($datos['values']['cuil_cuit'])){echo htmlspecialchars($datos['values']['cuil_cuit']);}?>">
            </div>
        </div>
        <div class="mb-3">
            <label for="domicilio" class="form-label">Domicilio</label>
            <input type="text" class="form-control" id="domicilio" name="domicilio" 
                   value="<?php if(!empty($datos['values']['domicilio'])){echo htmlspecialchars($datos['values']['domicilio']);}?>">
        </div>
        <div class="row">
            <div class="col-md-6 mb-3">
                <label for="correo_electronico" class="form-label">Correo Electrónico</label>
                <input type="email" class="form-control" id="correo_electronico" name="correo_electronico" 
                       value="<?php if(!empty($datos['values']['correo_electronico'])){echo htmlspecialchars($datos['values']['correo_electronico']);}?>">
            </div>
            <div class="col-md-6 mb-3">
                <label for="nro_telefono" class="form-label">Teléfono</label>
                <input type="text" class="form-control" id="nro_telefono" name="nro_telefono" 
                       value="<?php if(!empty($datos['values']['nro_telefono'])){echo htmlspecialchars($datos['values']['nro_telefono']);}?>">
            </div>
        </div>
        <div class="row">
            <div class="col-md-6 mb-3">
                <label for="localidad" class="form-label">Localidad</label>
                <input type="text" class="form-control" id="localidad" name="localidad" 
                       value="<?php if(!empty($datos['values']['localidad'])){echo htmlspecialchars($datos['values']['localidad']);}?>">
            </div>
            <div class="col-md-6 mb-3">
                <label for="codigo_postal" class="form-label">Código Postal</label>
                <input type="text" class="form-control" id="codigo_postal" name="codigo_postal" 
                       value="<?php if(!empty($datos['values']['codigo_postal'])){echo htmlspecialchars($datos['values']['codigo_postal']);}?>">
            </div>
        </div>

        <div class="d-grid gap-2 d-md-flex justify-content-md-end mt-4">
            <button type="submit" class="btn btn-success">
                <i class="bi bi-save"></i> Guardar</button>
            <a href="<?= URL ?>/depositante" class="btn btn-secondary">
                <i class="bi bi-x-circle"></i> Cancelar</a>
        </div>
    </form>
</div>

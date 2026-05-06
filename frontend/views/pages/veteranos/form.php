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
                <label for="nombre_veterano" class="form-label">Nombre</label>
                <input type="text" class="form-control" id="nombre_veterano" name="nombre_veterano" 
                       value="<?php if(!empty($datos['values']['nombre_veterano'])){echo htmlspecialchars($datos['values']['nombre_veterano']);}?>" required>
            </div>
            <div class="col-md-6 mb-3">
                <label for="apellido_veterano" class="form-label">Apellido</label>
                <input type="text" class="form-control" id="apellido_veterano" name="apellido_veterano" 
                       value="<?php if(!empty($datos['values']['apellido_veterano'])){echo htmlspecialchars($datos['values']['apellido_veterano']);}?>" required>
            </div>
        </div>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label for="dni_veterano" class="form-label">DNI</label>
                <input type="text" class="form-control" id="dni_veterano" name="dni_veterano" 
                       value="<?php if(!empty($datos['values']['dni_veterano'])){echo htmlspecialchars($datos['values']['dni_veterano']);}?>">
            </div>
            <div class="col-md-6 mb-3">
                <label for="fecha_nacimiento" class="form-label">Fecha de Nacimiento</label>
                <input type="date" class="form-control" id="fecha_nacimiento" name="fecha_nacimiento" 
                       value="<?php if(!empty($datos['values']['fecha_nacimiento'])){echo htmlspecialchars($datos['values']['fecha_nacimiento']);}?>">
            </div>
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
            <div class="col-md-4 mb-3">
                <label for="localidad" class="form-label">Localidad</label>
                <input type="text" class="form-control" id="localidad" name="localidad" 
                       value="<?php if(!empty($datos['values']['localidad'])){echo htmlspecialchars($datos['values']['localidad']);}?>">
            </div>
            <div class="col-md-4 mb-3">
                <label for="legajo_militar" class="form-label">Legajo Militar</label>
                <input type="text" class="form-control" id="legajo_militar" name="legajo_militar" 
                       value="<?php if(!empty($datos['values']['legajo_militar'])){echo htmlspecialchars($datos['values']['legajo_militar']);}?>">
            </div>
            <div class="col-md-4 mb-3">
                <label for="rango" class="form-label">Rango</label>
                <input type="text" class="form-control" id="rango" name="rango" 
                       value="<?php if(!empty($datos['values']['rango'])){echo htmlspecialchars($datos['values']['rango']);}?>">
            </div>
        </div>
        
        <div class="mb-3">
            <label for="fecha_fallecimiento" class="form-label">Fecha de Fallecimiento (Opcional)</label>
            <input type="date" class="form-control" id="fecha_fallecimiento" name="fecha_fallecimiento" 
                   value="<?php if(!empty($datos['values']['fecha_fallecimiento'])){echo htmlspecialchars($datos['values']['fecha_fallecimiento']);}?>">
        </div>

        <div class="d-grid gap-2 d-md-flex justify-content-md-end mt-4">
            <button type="submit" class="btn btn-success">
                <i class="bi bi-save"></i> Guardar</button>
            <a href="<?= URL ?>/veteranos" class="btn btn-secondary">
                <i class="bi bi-x-circle"></i> Cancelar</a>
        </div>
    </form>
</div>
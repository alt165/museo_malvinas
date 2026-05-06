
<?php if (!empty($datos['errores'])): ?>
    <div class="alert alert-danger">
        <ul>
            <?php foreach ($datos['errores'] as $e): ?>
                <li><?= htmlspecialchars($e) ?></li>
            <?php endforeach ?>
        </ul>
    </div>
<?php endif; ?>
<div class="container-fluid mt-1 px-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="mb-0"><?= $datos['title'] ?></h2>
        <?php if (!empty($datos['urlCrear']) && in_array('cargar abm',$_SESSION['usuario_derechos'])): ?>
            <a href="<?= $datos['urlCrear'] ?>" class="btn btn-success"><?= !empty($datos['textoCrear']) ? htmlspecialchars($datos['textoCrear']) : 'Nuevo Usuario' ?></a>
        <?php endif; ?>
    </div>
    <div class="table-responsive-lg shadow rounded">
        <table class="table table-hover align-middle mb-0" id="tablaABM" style="min-width: 800px;">
            <thead class="table-light">
                <tr>
                    <?php foreach ($datos['columnas'] as $col): ?>
                        <th><?= $col ?></th>
                    <?php endforeach ?>
                    <?php if (!empty($datos['acciones'])): ?>
                        <th>Acciones</th>
                    <?php endif; ?>
                </tr>
            </thead>
        </table>
    </div>
</div>

<!-- Modal para visualizar imagen -->
<div class="modal fade" id="modalVerImagen" tabindex="-1" aria-labelledby="modalVerImagenLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="modalVerImagenLabel">Vista de imagen</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <div class="modal-body text-center">
                <img id="imagenModal" src="" alt="Imagen del objeto"
                     class="img-fluid rounded shadow"
                     style="max-height: 60vh; object-fit: contain;"
                     onerror="this.style.display='none'; document.getElementById('imagenModalError').style.display='block';">
                <p id="imagenModalError" class="text-muted mt-3" style="display:none;">
                    La imagen no está disponible o la ruta es incorrecta.
                </p>
                <div id="imagenModalDescripcion" class="mt-3 text-start" style="display:none;">
                    <hr>
                    <p class="text-secondary" style="text-align: justify; line-height: 1.6;"></p>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
            </div>
        </div>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function () {
    $('#tablaABM').DataTable({
        processing: true,
        serverSide: true,
        ajax: '<?= $datos['urlAjax'] ?>',
        columns: [
            <?php foreach ($datos['columnas_claves'] as $col): ?>
                { data: '<?= $col ?>', render: function(data) { return data; } },
            <?php endforeach; ?>
            <?php if (!empty($datos['acciones'])): ?>
                { data: 'acciones', orderable: false, searchable: false }
            <?php endif; ?>
        ],
        dom: 'Bfrtip',
        buttons: [
            { extend: 'copy', text: 'Copiar', className: 'btn btn-secondary btn-sm' },
            { extend: 'csv', text: 'CSV', className: 'btn btn-primary btn-sm', bom: true, charset: 'UTF-8' },
            { extend: 'excel', text: 'Excel', className: 'btn btn-success btn-sm' },
            { extend: 'pdf', text: 'PDF', className: 'btn btn-danger btn-sm' },
            { extend: 'print', text: 'Imprimir', className: 'btn btn-info btn-sm' }
        ],
        language: {
            url: 'https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json'
        }
    });

    // Abrir modal al hacer click en "Ver imagen"
    document.addEventListener('click', function(e) {
        if (e.target && e.target.classList.contains('btn-ver-imagen')) {
            var ruta = e.target.getAttribute('data-imagen');
            var titulo = e.target.getAttribute('data-titulo') || 'Vista de imagen';
            var descripcion = e.target.getAttribute('data-descripcion') || '';

            var img = document.getElementById('imagenModal');
            var error = document.getElementById('imagenModalError');
            var tituloEl = document.getElementById('modalVerImagenLabel');
            var descContainer = document.getElementById('imagenModalDescripcion');
            var descText = descContainer.querySelector('p');

            // Resetear estado
            img.style.display = 'block';
            error.style.display = 'none';
            img.src = ruta || '';

            // Título dinámico
            tituloEl.textContent = titulo;

            // Descripción
            if (descripcion) {
                descText.textContent = descripcion;
                descContainer.style.display = 'block';
            } else {
                descContainer.style.display = 'none';
            }

            var modal = new bootstrap.Modal(document.getElementById('modalVerImagen'));
            modal.show();
        }
    });
});
</script>
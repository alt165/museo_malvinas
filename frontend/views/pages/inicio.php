
<div class="container mt-4">
    <?php if (!empty($datos['errores'])): ?>
    <div class="alert alert-danger">
        <ul>
            <?php foreach ($datos['errores'] as $e): ?>
                <li><?= htmlspecialchars($e) ?></li>
            <?php endforeach ?>
        </ul>
    </div>
<?php endif; ?>
    <h1 class="mb-4 text-center"style="font-family: 'Lexend';">Archivo Histórico del Museo Malvinas, Antártida y Atlántico Sur </h1>
    
    <input type="text" id="buscarObjetos" class="form-control mb-3" placeholder="🔍 Buscar objeto...">
                <?php if (isset($_SESSION['mensaje_exito'])): //Codigo para lanzar mensaje de contraseña cambiada?>
                <div class="alert alert-success alert-dismissible fade show mt-3" role="alert">
                    <?= $_SESSION['mensaje_exito']; ?>
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
                <?php unset($_SESSION['mensaje_exito']); // Lo borramos para que no aparezca si recarga la página ?>
            <?php endif; ?>
    <div class="row mb-4">
        <div class="col-md-12 mb-3">
            <div class="card shadow-sm h-100">
                <div class="card-header bg-info text-white">📅  Exhibiciones Programadas</div>
                <div class="card-body" style="max-height: 450px; overflow-y: auto;">
                    <?php if (!empty($datos['exhibiciones_desde_hoy'])): ?>
                        <ul id="listaObjetos" class="list-group list-group-flush">
                            <?php if (!empty($datos['exhibiciones_desde_hoy'])): ?>
                                <?php foreach ($datos['exhibiciones_desde_hoy'] as $r): ?>
                                    <!-- Personalizar cómo se muestra cada exhibición activa -->
                                <?php endforeach; ?>
                            <?php else: ?>
                                <li class="list-group-item text-muted">No hay exhibiciones futuras.</li>
                            <?php endif; ?>
                        </ul>
                    <?php else: ?>
                        <p class="text-muted">No hay exhibiciones futuras.</p>
                    <?php endif; ?>
                </div>
            </div>
        </div>
    </div>
    <body> 
        <img src="<?= URL ?>/public/img/logo_museo_blanco.png" alt="Logo del museo para la página de inicio"  
                    align="center" style="width: 100%; max-width: 550px; object-fit: contain; display: block; margin-left: auto; margin-right: auto;"> 
        <script>
            document.addEventListener('DOMContentLoaded', function () {
                const buscador = document.getElementById('buscarObjetos');
                const items = document.querySelectorAll('#listaObjetos li');

                buscador.addEventListener('keyup', function () {
                    const filtro = this.value.toLowerCase();
                    let visibles = 0;

                    items.forEach(function (item) {
                        const texto = item.innerText.toLowerCase();
                        const coincide = texto.includes(filtro);
                        item.style.display = coincide ? '' : 'none';
                        if (coincide) visibles++;
                    });

                    // Si no hay coincidencias, mostramos un mensaje
                    if (visibles === 0) {
                        if (!document.getElementById('sinResultados')) {
                            const li = document.createElement('li');
                            li.id = 'sinResultados';
                            li.className = 'list-group-item text-muted';
                            li.textContent = 'No se encontraron resultados.';
                            document.getElementById('listaObjetos').appendChild(li);
                        }
                    } else {
                        const sinResultados = document.getElementById('sinResultados');
                        if (sinResultados) sinResultados.remove();
                    }
                });
            });
        </script>
    </body>
</div>




    
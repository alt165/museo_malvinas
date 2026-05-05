# Domain

## Conceptos principales

El dominio representa la gestion patrimonial del museo:

- Objetos del museo y objetos digitales.
- Inventario, ubicaciones y movimientos.
- Exhibiciones y devolucion de objetos.
- Veteranos y actuaciones.
- Depositantes.
- Categorias.
- Relaciones entre objetos.
- Auditoria y usuarios referenciados desde Keycloak.

## Entidad base y borrado logico

Las entidades persistentes extienden `EntidadBase`, que contiene:

- `activo`
- `eliminado`
- `fechaEliminacion`

La baja de recursos se implementa como borrado logico. Los services validan que una entidad no este eliminada al recuperarla.

## Objetos del museo

`ObjetoMuseo` es la entidad central del inventario patrimonial.

Campos principales:

- `numeroInventario`: unico y obligatorio.
- `nombre`: obligatorio.
- `tipoObjeto`.
- `descripcion`.

Regla importante:

- No puede existir otro objeto activo con el mismo `numeroInventario`.

`ObjetoDigital` hereda de `ObjetoMuseo` usando estrategia JPA `JOINED`. En este modelo no representa un archivo asociado; guarda metadatos digitales como formato, resolucion y derechos.

## Inventario y movimientos

`Inventario` vincula un objeto con su ubicacion, estado y conservacion.

Relaciones:

- `Inventario` tiene una relacion uno a uno con `ObjetoMuseo`.
- `Inventario` referencia una `Ubicacion`.
- `MovimientoInventario` registra cambios sobre un objeto y puede referenciar ubicacion origen, ubicacion destino y usuario.

Estados de inventario:

- `DISPONIBLE`
- `EN_EXHIBICION`
- `EN_RESTAURACION`
- `PRESTADO`
- `BAJA`

Estados de conservacion:

- `EXCELENTE`
- `BUENO`
- `REGULAR`
- `MALO`
- `CRITICO`

Tipos de movimiento:

- `INGRESO`
- `CAMBIO_UBICACION`
- `SALIDA_EXHIBICION`
- `DEVOLUCION_EXHIBICION`
- `RESTAURACION`
- `PRESTAMO`
- `BAJA`

Regla implementada:

- Al crear o actualizar inventario se registra un `MovimientoInventario` asociado.

## Exhibiciones

`Exhibicion` modela muestras temporales o permanentes del museo.

Tipos:

- `TEMPORAL`
- `PERMANENTE`

Estados:

- `PLANIFICADA`
- `ACTIVA`
- `FINALIZADA`

`ExhibicionObjeto` vincula objetos con exhibiciones y registra inclusion, retiro y verificacion de devolucion.

Estados de objeto en exhibicion:

- `EN_EXHIBICION`
- `DEVUELTO`
- `PENDIENTE_REVISION`

Reglas implementadas:

- Un objeto no puede estar asociado a mas de una exhibicion activa.
- Para finalizar una exhibicion, todos sus objetos asociados deben tener devolucion verificada.
- No se puede dar de baja una exhibicion activa.
- El endpoint de verificacion de devolucion marca el objeto como devuelto y registra datos de verificacion.

## Veteranos y actuaciones

`Veterano` registra informacion de personas vinculadas a la guerra.

Campos principales:

- `nombre`
- `apellido`
- `fuerza`
- `fechaNacimiento`
- `fechaFallecimiento`
- `historia`

Fuerzas validas:

- `EJERCITO`
- `ARMADA`
- `FUERZA_AEREA`
- `PREFECTURA`
- `GENDARMERIA`
- `CIVIL`

`ActuacionVeterano` registra informacion de actuacion como rol, unidad, lugar, fechas y descripcion.

## Depositantes

`Depositante` representa personas o instituciones que entregaron o vincularon objetos al museo.

Tipos:

- `PERSONA`
- `INSTITUCION`

`ObjetoDepositante` es la entidad intermedia entre objetos y depositantes. Permite registrar fecha de deposito, tipo de deposito y observaciones.

## Categorias y relaciones

`CategoriaObjeto` clasifica objetos.

`ObjetoCategoria` es la entidad intermedia entre objetos y categorias. Se evita `@ManyToMany` directo para mantener control del dominio y permitir atributos adicionales.

`RelacionObjeto` vincula dos objetos entre si con:

- objeto origen
- objeto relacionado
- tipo de relacion
- descripcion

## Usuarios y auditoria

`Usuario` guarda una referencia local a Keycloak:

- `keycloakId`
- `nombre`
- `email`
- `fechaCreacion`

No se persisten roles locales; Keycloak es la fuente de verdad.

`Auditoria` existe en el modelo para registrar operaciones sobre entidades, con tipo de operacion, entidad, id y datos previos/nuevos.


# Ficha completa del objeto en PDF

## Alcance

Se incorporó un reporte A4 vertical denominado **Ficha completa del objeto**, accesible desde la consulta individual mediante el botón **Descargar ficha PDF**. La operación es de solo lectura y no modifica el objeto, el inventario ni sus estados.

## Endpoint

`GET /api/objetos/{id}/ficha-pdf`

- Requiere autenticación y uno de los roles de consulta `ADMIN`, `MUSEOLOGO` o `VIEWER`.
- Responde `application/pdf` y `Content-Disposition: attachment`.
- El nombre es `ficha-objeto-{numeroInventario}.pdf`; si no existe número de inventario utiliza el ID. El identificador se normaliza para evitar caracteres inseguros.
- Conserva el manejo vigente de `401`, `403`, `404` y errores internos sin exponer rutas del servidor.

## Diseño y secciones

Se reutilizó iText y se amplió `PdfReportService` con una variante para reportes detallados A4 verticales. Conserva colores, tipografías, encabezado institucional, fecha de generación y pie con número de página de los reportes existentes.

El contenido sigue los campos y el orden funcional de la consulta del objeto:

1. Datos generales.
2. Recepción y procedencia.
3. Información técnica.
4. Situación legal.
5. Estado de conservación.
6. Conservación preventiva.
7. Inventario.
8. Relaciones con personas y otros objetos en tabla textual.
9. Exhibiciones.
10. Recibos, únicamente para roles autorizados.
11. Registro fotográfico.

Las secciones sin datos se omiten, salvo el registro fotográfico, que informa discretamente cuando no hay fotografías visibles. Los textos conservan saltos de línea, no se truncan y pueden continuar en varias páginas.

## Visibilidad y autorización

- El reporte parte de `ObjetoMuseoService.obtenerPorId`, el mismo DTO filtrado que consume la pantalla de detalle. Por ello reutiliza la política de embargo y la visibilidad campo por campo.
- `RelacionObjetoService.listarPorObjeto` conserva la visibilidad de objetos relacionados y sus campos.
- Los recibos se consultan e incluyen sólo cuando `ObjetoVisibilityPolicy` autoriza campos privados (`ADMIN` o `MUSEOLOGO`).
- Un `VIEWER` no puede obtener campos privados, recibos privados, fotografías privadas ni un objeto embargado modificando la URL.
- La seguridad no depende del frontend; el botón sólo inicia una operación que el backend vuelve a autorizar.

## Fotografías

- Se utiliza exclusivamente `FotoObjetoMuseoService.descargar`, que entrega la variante pública/autorizada y aplica la política de visibilidad. El reporte nunca invoca la descarga de originales.
- Se mantiene el mismo orden de la galería: fecha de carga e ID. La primera fotografía se presenta como principal y no se repite.
- La principal ocupa un bloque destacado y las restantes se disponen en una cuadrícula de dos columnas.
- Cada imagen se decodifica de manera independiente, conserva su relación de aspecto, se limita a 1200 píxeles en el lado mayor y se incorpora como JPEG reducido.
- El límite de entrada por imagen es 15 MiB. Si una imagen no puede procesarse, se registra sólo el ID y el tipo de error, se muestra `Imagen no disponible` y el documento continúa.
- Se incluyen las descripciones autorizadas. No se consulta ni incorpora ningún servicio, archivo, miniatura, reproductor o enlace de video.

## Frontend

La consulta individual incorpora el botón junto a las acciones existentes. Durante la generación:

- muestra `Generando PDF...`;
- deshabilita el botón para evitar duplicados;
- conserva al usuario en la pantalla;
- descarga el blob con un nombre seguro;
- muestra el error mediante el componente común de errores.

## Archivos modificados

- `backend/src/main/java/com/proveedores/controller/ObjetoMuseoController.java`
- `backend/src/main/java/com/proveedores/report/PdfReportService.java`
- `backend/src/main/java/com/proveedores/service/ExhibicionObjetoService.java`
- `backend/src/main/java/com/proveedores/service/ObjetoMuseoFichaPdfService.java`
- `backend/src/test/java/com/proveedores/controller/ObjetoMuseoControllerTest.java`
- `backend/src/test/java/com/proveedores/controller/ObjetoPendienteCompletarSecurityTest.java`
- `backend/src/test/java/com/proveedores/service/ObjetoMuseoFichaPdfServiceTest.java`
- `frontend/src/app/objetos/[id]/page.tsx`
- `frontend/src/features/objetos/api/index.ts`
- `frontend/src/features/objetos/api/objetos-api.ts`
- `OBJECT_FULL_PDF_REPORT_IMPLEMENTATION.md`

## Pruebas ejecutadas

### Backend

- Pruebas focalizadas de servicio, controlador y seguridad: **36 ejecutadas, 0 fallos, 0 errores**.
- `mvn clean test`: **333 ejecutadas, 0 fallos, 0 errores, 0 omitidas**.
- La línea base era 325; se agregaron 8 pruebas.

Los casos nuevos cubren PDF válido, cabeceras de descarga, autenticación y autorización, `ADMIN`, `MUSEOLOGO`, `VIEWER`, objeto inexistente/no visible por embargo, filtrado privado, recibos, fotos autorizadas, no uso de originales, cero/múltiples/fotos dañadas, relaciones, exhibiciones, caracteres acentuados, contenido extenso multipágina y ausencia de páginas vacías.

### Frontend

- `npm run lint`: exitoso.
- `npx tsc --noEmit`: exitoso.
- `npm run build`: exitoso; 39 páginas estáticas generadas y la ruta dinámica `/objetos/[id]` compilada.

## Limitaciones

- La ficha toma la información del DTO vigente de consulta, no de una consulta paralela ni de una lista de campos suministrada externamente.
- Una fotografía corrupta, no soportada o superior al límite no bloquea la ficha y se representa mediante el marcador de indisponibilidad.
- No se agregaron bibliotecas ni migraciones y se mantiene Java 17.

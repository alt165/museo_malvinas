"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { hasRole, useAuth } from "@/lib/auth";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import {
  descargarCopiaFirmadaRecibo,
  descargarReciboPdf
} from "@/features/objetos/api";
import {
  useObjetoQuery,
  useRecibosObjetoQuery
} from "@/features/objetos/queries";
import { ObjetoArchivosPanel } from "@/features/objetos/components/objeto-archivos-panel";
import { getApiErrorMessage } from "@/features/objetos/utils";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

function descargarBlob(blob: Blob, nombre: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = nombre;
  link.click();
  URL.revokeObjectURL(url);
}

export default function DetalleObjetoPage() {
  const params = useParams<{ id: string }>();
  const id = getParamId(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const { roles } = useAuth();
  const esAdmin = hasRole(roles, "ADMIN");
  const { data, error, isError, isLoading } = useObjetoQuery(id);
  const { data: recibos = [] } = useRecibosObjetoQuery(id);

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            <div className="flex gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos">
                Volver
              </Link>
              {puedeEscribir && data ? (
                <Link
                  className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                  href={`/objetos/${data.id}/editar`}
                >
                  Editar
                </Link>
              ) : null}
              {puedeEscribir && data ? (
                <Link
                  className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                  href={`/objetos/${data.id}/movimientos`}
                >
                  Ver movimientos
                </Link>
              ) : null}
              {esAdmin && data ? (
                <Link
                  className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                  href={`/objetos/${data.id}/historial`}
                >
                  Historial
                </Link>
              ) : null}
              {data ? (
                <Link
                  className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                  href={`/objetos/${data.id}/relaciones`}
                >
                  Ver relaciones
                </Link>
              ) : null}
            </div>
          }
          description="Detalle completo del objeto patrimonial."
          title="Detalle de objeto"
        />
        {isLoading ? <LoadingState label="Cargando objeto..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {data ? (
          <div className="rounded-lg border p-5">
            <dl className="grid gap-5 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-muted-foreground">Numero de inventario</dt>
                <dd className="mt-1 font-medium">{data.numeroInventario}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Denominacion</dt>
                <dd className="mt-1 font-medium">{data.denominacionObjeto}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-muted-foreground">Descripcion</dt>
                <dd className="mt-1 whitespace-pre-wrap font-medium">{data.descripcion || "Sin descripcion"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Estado de conservacion</dt>
                <dd className="mt-1 font-medium">{data.estadoConservacion || "No especificado"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Ubicacion actual</dt>
                <dd className="mt-1 font-medium">{data.ubicacionNombre || "Sin ubicacion registrada"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Coleccion</dt>
                <dd className="mt-1 font-medium">
                  {data.coleccionId ? (
                    <Link className="text-primary underline-offset-4 hover:underline" href={`/objetos/colecciones/${data.coleccionId}`}>
                      {data.coleccionNombre}
                    </Link>
                  ) : "Sin coleccion"}
                </dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Categorias</dt>
                <dd className="mt-1 font-medium">{data.categorias?.map((categoria) => categoria.nombre).join(", ") || "Sin categorias"}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-muted-foreground">Descripcion tecnica</dt>
                <dd className="mt-1 whitespace-pre-wrap font-medium">{data.descripcionTecnica || "Sin descripcion tecnica"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Materiales</dt>
                <dd className="mt-1 whitespace-pre-wrap font-medium">{data.materiales || "No especificado"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Dimensiones</dt>
                <dd className="mt-1 whitespace-pre-wrap font-medium">{data.dimensiones || "No especificado"}</dd>
              </div>
            </dl>
          </div>
        ) : null}
        {data ? <ObjetoArchivosPanel mode="view" objeto={data} /> : null}
        {data ? (
          <div className="rounded-lg border p-5">
            <h2 className="text-base font-semibold">Recibos</h2>
            <div className="mt-4 grid gap-3">
              {recibos.length === 0 ? <p className="text-sm text-muted-foreground">Sin recibos emitidos.</p> : null}
              {recibos.map((recibo) => (
                <div className="rounded-md border p-3 text-sm" key={recibo.id}>
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p className="font-medium">{recibo.numeroRecibo}</p>
                      <p className="text-muted-foreground">{recibo.tieneCopiaFirmada ? `Copia firmada: ${recibo.copiaFirmadaNombreArchivo}` : "Sin copia firmada adjunta"}</p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <button className="rounded-md border px-3 py-1.5 hover:bg-muted" onClick={async () => descargarBlob(await descargarReciboPdf(recibo.id), `recibo-${recibo.id}.pdf`)} type="button">PDF</button>
                      {recibo.tieneCopiaFirmada ? (
                        <button className="rounded-md border px-3 py-1.5 hover:bg-muted" onClick={async () => descargarBlob(await descargarCopiaFirmadaRecibo(recibo.id), recibo.copiaFirmadaNombreArchivo || `recibo-firmado-${recibo.id}`)} type="button">Copia firmada</button>
                      ) : null}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

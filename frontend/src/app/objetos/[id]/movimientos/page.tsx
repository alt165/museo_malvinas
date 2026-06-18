"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useMovimientosObjetoQuery, useObjetoQuery } from "@/features/objetos/queries";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "Sin dato";
  }
  return new Intl.DateTimeFormat("es-AR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

export default function MovimientosObjetoPage() {
  const params = useParams<{ id: string }>();
  const id = getParamId(params.id);
  const objetoQuery = useObjetoQuery(id);
  const movimientosQuery = useMovimientosObjetoQuery(id);
  const movimientos = movimientosQuery.data ?? [];

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          actions={
            <div className="flex gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/objetos/${id}`}>
                Volver
              </Link>
            </div>
          }
          description={objetoQuery.data ? `${objetoQuery.data.numeroInventario} - ${objetoQuery.data.denominacionObjeto}` : "Historial de ubicaciones del objeto."}
          title="Movimientos del objeto"
        />
        {objetoQuery.isLoading || movimientosQuery.isLoading ? <LoadingState label="Cargando movimientos..." /> : null}
        {objetoQuery.isError ? <ErrorState message={getApiErrorMessage(objetoQuery.error)} requestId={objetoQuery.error instanceof ApiClientError ? objetoQuery.error.requestId : undefined} /> : null}
        {movimientosQuery.isError ? <ErrorState message={getApiErrorMessage(movimientosQuery.error)} requestId={movimientosQuery.error instanceof ApiClientError ? movimientosQuery.error.requestId : undefined} /> : null}
        {!movimientosQuery.isLoading && !movimientosQuery.isError && movimientos.length === 0 ? (
          <EmptyState description="El objeto no tiene movimientos registrados." title="Sin movimientos" />
        ) : null}
        {!movimientosQuery.isLoading && !movimientosQuery.isError && movimientos.length > 0 ? (
          <div className="overflow-hidden rounded-lg border">
            <table className="w-full border-collapse text-sm">
              <thead className="bg-primary text-primary-foreground">
                <tr>
                  <th className="px-4 py-3 text-left font-semibold text-white">Fecha</th>
                  <th className="px-4 py-3 text-left font-semibold text-white">Origen</th>
                  <th className="px-4 py-3 text-left font-semibold text-white">Destino</th>
                  <th className="px-4 py-3 text-left font-semibold text-white">Descripcion</th>
                  <th className="px-4 py-3 text-left font-semibold text-white">Usuario</th>
                </tr>
              </thead>
              <tbody>
                {movimientos.map((movimiento) => (
                  <tr className="border-t" key={movimiento.id}>
                    <td className="px-4 py-3 align-top">{formatDateTime(movimiento.fechaMovimiento)}</td>
                    <td className="px-4 py-3 align-top">{movimiento.ubicacionOrigen || "No aplica"}</td>
                    <td className="px-4 py-3 align-top font-medium">{movimiento.ubicacionDestino || "Sin dato"}</td>
                    <td className="px-4 py-3 align-top text-muted-foreground">{movimiento.descripcion || "Sin descripcion"}</td>
                    <td className="px-4 py-3 align-top">{movimiento.usuarioMovimiento || "Sin dato"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

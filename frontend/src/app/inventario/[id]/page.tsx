"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useInventarioQuery } from "@/features/inventario/queries";
import { formatDate, formatDateTime, getApiErrorMessage } from "@/features/inventario/utils";
import { canWrite, useAuth } from "@/lib/auth";
import { ApiClientError } from "@/lib/errors/api-error";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function DetalleInventarioPage() {
  const params = useParams<{ id: string }>();
  const id = getParamId(params.id);
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const { data, error, isError, isLoading } = useInventarioQuery(id);

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            <div className="flex gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/inventario">
                Volver
              </Link>
              {puedeEscribir && data ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/inventario/${data.id}/editar`}>
                  Editar
                </Link>
              ) : null}
            </div>
          }
          description="Detalle del registro de inventario."
          title="Detalle de inventario"
        />
        {isLoading ? <LoadingState label="Cargando inventario..." /> : null}
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
                <dt className="text-muted-foreground">Objeto</dt>
                <dd className="mt-1 font-medium">{data.objetoNombre}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Ubicacion</dt>
                <dd className="mt-1 font-medium">{data.ubicacionNombre}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Estado</dt>
                <dd className="mt-1 font-medium">{data.estado}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Conservacion</dt>
                <dd className="mt-1 font-medium">{data.estadoConservacion}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Fecha de ingreso</dt>
                <dd className="mt-1 font-medium">{formatDate(data.fechaIngreso)}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Fecha de salida</dt>
                <dd className="mt-1 font-medium">{formatDate(data.fechaSalida)}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Ultimo movimiento</dt>
                <dd className="mt-1 font-medium">{formatDateTime(data.fechaUltimoMovimiento)}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-muted-foreground">Observaciones</dt>
                <dd className="mt-1 whitespace-pre-wrap font-medium">{data.observaciones || "Sin observaciones"}</dd>
              </div>
            </dl>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

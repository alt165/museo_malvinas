"use client";

import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { MovimientosInventarioTable } from "@/features/inventario/components/movimientos-inventario-table";
import { useMovimientosInventarioQuery } from "@/features/inventario/queries";
import { getApiErrorMessage } from "@/features/inventario/utils";
import { ApiClientError } from "@/lib/errors/api-error";

export default function MovimientosInventarioPage() {
  const { data = [], error, isError, isLoading } = useMovimientosInventarioQuery();

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          description="Historial de entradas, salidas y cambios de ubicacion de objetos."
          title="Movimientos de inventario"
        />
        {isLoading ? <LoadingState label="Cargando movimientos..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {!isLoading && !isError && data.length === 0 ? (
          <EmptyState
            description="Los movimientos se generan desde altas y actualizaciones de inventario."
            title="Sin movimientos"
          />
        ) : null}
        {!isLoading && !isError && data.length > 0 ? <MovimientosInventarioTable movimientos={data} /> : null}
      </div>
    </AppShell>
  );
}

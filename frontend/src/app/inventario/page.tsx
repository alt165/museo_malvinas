"use client";

import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { InventarioTable } from "@/features/inventario/components/inventario-table";
import { useInventariosQuery } from "@/features/inventario/queries";
import { getApiErrorMessage } from "@/features/inventario/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

export default function InventarioPage() {
  const { canEdit: puedeEscribir } = useEditingMode();
  const { data = [], error, isError, isLoading } = useInventariosQuery();

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            puedeEscribir ? (
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/inventario/nuevo">
                Nuevo inventario
              </Link>
            ) : null
          }
          description="Ubicacion, disponibilidad y estado de conservacion de los objetos."
          title="Inventario"
        />
        {isLoading ? <LoadingState label="Cargando inventario..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {!isLoading && !isError && data.length === 0 ? (
          <EmptyState
            action={
              puedeEscribir ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/inventario/nuevo">
                  Nuevo inventario
                </Link>
              ) : null
            }
            description="Todavia no hay registros activos de inventario."
            title="Sin inventario"
          />
        ) : null}
        {!isLoading && !isError && data.length > 0 ? <InventarioTable canEdit={puedeEscribir} inventarios={data} /> : null}
      </div>
    </AppShell>
  );
}

"use client";

import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ColeccionesTable } from "@/features/colecciones/components/colecciones-table";
import { useBajaLogicaColeccionMutation, useColeccionesQuery } from "@/features/colecciones/queries";
import { getApiErrorMessage } from "@/features/colecciones/utils";
import { canWrite, useAuth } from "@/lib/auth";
import { ApiClientError } from "@/lib/errors/api-error";
import { routes } from "@/lib/routes";

export default function ColeccionesPage() {
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const coleccionesQuery = useColeccionesQuery();
  const bajaMutation = useBajaLogicaColeccionMutation();

  function handleDelete(id: number) {
    if (window.confirm("Dar de baja esta coleccion? Los objetos asociados quedaran sin coleccion.")) {
      bajaMutation.mutate(id);
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            puedeEscribir ? (
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={routes.objetosColeccionNueva}>
                Nueva coleccion
              </Link>
            ) : null
          }
          description="Agrupaciones de objetos patrimoniales del museo."
          title="Colecciones de objetos"
        />
        {coleccionesQuery.isLoading ? <LoadingState label="Cargando colecciones..." /> : null}
        {coleccionesQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(coleccionesQuery.error)}
            requestId={coleccionesQuery.error instanceof ApiClientError ? coleccionesQuery.error.requestId : undefined}
          />
        ) : null}
        {bajaMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(bajaMutation.error)}
            requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined}
          />
        ) : null}
        {coleccionesQuery.data?.length === 0 ? (
          <EmptyState
            action={
              puedeEscribir ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={routes.objetosColeccionNueva}>
                  Nueva coleccion
                </Link>
              ) : null
            }
            description="Todavia no hay colecciones registradas."
            title="Sin colecciones"
          />
        ) : null}
        {coleccionesQuery.data && coleccionesQuery.data.length > 0 ? (
          <ColeccionesTable
            canEdit={puedeEscribir}
            colecciones={coleccionesQuery.data}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

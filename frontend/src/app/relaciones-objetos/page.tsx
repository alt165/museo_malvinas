"use client";

import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { RelacionesObjetosTable } from "@/features/relaciones-objetos/components/relaciones-objetos-table";
import { useBajaLogicaRelacionObjetoMutation, useRelacionesObjetoQuery } from "@/features/relaciones-objetos/queries";
import { getApiErrorMessage } from "@/features/relaciones-objetos/utils";
import { canWrite, useAuth } from "@/lib/auth";
import { ApiClientError } from "@/lib/errors/api-error";

export default function RelacionesObjetosPage() {
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const relacionesQuery = useRelacionesObjetoQuery();
  const bajaMutation = useBajaLogicaRelacionObjetoMutation();

  function handleDelete(id: number) {
    if (window.confirm("Dar de baja esta relacion entre objetos?")) {
      bajaMutation.mutate(id);
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={puedeEscribir ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/relaciones-objetos/nueva">Nueva relacion</Link> : null}
          description="Vinculos historicos o documentales entre objetos del museo."
          title="Relaciones entre objetos"
        />
        {relacionesQuery.isLoading ? <LoadingState label="Cargando relaciones..." /> : null}
        {relacionesQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(relacionesQuery.error)}
            requestId={relacionesQuery.error instanceof ApiClientError ? relacionesQuery.error.requestId : undefined}
          />
        ) : null}
        {bajaMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(bajaMutation.error)}
            requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined}
          />
        ) : null}
        {relacionesQuery.data?.length === 0 ? <EmptyState title="Sin relaciones" description="Todavia no hay relaciones entre objetos registradas." /> : null}
        {relacionesQuery.data && relacionesQuery.data.length > 0 ? (
          <RelacionesObjetosTable
            canEdit={puedeEscribir}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
            relaciones={relacionesQuery.data}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

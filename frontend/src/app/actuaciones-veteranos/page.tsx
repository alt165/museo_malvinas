"use client";

import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ActuacionesVeteranosTable } from "@/features/veteranos/components/actuaciones-veteranos-table";
import { useActuacionesVeteranosQuery, useBajaLogicaActuacionVeteranoMutation } from "@/features/veteranos/queries";
import { getApiErrorMessage } from "@/features/veteranos/utils";
import { canWrite, useAuth } from "@/lib/auth";
import { ApiClientError } from "@/lib/errors/api-error";

export default function ActuacionesVeteranosPage() {
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const { data = [], error, isError, isLoading } = useActuacionesVeteranosQuery();
  const bajaMutation = useBajaLogicaActuacionVeteranoMutation();

  function handleDelete(id: number) {
    if (window.confirm("Dar de baja esta actuacion?")) {
      bajaMutation.mutate(id);
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={puedeEscribir ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/actuaciones-veteranos/nueva">Nueva actuacion</Link> : null}
          description="Participaciones, unidades, roles y periodos de veteranos."
          title="Actuaciones de veteranos"
        />
        {isLoading ? <LoadingState label="Cargando actuaciones..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined} /> : null}
        {!isLoading && !isError && data.length === 0 ? <EmptyState title="Sin actuaciones" /> : null}
        {data.length > 0 ? (
          <ActuacionesVeteranosTable
            actuaciones={data}
            canEdit={puedeEscribir}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

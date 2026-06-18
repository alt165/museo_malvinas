"use client";

import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { DepositantesTable } from "@/features/depositantes/components/depositantes-table";
import { useBajaLogicaDepositanteMutation, useDepositantesQuery } from "@/features/depositantes/queries";
import { getApiErrorMessage } from "@/features/depositantes/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

export default function DepositantesPage() {
  const { canEdit: puedeEscribir } = useEditingMode();
  const depositantesQuery = useDepositantesQuery();
  const bajaMutation = useBajaLogicaDepositanteMutation();

  function handleDelete(id: number) {
    if (window.confirm("Dar de baja este depositante?")) {
      bajaMutation.mutate(id);
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={puedeEscribir ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/depositantes/nuevo">Nuevo depositante</Link> : null}
          description="Personas e instituciones depositantes vinculadas a objetos."
          title="Depositantes"
        />
        {depositantesQuery.isLoading ? <LoadingState label="Cargando depositantes..." /> : null}
        {depositantesQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(depositantesQuery.error)}
            requestId={depositantesQuery.error instanceof ApiClientError ? depositantesQuery.error.requestId : undefined}
          />
        ) : null}
        {bajaMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(bajaMutation.error)}
            requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined}
          />
        ) : null}
        {depositantesQuery.data?.length === 0 ? <EmptyState title="Sin depositantes" description="Todavia no hay depositantes registrados." /> : null}
        {depositantesQuery.data && depositantesQuery.data.length > 0 ? (
          <DepositantesTable
            canEdit={puedeEscribir}
            depositantes={depositantesQuery.data}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

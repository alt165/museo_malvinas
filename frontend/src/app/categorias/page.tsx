"use client";

import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { CategoriasTable } from "@/features/categorias/components/categorias-table";
import { useBajaLogicaCategoriaMutation, useCategoriasQuery } from "@/features/categorias/queries";
import { getApiErrorMessage } from "@/features/categorias/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function CategoriasPage() {
  const { canAdminEdit: puedeEscribir } = useEditingMode();
  const categoriasQuery = useCategoriasQuery();
  const bajaMutation = useBajaLogicaCategoriaMutation();

  function handleDelete(id: number) {
    if (window.confirm("Dar de baja esta categoria?")) {
      bajaMutation.mutate(id);
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader
          actions={puedeEscribir ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/categorias/nueva">Nueva categoria</Link> : null}
          description="Clasificacion de objetos del museo."
          title="Categorias"
        />
        {categoriasQuery.isLoading ? <LoadingState label="Cargando categorias..." /> : null}
        {categoriasQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(categoriasQuery.error)}
            requestId={categoriasQuery.error instanceof ApiClientError ? categoriasQuery.error.requestId : undefined}
          />
        ) : null}
        {bajaMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(bajaMutation.error)}
            requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined}
          />
        ) : null}
        {categoriasQuery.data?.length === 0 ? <EmptyState title="Sin categorias" description="Todavia no hay categorias registradas." /> : null}
        {categoriasQuery.data && categoriasQuery.data.length > 0 ? (
          <CategoriasTable
            canEdit={puedeEscribir}
            categorias={categoriasQuery.data}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

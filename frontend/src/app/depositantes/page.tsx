"use client";

import { Search, X } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { DepositantesTable } from "@/features/depositantes/components/depositantes-table";
import { useBajaLogicaDepositanteMutation, useBuscarDepositantesQuery } from "@/features/depositantes/queries";
import { getApiErrorMessage } from "@/features/depositantes/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";
import { normalizarTextoBusqueda } from "@/lib/utils";

export default function DepositantesPage() {
  const { canEdit: puedeEscribir } = useEditingMode();
  const bajaMutation = useBajaLogicaDepositanteMutation();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [busqueda, setBusqueda] = useState("");
  const [busquedaAplicada, setBusquedaAplicada] = useState("");
  const params = useMemo(() => ({ texto: busquedaAplicada, page, size }), [busquedaAplicada, page, size]);
  const depositantesQuery = useBuscarDepositantesQuery(params);
  const depositantes = depositantesQuery.data?.content ?? [];
  const hayBusqueda = normalizarTextoBusqueda(busquedaAplicada).length > 0;

  function aplicarBusqueda(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setBusquedaAplicada(busqueda.trim());
  }

  function limpiarFiltros() {
    setPage(0);
    setBusqueda("");
    setBusquedaAplicada("");
  }

  function handleDelete(id: number) {
    if (window.confirm("Dar de baja este depositante?")) {
      bajaMutation.mutate(id);
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          actions={puedeEscribir ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/depositantes/nuevo">Nuevo depositante</Link> : null}
          description="Personas e instituciones depositantes vinculadas a objetos."
          title="Depositantes"
        />
        <form className="rounded-lg border bg-card p-4" onSubmit={aplicarBusqueda}>
          <label className="block text-sm font-medium" htmlFor="buscar-depositante">
            Buscar depositante por nombre, DNI o CUIT
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20"
                id="buscar-depositante"
                onChange={(event) => setBusqueda(event.target.value)}
                placeholder="Nombre, DNI o CUIT"
                type="search"
                value={busqueda}
              />
            </div>
            <button className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90" type="submit">Buscar</button>
            {busqueda || busquedaAplicada ? (
              <button className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-3 text-sm font-medium hover:bg-muted" onClick={limpiarFiltros} type="button">
                <X className="h-4 w-4" />
                Limpiar filtros
              </button>
            ) : null}
          </div>
        </form>
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
        {!depositantesQuery.isLoading && !depositantesQuery.isError && depositantes.length === 0 && !hayBusqueda ? <EmptyState title="Sin depositantes" description="Todavia no hay depositantes registrados." /> : null}
        {!depositantesQuery.isLoading && !depositantesQuery.isError && hayBusqueda && depositantes.length === 0 ? <EmptyState title="Sin resultados" description="No hay depositantes que coincidan con la busqueda ingresada." /> : null}
        {!depositantesQuery.isLoading && !depositantesQuery.isError && depositantes.length > 0 ? (
          <DepositantesTable
            canEdit={puedeEscribir}
            depositantes={depositantes}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
          />
        ) : null}
        {!depositantesQuery.isLoading && !depositantesQuery.isError && depositantesQuery.data ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
            <div className="text-muted-foreground">
              {depositantesQuery.isFetching ? "Actualizando..." : depositantesQuery.data.totalElements + " depositantes encontrados"}
            </div>
            <div className="flex items-center gap-2">
              <label className="flex items-center gap-2">
                <span>Cantidad por página</span>
                <select
                  className="h-9 rounded-md border bg-white px-2"
                  onChange={(event) => {
                    setPage(0);
                    setSize(Number(event.target.value));
                  }}
                  value={size}
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>
              </label>
              <button
                className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                disabled={depositantesQuery.data.first}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                type="button"
              >Anterior</button>
              <span>Página {depositantesQuery.data.number + 1} de {Math.max(depositantesQuery.data.totalPages, 1)}</span>
              <button
                className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                disabled={depositantesQuery.data.last}
                onClick={() => setPage((current) => current + 1)}
                type="button"
              >Siguiente</button>
            </div>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

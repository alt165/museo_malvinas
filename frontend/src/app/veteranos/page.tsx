"use client";

import { Search, X } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { VeteranosTable } from "@/features/veteranos/components/veteranos-table";
import { useBajaLogicaVeteranoMutation, useBuscarVeteranosQuery } from "@/features/veteranos/queries";
import { getApiErrorMessage } from "@/features/veteranos/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

export default function VeteranosPage() {
  const { canEdit: puedeEscribir } = useEditingMode();
  const bajaMutation = useBajaLogicaVeteranoMutation();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [busqueda, setBusqueda] = useState("");
  const [busquedaAplicada, setBusquedaAplicada] = useState("");
  const params = useMemo(() => ({ texto: busquedaAplicada, page, size }), [busquedaAplicada, page, size]);
  const veteranosQuery = useBuscarVeteranosQuery(params);
  const veteranos = veteranosQuery.data?.content ?? [];
  const hayBusqueda = busquedaAplicada.trim().length > 0;

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
    if (window.confirm("Dar de baja este veterano?")) {
      bajaMutation.mutate(id);
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={puedeEscribir ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/veteranos/nuevo">Nueva persona</Link> : null}
          description="Registro de veteranos vinculados al acervo del museo."
          title="Personas"
        />
        <form className="rounded-lg border bg-card p-4" onSubmit={aplicarBusqueda}>
          <label className="block text-sm font-medium" htmlFor="buscar-veterano">
            Buscar persona por nombre, apellido o fuerza
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60"
                id="buscar-veterano"
                onChange={(event) => setBusqueda(event.target.value)}
                placeholder="Nombre, apellido o fuerza"
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
        {veteranosQuery.isLoading ? <LoadingState label="Cargando personas..." /> : null}
        {veteranosQuery.isError ? <ErrorState message={getApiErrorMessage(veteranosQuery.error)} requestId={veteranosQuery.error instanceof ApiClientError ? veteranosQuery.error.requestId : undefined} /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined} /> : null}
        {!veteranosQuery.isLoading && !veteranosQuery.isError && veteranos.length === 0 && !hayBusqueda ? <EmptyState title="Sin personas" description="Todavía no hay personas registrados." /> : null}
        {!veteranosQuery.isLoading && !veteranosQuery.isError && hayBusqueda && veteranos.length === 0 ? <EmptyState title="Sin resultados" description="No hay veteranos que coincidan con la busqueda ingresada." /> : null}
        {!veteranosQuery.isLoading && !veteranosQuery.isError && veteranos.length > 0 ? <VeteranosTable canEdit={puedeEscribir} isDeleting={bajaMutation.isPending} onDelete={handleDelete} veteranos={veteranos} /> : null}
        {!veteranosQuery.isLoading && !veteranosQuery.isError && veteranosQuery.data ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
            <div className="text-muted-foreground">
              {veteranosQuery.isFetching ? "Actualizando..." : veteranosQuery.data.totalElements + " personas encontradas"}
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
                disabled={veteranosQuery.data.first}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                type="button"
              >Anterior</button>
              <span>Página {veteranosQuery.data.number + 1} de {Math.max(veteranosQuery.data.totalPages, 1)}</span>
              <button
                className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                disabled={veteranosQuery.data.last}
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

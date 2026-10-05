"use client";

import Link from "next/link";
import { Search, X } from "lucide-react";
import { useMemo, useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ActuacionesVeteranosTable } from "@/features/veteranos/components/actuaciones-veteranos-table";
import { useBajaLogicaActuacionVeteranoMutation, useBuscarActuacionesVeteranosQuery } from "@/features/veteranos/queries";
import { getApiErrorMessage } from "@/features/veteranos/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

export default function ActuacionesVeteranosPage() {
  const { canEdit: puedeEscribir } = useEditingMode();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [busqueda, setBusqueda] = useState("");
  const [busquedaAplicada, setBusquedaAplicada] = useState("");
  const params = useMemo(() => ({ texto: busquedaAplicada, page, size }), [busquedaAplicada, page, size]);
  const actuacionesQuery = useBuscarActuacionesVeteranosQuery(params);
  const { data, error, isError, isFetching, isLoading } = actuacionesQuery;
  const actuaciones = data?.content ?? [];
  const bajaMutation = useBajaLogicaActuacionVeteranoMutation();

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
    if (window.confirm("Dar de baja esta actuacion?")) {
      bajaMutation.mutate(id);
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={puedeEscribir ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/actuaciones-veteranos/nueva">Nueva actuacion</Link> : null}
          description="Participaciones, unidades, roles y periodos de personas."
          title="Actuaciones de personas"
        />
        <form className="rounded-lg border bg-card p-4" onSubmit={aplicarBusqueda}>
          <label className="block text-sm font-medium" htmlFor="buscar-actuacion">
            Buscar actuación por persona o unidad militar
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20"
                id="buscar-actuacion"
                onChange={(event) => setBusqueda(event.target.value)}
                placeholder="Nombre de la persona o unidad militar"
                type="search"
                value={busqueda}
              />
            </div>
            <button className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90" type="submit">
              Buscar
            </button>
            {busqueda || busquedaAplicada ? (
              <button className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-3 text-sm font-medium hover:bg-muted" onClick={limpiarFiltros} type="button">
                <X className="h-4 w-4" />
                Limpiar filtros
              </button>
            ) : null}
          </div>
        </form>
        {isLoading ? <LoadingState label="Cargando actuaciones..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined} /> : null}
        {!isLoading && !isError && actuaciones.length === 0 && !busquedaAplicada ? <EmptyState title="Sin actuaciones" /> : null}
        {!isLoading && !isError && actuaciones.length === 0 && busquedaAplicada ? <EmptyState description="No hay actuaciones que coincidan con la búsqueda ingresada." title="Sin resultados" /> : null}
        {!isLoading && !isError && actuaciones.length > 0 ? (
          <ActuacionesVeteranosTable
            actuaciones={actuaciones}
            canEdit={puedeEscribir}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
          />
        ) : null}
        {!isLoading && !isError && data ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
            <div className="text-muted-foreground">
              {isFetching ? "Actualizando..." : `${data.totalElements} actuaciones encontradas`}
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
                disabled={data.first}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                type="button"
              >Anterior</button>
              <span>Página {data.number + 1} de {Math.max(data.totalPages, 1)}</span>
              <button
                className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                disabled={data.last}
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

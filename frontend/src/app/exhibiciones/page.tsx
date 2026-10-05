"use client";

import { Search, X } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ExhibicionesTable } from "@/features/exhibiciones/components/exhibiciones-table";
import { useBuscarExhibicionesQuery, useCancelarExhibicionPorIdMutation, useFinalizarExhibicionPorIdMutation } from "@/features/exhibiciones/queries";
import { getApiErrorMessage } from "@/features/exhibiciones/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { normalizarTextoBusqueda } from "@/lib/utils";

export default function ExhibicionesPage() {
  const { canEdit: puedeEscribir } = useEditingMode();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [finalizandoId, setFinalizandoId] = useState<number>();
  const [cancelandoId, setCancelandoId] = useState<number>();
  const finalizarMutation = useFinalizarExhibicionPorIdMutation();
  const cancelarMutation = useCancelarExhibicionPorIdMutation();
  const [busqueda, setBusqueda] = useState("");
  const [busquedaAplicada, setBusquedaAplicada] = useState("");
  const params = useMemo(() => ({ texto: busquedaAplicada, page, size }), [busquedaAplicada, page, size]);
  const { data, error, isError, isFetching, isLoading } = useBuscarExhibicionesQuery(params);
  const exhibiciones = data?.content ?? [];
  const normalizarBusqueda = normalizarTextoBusqueda(busquedaAplicada);
  const hayBusqueda = normalizarBusqueda.length > 0;

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

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            puedeEscribir ? (
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/exhibiciones/nueva">
                Nueva exhibición
              </Link>
            ) : null
          }
          description="Muestras temporales y permanentes del museo."
          title="Exhibiciones"
        />
        <form className="rounded-lg border bg-card p-4" onSubmit={aplicarBusqueda}>
          <label className="block text-sm font-medium" htmlFor="buscar-exhibicion">
            Buscar exhibición por nombre o descripción
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60"
                id="buscar-exhibicion"
                onChange={(event) => setBusqueda(event.target.value)}
                placeholder="Nombre o descripción"
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
        {isLoading ? <LoadingState label="Cargando exhibiciones..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {finalizarMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(finalizarMutation.error)}
            requestId={finalizarMutation.error instanceof ApiClientError ? finalizarMutation.error.requestId : undefined}
          />
        ) : null}
        {cancelarMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(cancelarMutation.error)}
            requestId={cancelarMutation.error instanceof ApiClientError ? cancelarMutation.error.requestId : undefined}
          />
        ) : null}
        {!isLoading && !isError && exhibiciones.length === 0 && !hayBusqueda ? (
          <EmptyState
            action={
              puedeEscribir ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/exhibiciones/nueva">
                  Nueva exhibición
                </Link>
              ) : null
            }
            description="Todavía no hay exhibiciones activas o planificadas."
            title="Sin exhibiciones"
          />
        ) : null}
        {!isLoading && !isError && hayBusqueda && exhibiciones.length === 0 ? (
          <EmptyState description="No hay exhibiciones que coincidan con la busqueda ingresada." title="Sin resultados" />
        ) : null}
        {!isLoading && !isError && exhibiciones.length > 0 ? (
          <ExhibicionesTable
            canEdit={puedeEscribir}
            exhibiciones={exhibiciones}
            cancelandoId={cancelarMutation.isPending ? cancelandoId : undefined}
            finalizandoId={finalizarMutation.isPending ? finalizandoId : undefined}
            onCancelar={(id) => {
              if (window.confirm("¿Confirma que desea cancelar esta exhibición? Los objetos asociados quedarán disponibles.")) {
                setCancelandoId(id);
                cancelarMutation.mutate(id);
              }
            }}
            onFinalizar={(id) => {
              if (window.confirm("Confirmar finalización de la exhibición")) {
                setFinalizandoId(id);
                finalizarMutation.mutate(id);
              }
            }}
          />
        ) : null}
        {!isLoading && !isError && data ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
            <div className="text-muted-foreground">
              {isFetching ? "Actualizando..." : `${data.totalElements} exhibiciones encontradas`}
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

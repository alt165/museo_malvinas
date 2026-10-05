"use client";

import { Search, X } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ColeccionesTable } from "@/features/colecciones/components/colecciones-table";
import { useBajaLogicaColeccionMutation, useBuscarColeccionesQuery } from "@/features/colecciones/queries";
import { getApiErrorMessage } from "@/features/colecciones/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { routes } from "@/lib/routes";

export default function ColeccionesPage() {
  const { canAdminEdit: puedeEliminar, canEdit: puedeEscribir } = useEditingMode();
  const bajaMutation = useBajaLogicaColeccionMutation();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [busquedaNombre, setBusquedaNombre] = useState("");
  const [busquedaAplicada, setBusquedaAplicada] = useState("");
  const params = useMemo(() => ({ nombre: busquedaAplicada, page, size }), [busquedaAplicada, page, size]);
  const coleccionesQuery = useBuscarColeccionesQuery(params);
  const colecciones = coleccionesQuery.data?.content ?? [];
  const hayBusqueda = busquedaAplicada.trim().length > 0;

  function aplicarBusqueda(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setBusquedaAplicada(busquedaNombre.trim());
  }

  function limpiarFiltros() {
    setPage(0);
    setBusquedaNombre("");
    setBusquedaAplicada("");
  }

  function handleDelete(id: number) {
    if (!puedeEliminar) {
      return;
    }

    if (window.confirm("Al eliminar esta colección, los objetos asociados quedarán sin colección. ¿Desea continuar?")) {
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
        <form className="rounded-lg border bg-card p-4" onSubmit={aplicarBusqueda}>
          <label className="block text-sm font-medium" htmlFor="buscar-coleccion-nombre">
            Buscar coleccion por nombre
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60"
                id="buscar-coleccion-nombre"
                onChange={(event) => setBusquedaNombre(event.target.value)}
                placeholder="Nombre de la coleccion"
                type="search"
                value={busquedaNombre}
              />
            </div>
            <button className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90" type="submit">Buscar</button>
            {busquedaNombre || busquedaAplicada ? (
              <button className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-3 text-sm font-medium hover:bg-muted" onClick={limpiarFiltros} type="button">
                <X className="h-4 w-4" />
                Limpiar filtros
              </button>
            ) : null}
          </div>
        </form>
        {coleccionesQuery.isLoading ? <LoadingState label="Cargando colecciones..." /> : null}
        {coleccionesQuery.isError ? (
          <ErrorState message={getApiErrorMessage(coleccionesQuery.error)} />
        ) : null}
        {bajaMutation.isError ? (
          <ErrorState message={getApiErrorMessage(bajaMutation.error)} title="No se pudo eliminar la colección" />
        ) : null}
        {!coleccionesQuery.isLoading && !coleccionesQuery.isError && colecciones.length === 0 && !hayBusqueda ? (
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
        {!coleccionesQuery.isLoading && !coleccionesQuery.isError && hayBusqueda && colecciones.length === 0 ? (
          <EmptyState description="No hay colecciones que coincidan con la busqueda ingresada." title="Sin resultados" />
        ) : null}
        {!coleccionesQuery.isLoading && !coleccionesQuery.isError && colecciones.length > 0 ? (
          <ColeccionesTable
            canDelete={puedeEliminar}
            canEdit={puedeEscribir}
            colecciones={colecciones}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
          />
        ) : null}
        {!coleccionesQuery.isLoading && !coleccionesQuery.isError && coleccionesQuery.data ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
            <div className="text-muted-foreground">
              {coleccionesQuery.isFetching ? "Actualizando..." : `${coleccionesQuery.data.totalElements} colecciones encontradas`}
            </div>
            <div className="flex items-center gap-2">
              <label className="flex items-center gap-2">
                <span>Cantidad de elementos por página</span>
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
                disabled={coleccionesQuery.data.first}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                type="button"
              >Anterior</button>
              <span>Página {coleccionesQuery.data.number + 1} de {Math.max(coleccionesQuery.data.totalPages, 1)}</span>
              <button
                className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                disabled={coleccionesQuery.data.last}
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

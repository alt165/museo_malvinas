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
  const [busquedaNombre, setBusquedaNombre] = useState("");

  const colecciones = useMemo(() => coleccionesQuery.data ?? [], [coleccionesQuery.data]);
  const valorBusqueda = busquedaNombre.trim().toLocaleLowerCase("es");
  const hayBusqueda = valorBusqueda.length > 0;
  const coleccionesFiltradas = useMemo(() => {
    if (!hayBusqueda) {
      return colecciones;
    }

    return colecciones.filter((coleccion) => coleccion.nombre.toLocaleLowerCase("es").includes(valorBusqueda));
  }, [colecciones, hayBusqueda, valorBusqueda]);

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
        <div className="rounded-lg border bg-card p-4">
          <label className="block text-sm font-medium" htmlFor="buscar-coleccion-nombre">
            Buscar coleccion por nombre
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={coleccionesQuery.isLoading}
                id="buscar-coleccion-nombre"
                onChange={(event) => setBusquedaNombre(event.target.value)}
                placeholder="Nombre de la coleccion"
                type="search"
                value={busquedaNombre}
              />
            </div>
            {busquedaNombre ? (
              <button
                className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-3 text-sm font-medium hover:bg-muted"
                onClick={() => setBusquedaNombre("")}
                type="button"
              >
                <X className="h-4 w-4" />
                Limpiar
              </button>
            ) : null}
          </div>
        </div>
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
        {!coleccionesQuery.isLoading && !coleccionesQuery.isError && hayBusqueda && coleccionesFiltradas.length === 0 ? (
          <EmptyState description="No hay colecciones que coincidan con la busqueda ingresada." title="Sin resultados" />
        ) : null}
        {!coleccionesQuery.isLoading && !coleccionesQuery.isError && coleccionesFiltradas.length > 0 ? (
          <ColeccionesTable
            canEdit={puedeEscribir}
            colecciones={coleccionesFiltradas}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

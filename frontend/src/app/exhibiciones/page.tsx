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
import { useCancelarExhibicionPorIdMutation, useExhibicionesQuery, useFinalizarExhibicionPorIdMutation } from "@/features/exhibiciones/queries";
import { getApiErrorMessage } from "@/features/exhibiciones/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { normalizarTextoBusqueda } from "@/lib/utils";

export default function ExhibicionesPage() {
  const { canEdit: puedeEscribir } = useEditingMode();
  const { data = [], error, isError, isLoading } = useExhibicionesQuery();
  const [finalizandoId, setFinalizandoId] = useState<number>();
  const [cancelandoId, setCancelandoId] = useState<number>();
  const finalizarMutation = useFinalizarExhibicionPorIdMutation();
  const cancelarMutation = useCancelarExhibicionPorIdMutation();
  const [busqueda, setBusqueda] = useState("");
  const valorBusqueda = normalizarTextoBusqueda(busqueda);
  const hayBusqueda = valorBusqueda.length > 0;
  const exhibicionesFiltradas = useMemo(() => {
    if (!hayBusqueda) {
      return data;
    }

    return data.filter((exhibicion) =>
      [exhibicion.nombre, exhibicion.descripcion].some((valor) => valor ? normalizarTextoBusqueda(valor).includes(valorBusqueda) : false)
    );
  }, [data, hayBusqueda, valorBusqueda]);

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
        <div className="rounded-lg border bg-card p-4">
          <label className="block text-sm font-medium" htmlFor="buscar-exhibicion">
            Buscar exhibición por nombre o descripción
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={isLoading}
                id="buscar-exhibicion"
                onChange={(event) => setBusqueda(event.target.value)}
                placeholder="Nombre o descripción"
                type="search"
                value={busqueda}
              />
            </div>
            {busqueda ? (
              <button
                className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-3 text-sm font-medium hover:bg-muted"
                onClick={() => setBusqueda("")}
                type="button"
              >
                <X className="h-4 w-4" />
                Limpiar
              </button>
            ) : null}
          </div>
        </div>
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
        {!isLoading && !isError && data.length === 0 && !hayBusqueda ? (
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
        {!isLoading && !isError && hayBusqueda && exhibicionesFiltradas.length === 0 ? (
          <EmptyState description="No hay exhibiciones que coincidan con la busqueda ingresada." title="Sin resultados" />
        ) : null}
        {!isLoading && !isError && exhibicionesFiltradas.length > 0 ? (
          <ExhibicionesTable
            canEdit={puedeEscribir}
            exhibiciones={exhibicionesFiltradas}
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
      </div>
    </AppShell>
  );
}

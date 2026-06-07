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
import { useBajaLogicaVeteranoMutation, useRelacionesObjetoVeteranoQuery, useVeteranosQuery } from "@/features/veteranos/queries";
import { getApiErrorMessage, veteranoCoincideConBusqueda } from "@/features/veteranos/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

export default function VeteranosPage() {
  const { canEdit: puedeEscribir } = useEditingMode();
  const veteranosQuery = useVeteranosQuery();
  const objetosQuery = useRelacionesObjetoVeteranoQuery();
  const bajaMutation = useBajaLogicaVeteranoMutation();
  const [busqueda, setBusqueda] = useState("");
  const objetos = objetosQuery.data ?? [];
  const veteranos = useMemo(() => veteranosQuery.data ?? [], [veteranosQuery.data]);
  const hayBusqueda = busqueda.trim().length > 0;
  const veteranosFiltrados = useMemo(
    () => veteranos.filter((veterano) => veteranoCoincideConBusqueda(veterano, busqueda)),
    [busqueda, veteranos]
  );

  function handleDelete(id: number) {
    if (window.confirm("Dar de baja este veterano?")) {
      bajaMutation.mutate(id);
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={puedeEscribir ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/veteranos/nuevo">Nuevo veterano</Link> : null}
          description="Registro de veteranos vinculados al acervo del museo."
          title="Veteranos"
        />
        <div className="rounded-lg border bg-card p-4">
          <label className="block text-sm font-medium" htmlFor="buscar-veterano">
            Buscar veterano por nombre, apellido o fuerza
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={veteranosQuery.isLoading}
                id="buscar-veterano"
                onChange={(event) => setBusqueda(event.target.value)}
                placeholder="Nombre, apellido o fuerza"
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
        {veteranosQuery.isLoading ? <LoadingState label="Cargando veteranos..." /> : null}
        {veteranosQuery.isError ? <ErrorState message={getApiErrorMessage(veteranosQuery.error)} requestId={veteranosQuery.error instanceof ApiClientError ? veteranosQuery.error.requestId : undefined} /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined} /> : null}
        {!veteranosQuery.isLoading && !veteranosQuery.isError && veteranos.length === 0 && !hayBusqueda ? <EmptyState title="Sin veteranos" description="Todavía no hay veteranos registrados." /> : null}
        {!veteranosQuery.isLoading && !veteranosQuery.isError && hayBusqueda && veteranosFiltrados.length === 0 ? <EmptyState title="Sin resultados" description="No hay veteranos que coincidan con la busqueda ingresada." /> : null}
        {!veteranosQuery.isLoading && !veteranosQuery.isError && veteranosFiltrados.length > 0 ? <VeteranosTable canEdit={puedeEscribir} isDeleting={bajaMutation.isPending} objetos={objetos} onDelete={handleDelete} veteranos={veteranosFiltrados} /> : null}
      </div>
    </AppShell>
  );
}

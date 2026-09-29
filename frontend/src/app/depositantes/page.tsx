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
import { useBajaLogicaDepositanteMutation, useDepositantesQuery } from "@/features/depositantes/queries";
import { getApiErrorMessage } from "@/features/depositantes/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";
import { normalizarTextoBusqueda } from "@/lib/utils";

export default function DepositantesPage() {
  const { canEdit: puedeEscribir } = useEditingMode();
  const depositantesQuery = useDepositantesQuery();
  const bajaMutation = useBajaLogicaDepositanteMutation();
  const [busqueda, setBusqueda] = useState("");
  const depositantes = useMemo(() => depositantesQuery.data ?? [], [depositantesQuery.data]);
  const valorBusqueda = normalizarTextoBusqueda(busqueda);
  const identificacionBuscada = valorBusqueda.replace(/[.\-\s]/g, "");
  const hayBusqueda = valorBusqueda.length > 0;
  const depositantesFiltrados = useMemo(() => {
    if (!hayBusqueda) {
      return depositantes;
    }

    return depositantes.filter((depositante) => {
      const nombre = normalizarTextoBusqueda(depositante.nombre);
      const dni = (depositante.dni ?? "").replace(/[.\-\s]/g, "");
      const cuit = (depositante.cuit ?? "").replace(/[.\-\s]/g, "");
      const coincideIdentificacion = identificacionBuscada.length > 0 && (dni.includes(identificacionBuscada) || cuit.includes(identificacionBuscada));
      return nombre.includes(valorBusqueda) || coincideIdentificacion;
    });
  }, [depositantes, hayBusqueda, identificacionBuscada, valorBusqueda]);

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
        <div className="rounded-lg border bg-card p-4">
          <label className="block text-sm font-medium" htmlFor="buscar-depositante">
            Buscar depositante por nombre, DNI o CUIT
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={depositantesQuery.isLoading}
                id="buscar-depositante"
                onChange={(event) => setBusqueda(event.target.value)}
                placeholder="Nombre, DNI o CUIT"
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
        {!depositantesQuery.isLoading && !depositantesQuery.isError && hayBusqueda && depositantesFiltrados.length === 0 ? <EmptyState title="Sin resultados" description="No hay depositantes que coincidan con la busqueda ingresada." /> : null}
        {!depositantesQuery.isLoading && !depositantesQuery.isError && depositantesFiltrados.length > 0 ? (
          <DepositantesTable
            canEdit={puedeEscribir}
            depositantes={depositantesFiltrados}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

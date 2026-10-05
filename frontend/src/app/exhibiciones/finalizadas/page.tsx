
"use client";

import { CopyPlus, Search, X } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { RowActionLink, RowActions } from "@/components/common/row-actions";
import { AppShell } from "@/components/layout/app-shell";
import { useExhibicionesFinalizadasQuery } from "@/features/exhibiciones/queries";
import { formatDate, getApiErrorMessage } from "@/features/exhibiciones/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function ExhibicionesFinalizadasPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [texto, setTexto] = useState("");
  const [textoAplicado, setTextoAplicado] = useState("");
  const params = useMemo(() => ({ texto: textoAplicado, page, size }), [textoAplicado, page, size]);
  const { data, error, isError, isFetching, isLoading } = useExhibicionesFinalizadasQuery(params, true);
  const exhibiciones = data?.content ?? [];

  function aplicarBusqueda(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setTextoAplicado(texto.trim());
  }

  function limpiarFiltros() {
    setPage(0);
    setTexto("");
    setTextoAplicado("");
  }

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          actions={
            <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/exhibiciones">
              Volver
            </Link>
          }
          description="Seleccione una exhibición finalizada para crear una nueva repetición."
          title="Exhibiciones finalizadas"
        />

        <form className="rounded-lg border p-4" onSubmit={aplicarBusqueda}>
          <div className="flex flex-col gap-2 sm:flex-row">
            <input
              className="h-10 min-w-0 flex-1 rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
              onChange={(event) => setTexto(event.target.value)}
              placeholder="Buscar exhibición finalizada"
              value={texto}
            />
            <button className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90" type="submit">Buscar</button>
            {texto || textoAplicado ? (
              <button className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-3 text-sm font-medium hover:bg-muted" onClick={limpiarFiltros} onMouseDown={(event) => event.preventDefault()} type="button">
                <X className="h-4 w-4" />
                Limpiar filtros
              </button>
            ) : null}
          </div>
        </form>

        {isLoading || isFetching ? <LoadingState label="Cargando exhibiciones finalizadas..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {!isLoading && !isError && exhibiciones.length === 0 ? <EmptyState description={textoAplicado ? "No hay exhibiciones finalizadas que coincidan con la búsqueda." : "No hay exhibiciones finalizadas para repetir."} title="Sin exhibiciones finalizadas" /> : null}
        {!isLoading && !isError && exhibiciones.length > 0 ? (
          <div className="overflow-hidden rounded-lg border">
            <table className="w-full border-collapse text-sm">
              <thead className="bg-muted/60">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Nombre</th>
                  <th className="px-4 py-3 text-left font-medium">Tipo</th>
                  <th className="px-4 py-3 text-left font-medium">Periodo</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {exhibiciones.map((exhibicion) => (
                  <tr className="border-t" key={exhibicion.id}>
                    <td className="px-4 py-3 align-top font-medium">{exhibicion.nombre}</td>
                    <td className="px-4 py-3 align-top">{exhibicion.tipo}</td>
                    <td className="px-4 py-3 align-top">{formatDate(exhibicion.fechaInicio)} - {formatDate(exhibicion.fechaFin)}</td>
                    <td className="px-4 py-3 align-top">
                      <RowActions>
                        <RowActionLink href={`/exhibiciones/${exhibicion.id}`} icon={Search} label="Ver" />
                        <RowActionLink href={`/exhibiciones/repetir/${exhibicion.id}`} icon={CopyPlus} label="Repetir" />
                      </RowActions>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
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

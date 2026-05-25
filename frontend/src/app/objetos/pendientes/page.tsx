"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { ArrowDown, ArrowUp, ChevronsUpDown, Download, Pencil } from "lucide-react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { descargarReciboPdf } from "@/features/objetos/api";
import { useObjetosPendientesCompletarQuery } from "@/features/objetos/queries";
import type {
  ObjetoPendienteSortField,
  ObjetosPendientesSort
} from "@/features/objetos/types";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function abrirBlob(blob: Blob, nombre: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = nombre;
  link.click();
  URL.revokeObjectURL(url);
}

export default function ObjetosPendientesPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [sort, setSort] = useState<ObjetosPendientesSort>({ field: "fechaCargaRapida", direction: "asc" });
  const params = useMemo(() => ({ page, size, sort: `${sort.field},${sort.direction}` }), [page, size, sort]);
  const { data, error, isError, isFetching, isLoading } = useObjetosPendientesCompletarQuery(params);
  const objetos = data?.content ?? [];

  function handleSortChange(field: ObjetoPendienteSortField) {
    setPage(0);
    setSort((current) => ({
      field,
      direction: current.field === field && current.direction === "asc" ? "desc" : "asc"
    }));
  }

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          actions={
            <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos/carga-rapida">
              Alta rapida
            </Link>
          }
          description="Objetos ingresados con datos minimos que requieren completar la ficha patrimonial."
          title="Pendientes de completar"
        />
        {isLoading ? <LoadingState label="Cargando pendientes..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {!isLoading && !isError && objetos.length === 0 ? (
          <EmptyState
            description="No hay objetos de alta rapida pendientes de completar."
            title="Sin pendientes"
          />
        ) : null}
        {!isLoading && !isError && objetos.length > 0 ? (
          <div className="space-y-3">
            <div className="overflow-hidden rounded-lg border">
              <table className="w-full border-collapse text-sm">
                <thead className="bg-primary text-primary-foreground">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold text-white">
                      <SortHeader field="numeroInventario" label="Numero de inventario" onSortChange={handleSortChange} sort={sort} />
                    </th>
                    <th className="px-4 py-3 text-left font-semibold text-white">
                      <SortHeader field="denominacionObjeto" label="Denominacion" onSortChange={handleSortChange} sort={sort} />
                    </th>
                    <th className="px-4 py-3 text-left font-semibold text-white">Depositante</th>
                    <th className="px-4 py-3 text-left font-semibold text-white">
                      <SortHeader field="fechaCargaRapida" label="Fecha alta rapida" onSortChange={handleSortChange} sort={sort} />
                    </th>
                    <th className="px-4 py-3 text-left font-semibold text-white">Cargado por</th>
                    <th className="px-4 py-3 text-right font-semibold text-white">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {objetos.map((objeto) => (
                    <tr className="border-t" key={objeto.id}>
                      <td className="px-4 py-3 align-top font-medium">{objeto.numeroInventario}</td>
                      <td className="px-4 py-3 align-top">
                        <p>{objeto.denominacionObjeto}</p>
                        {objeto.descripcion ? <p className="mt-1 text-xs text-muted-foreground">{objeto.descripcion}</p> : null}
                      </td>
                      <td className="px-4 py-3 align-top">{objeto.depositanteNombre || "Sin depositante"}</td>
                      <td className="px-4 py-3 align-top">{formatearFechaHora(objeto.fechaCargaRapida)}</td>
                      <td className="px-4 py-3 align-top">{objeto.cargaRapidaPor || "Sin dato"}</td>
                      <td className="px-4 py-3 align-top">
                        <div className="flex justify-end gap-2">
                          {objeto.reciboId ? (
                            <button
                              className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted"
                              onClick={async () => {
                                if (!objeto.reciboId) {
                                  return;
                                }
                                abrirBlob(await descargarReciboPdf(objeto.reciboId), `recibo-${objeto.reciboId}.pdf`);
                              }}
                              type="button"
                            >
                              <Download className="h-3.5 w-3.5" />
                              Recibo
                            </button>
                          ) : null}
                          <Link
                            className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted"
                            href={`/objetos/${objeto.id}/editar`}
                          >
                            <Pencil className="h-3.5 w-3.5" />
                            Continuar carga
                          </Link>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
              <div className="text-muted-foreground">
                {isFetching ? "Actualizando..." : `${data?.totalElements ?? 0} elementos encontrados`}
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
                  disabled={data?.first ?? true}
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                  type="button"
                >
                  Anterior
                </button>
                <span>
                  Pagina {(data?.number ?? 0) + 1} de {Math.max(data?.totalPages ?? 1, 1)}
                </span>
                <button
                  className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={data?.last ?? true}
                  onClick={() => setPage((current) => current + 1)}
                  type="button"
                >
                  Siguiente
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

function SortHeader({
  field,
  label,
  onSortChange,
  sort
}: {
  field: ObjetoPendienteSortField;
  label: string;
  onSortChange: (field: ObjetoPendienteSortField) => void;
  sort: ObjetosPendientesSort;
}) {
  const activo = sort.field === field;
  const Icon = activo ? (sort.direction === "asc" ? ArrowUp : ArrowDown) : ChevronsUpDown;

  return (
    <button
      className="inline-flex items-center gap-1 text-left font-semibold text-white hover:text-secondary"
      onClick={() => onSortChange(field)}
      type="button"
    >
      <span>{label}</span>
      <Icon className="h-3.5 w-3.5" />
    </button>
  );
}

function formatearFechaHora(value?: string | null) {
  if (!value) {
    return "Sin dato";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("es-AR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(date);
}

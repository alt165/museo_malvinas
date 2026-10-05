"use client";

import { RotateCcw, Search, X } from "lucide-react";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { RowActionButton, RowActions } from "@/components/common/row-actions";
import { AppShell } from "@/components/layout/app-shell";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";
import { useObjetosEliminadosQuery, useRestaurarObjetoMutation } from "@/features/objetos/queries";
import { formatDateTimeArgentina } from "@/lib/date-time";
import type { ObjetoMuseoEliminadoResponseDTO } from "@/features/objetos/types";
import { getApiErrorMessage, resumenDescripcion } from "@/features/objetos/utils";

export default function ObjetosEliminadosPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [texto, setTexto] = useState("");
  const [textoAplicado, setTextoAplicado] = useState("");
  const [mensaje, setMensaje] = useState<string | null>(null);
  useEffect(() => {
    const timeout = window.setTimeout(() => setTextoAplicado(texto.trim()), 300);
    return () => window.clearTimeout(timeout);
  }, [texto]);
  const buscando = texto.trim() !== textoAplicado;
  const params = useMemo(() => ({ texto: textoAplicado, page, size, sort: "fechaEliminacion,desc" }), [textoAplicado, page, size]);
  const eliminadosQuery = useObjetosEliminadosQuery(params);
  const restaurar = useRestaurarObjetoMutation();
  const objetos = eliminadosQuery.data?.content ?? [];

  function handleRestaurar(objeto: ObjetoMuseoEliminadoResponseDTO) {
    if (!window.confirm(`Restaurar el objeto ${objeto.numeroInventario}?`)) {
      return;
    }
    setMensaje(null);
    restaurar.mutate(objeto.id, {
      onSuccess: () => {
        setMensaje("Objeto restaurado correctamente.");
      }
    });
  }

  return (
    <AppShell requiredRoles={routePermissions.admin}>
      <div className="space-y-6">
        <PageHeader
          actions={
            <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos">
              Volver a consulta
            </Link>
          }
          description="Objetos dados de baja logicamente. Solo visible para administradores."
          title="Objetos eliminados"
        />

        {mensaje ? <div className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">{mensaje}</div> : null}
        {restaurar.isError ? (
          <ErrorState
            message={getApiErrorMessage(restaurar.error)}
            requestId={restaurar.error instanceof ApiClientError ? restaurar.error.requestId : undefined}
          />
        ) : null}
        <div className="max-w-2xl">
          <label className="text-sm font-medium" htmlFor="buscar-objetos-eliminados">
            Buscar objetos eliminados
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20"
                id="buscar-objetos-eliminados"
                onChange={(event) => {
                  setTexto(event.target.value);
                  setPage(0);
                }}
                placeholder="Número, denominación, descripción o usuario"
                type="search"
                value={texto}
              />
            </div>
            {texto ? (
              <button
                className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-4 text-sm font-medium hover:bg-muted"
                onClick={() => {
                  setTexto("");
                  setTextoAplicado("");
                  setPage(0);
                }}
                onMouseDown={(event) => event.preventDefault()}
                type="button"
              >
                <X className="h-4 w-4" />
                Limpiar
              </button>
            ) : null}
          </div>
        </div>
        {eliminadosQuery.isLoading ? <LoadingState label="Cargando objetos eliminados..." /> : null}
        {eliminadosQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(eliminadosQuery.error)}
            requestId={eliminadosQuery.error instanceof ApiClientError ? eliminadosQuery.error.requestId : undefined}
          />
        ) : null}

        {!eliminadosQuery.isLoading && !eliminadosQuery.isError && !buscando && objetos.length === 0 ? (
          <EmptyState description="No hay objetos eliminados para restaurar." title="Sin objetos eliminados" />
        ) : null}

        {!eliminadosQuery.isLoading && !eliminadosQuery.isError && !buscando && objetos.length > 0 ? (
          <div className="space-y-3">
            <div className="overflow-hidden rounded-lg border">
              <table className="w-full border-collapse text-sm">
                <thead className="bg-primary text-white">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold">Numero de inventario</th>
                    <th className="px-4 py-3 text-left font-semibold">Denominacion</th>
                    <th className="px-4 py-3 text-left font-semibold">Descripcion</th>
                    <th className="px-4 py-3 text-left font-semibold">Eliminado por</th>
                    <th className="px-4 py-3 text-left font-semibold">Fecha eliminacion</th>
                    <th className="px-4 py-3 text-right font-semibold">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {objetos.map((objeto) => (
                    <tr className="border-t" key={objeto.id}>
                      <td className="px-4 py-3 align-top font-medium">{objeto.numeroInventario}</td>
                      <td className="px-4 py-3 align-top">{objeto.denominacionObjeto}</td>
                      <td className="px-4 py-3 align-top text-muted-foreground">{resumenDescripcion(objeto.descripcion)}</td>
                      <td className="px-4 py-3 align-top">{objeto.eliminadoPor || "Sin dato"}</td>
                      <td className="px-4 py-3 align-top">
                        {formatDateTimeArgentina(objeto.fechaEliminacion, "Sin dato")}
                      </td>
                      <td className="px-4 py-3 text-right align-top">
                        <RowActions>
                          <RowActionButton disabled={restaurar.isPending} icon={RotateCcw} label="Restaurar" onClick={() => handleRestaurar(objeto)} />
                        </RowActions>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
              <div className="text-muted-foreground">{eliminadosQuery.data?.totalElements ?? 0} elementos encontrados</div>
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
                  disabled={eliminadosQuery.data?.first ?? true}
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                  type="button"
                >
                  Anterior
                </button>
                <span>
                  Pagina {(eliminadosQuery.data?.number ?? 0) + 1} de {Math.max(eliminadosQuery.data?.totalPages ?? 1, 1)}
                </span>
                <button
                  className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={eliminadosQuery.data?.last ?? true}
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

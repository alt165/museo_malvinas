"use client";

import Link from "next/link";
import { useMemo, useState, type FormEvent } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { canWrite, useAuth } from "@/lib/auth";
import { useBajaLogicaObjetoMutation, useBuscarObjetosQuery } from "@/features/objetos/queries";
import { ObjetosTable } from "@/features/objetos/components/objetos-table";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { useCategoriasQuery } from "@/features/categorias/queries";
import type { ObjetoMuseoResponseDTO } from "@/features/objetos/types";

type FiltrosObjetos = {
  nombre: string;
  numeroInventario: string;
  categoriaIds: number[];
};

const filtrosIniciales: FiltrosObjetos = {
  nombre: "",
  numeroInventario: "",
  categoriaIds: []
};

export default function ObjetosPage() {
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const [filtrosFormulario, setFiltrosFormulario] = useState<FiltrosObjetos>(filtrosIniciales);
  const [filtrosAplicados, setFiltrosAplicados] = useState<FiltrosObjetos>(filtrosIniciales);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const categoriasQuery = useCategoriasQuery();
  const buscarParams = useMemo(
    () => ({
      ...filtrosAplicados,
      page,
      size,
      sort: "numeroInventario,asc"
    }),
    [filtrosAplicados, page, size]
  );
  const { data, error, isError, isLoading, isFetching } = useBuscarObjetosQuery(buscarParams);
  const bajaLogica = useBajaLogicaObjetoMutation();
  const objetos = data?.content ?? [];
  const hayFiltros = Boolean(
    filtrosAplicados.nombre || filtrosAplicados.numeroInventario || filtrosAplicados.categoriaIds.length > 0
  );

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setFiltrosAplicados({
      nombre: filtrosFormulario.nombre.trim(),
      numeroInventario: filtrosFormulario.numeroInventario.trim(),
      categoriaIds: filtrosFormulario.categoriaIds
    });
  }

  function handleLimpiarFiltros() {
    setPage(0);
    setFiltrosFormulario(filtrosIniciales);
    setFiltrosAplicados(filtrosIniciales);
  }

  function handleBajaLogica(objeto: ObjetoMuseoResponseDTO) {
    if (window.confirm(`Dar de baja el objeto ${objeto.numeroInventario}?`)) {
      bajaLogica.mutate(objeto.id);
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            puedeEscribir ? (
              <div className="flex flex-wrap gap-2">
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos/carga-rapida">
                  Carga rapida
                </Link>
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos/nuevo">
                  Nuevo objeto
                </Link>
              </div>
            ) : null
          }
          description="Catalogo de objetos patrimoniales registrados en el museo."
          title="Objetos del museo"
        />
        <form className="rounded-lg border bg-surface p-4 shadow-sm" onSubmit={handleSubmit}>
          <div className="grid gap-4 md:grid-cols-[1fr_1fr_1.2fr_auto] md:items-end">
            <label className="space-y-1 text-sm font-medium text-primary">
              <span>Nombre del objeto</span>
              <input
                className="h-10 w-full rounded-md border bg-white px-3 text-sm outline-none focus:border-primary"
                onChange={(event) => setFiltrosFormulario((current) => ({ ...current, nombre: event.target.value }))}
                type="text"
                value={filtrosFormulario.nombre}
              />
            </label>
            <label className="space-y-1 text-sm font-medium text-primary">
              <span>Número de inventario</span>
              <input
                className="h-10 w-full rounded-md border bg-white px-3 text-sm outline-none focus:border-primary"
                onChange={(event) =>
                  setFiltrosFormulario((current) => ({ ...current, numeroInventario: event.target.value }))
                }
                type="text"
                value={filtrosFormulario.numeroInventario}
              />
            </label>
            <label className="space-y-1 text-sm font-medium text-primary">
              <span>Categorías</span>
              <select
                className="min-h-10 w-full rounded-md border bg-white px-3 py-2 text-sm outline-none focus:border-primary"
                disabled={categoriasQuery.isLoading}
                multiple
                onChange={(event) =>
                  setFiltrosFormulario((current) => ({
                    ...current,
                    categoriaIds: Array.from(event.target.selectedOptions).map((option) => Number(option.value))
                  }))
                }
                value={filtrosFormulario.categoriaIds.map(String)}
              >
                {(categoriasQuery.data ?? []).map((categoria) => (
                  <option key={categoria.id} value={categoria.id}>
                    {categoria.nombre}
                  </option>
                ))}
              </select>
            </label>
            <div className="flex flex-wrap gap-2">
              <button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-white hover:opacity-90" type="submit">
                Buscar
              </button>
              <button
                className="h-10 rounded-md border px-4 text-sm font-medium hover:bg-muted"
                onClick={handleLimpiarFiltros}
                type="button"
              >
                Limpiar filtros
              </button>
            </div>
          </div>
        </form>
        {isLoading ? <LoadingState label="Cargando objetos..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {!isLoading && !isError && objetos.length === 0 ? (
          <EmptyState
            action={
              puedeEscribir ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos/nuevo">
                  Nuevo objeto
                </Link>
              ) : null
            }
            description={hayFiltros ? "No hay objetos que coincidan con los filtros aplicados." : "Todavia no hay objetos activos registrados."}
            title={hayFiltros ? "Sin resultados" : "Sin objetos"}
          />
        ) : null}
        {!isLoading && !isError && objetos.length > 0 ? (
          <div className="space-y-3">
            <ObjetosTable
              canEdit={puedeEscribir}
              deletingId={bajaLogica.variables ?? null}
              objetos={objetos}
              onDelete={puedeEscribir ? handleBajaLogica : undefined}
            />
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
              <div className="text-muted-foreground">
                {isFetching ? "Actualizando..." : `${data?.totalElements ?? 0} resultado(s)`}
              </div>
              <div className="flex items-center gap-2">
                <label className="flex items-center gap-2">
                  <span>Tamaño</span>
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
                  Página {(data?.number ?? 0) + 1} de {Math.max(data?.totalPages ?? 1, 1)}
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

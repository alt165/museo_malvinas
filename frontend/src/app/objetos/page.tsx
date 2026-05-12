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
import type { ObjetoMuseoResponseDTO, ObjetoSortField, ObjetosSort } from "@/features/objetos/types";

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
  const [categoriaBusqueda, setCategoriaBusqueda] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [sort, setSort] = useState<ObjetosSort>({ field: "numeroInventario", direction: "asc" });
  const categoriasQuery = useCategoriasQuery();
  const categorias = useMemo(() => categoriasQuery.data ?? [], [categoriasQuery.data]);
  const categoriasFiltradas = useMemo(() => {
    const busqueda = categoriaBusqueda.trim().toLowerCase();
    if (!busqueda) {
      return categorias;
    }
    return categorias.filter((categoria) => categoria.nombre.toLowerCase().includes(busqueda));
  }, [categoriaBusqueda, categorias]);
  const categoriasSeleccionadas = useMemo(
    () => categorias.filter((categoria) => filtrosFormulario.categoriaIds.includes(categoria.id)),
    [categorias, filtrosFormulario.categoriaIds]
  );
  const buscarParams = useMemo(
    () => ({
      ...filtrosAplicados,
      page,
      size,
      sort: `${sort.field},${sort.direction}`
    }),
    [filtrosAplicados, page, size, sort]
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
    setCategoriaBusqueda("");
  }

  function handleBajaLogica(objeto: ObjetoMuseoResponseDTO) {
    if (window.confirm(`Dar de baja el objeto ${objeto.numeroInventario}?`)) {
      bajaLogica.mutate(objeto.id);
    }
  }

  function toggleCategoria(categoriaId: number) {
    setFiltrosFormulario((current) => {
      const seleccionada = current.categoriaIds.includes(categoriaId);
      return {
        ...current,
        categoriaIds: seleccionada
          ? current.categoriaIds.filter((id) => id !== categoriaId)
          : [...current.categoriaIds, categoriaId]
      };
    });
  }

  function handleSortChange(field: ObjetoSortField) {
    setPage(0);
    setSort((current) => ({
      field,
      direction: current.field === field && current.direction === "asc" ? "desc" : "asc"
    }));
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
          <div className="grid gap-4 lg:grid-cols-[1fr_1fr_auto] lg:items-end">
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
            <div className="flex flex-wrap gap-2 lg:justify-end">
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
            <section className="space-y-3 lg:col-span-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <label className="text-sm font-medium text-primary" htmlFor="categoria-busqueda">
                  Categorías
                </label>
                <span className="text-xs text-muted-foreground">
                  {filtrosFormulario.categoriaIds.length} seleccionada(s)
                </span>
              </div>
              <input
                className="h-10 w-full rounded-md border bg-white px-3 text-sm outline-none focus:border-primary"
                id="categoria-busqueda"
                onChange={(event) => setCategoriaBusqueda(event.target.value)}
                placeholder="Buscar categoría"
                type="text"
                value={categoriaBusqueda}
              />
              <div className="max-h-44 overflow-y-auto rounded-md border bg-white p-2">
                {categoriasQuery.isLoading ? (
                  <p className="px-2 py-2 text-sm text-muted-foreground">Cargando categorías...</p>
                ) : null}
                {!categoriasQuery.isLoading && categoriasFiltradas.length === 0 ? (
                  <p className="px-2 py-2 text-sm text-muted-foreground">Sin categorías disponibles.</p>
                ) : null}
                <div className="grid gap-1 sm:grid-cols-2 lg:grid-cols-3">
                  {categoriasFiltradas.map((categoria) => (
                    <label
                      className="flex min-h-9 items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-muted"
                      key={categoria.id}
                    >
                      <input
                        checked={filtrosFormulario.categoriaIds.includes(categoria.id)}
                        className="h-4 w-4 accent-primary"
                        onChange={() => toggleCategoria(categoria.id)}
                        type="checkbox"
                      />
                      <span className="min-w-0 flex-1 truncate">{categoria.nombre}</span>
                    </label>
                  ))}
                </div>
              </div>
              {categoriasSeleccionadas.length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {categoriasSeleccionadas.map((categoria) => (
                    <button
                      className="rounded-full border border-primary/20 bg-secondary/20 px-3 py-1 text-xs font-medium text-primary hover:bg-secondary/30"
                      key={categoria.id}
                      onClick={() => toggleCategoria(categoria.id)}
                      type="button"
                    >
                      {categoria.nombre} ×
                    </button>
                  ))}
                </div>
              ) : null}
            </section>
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
              onSortChange={handleSortChange}
              sort={sort}
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

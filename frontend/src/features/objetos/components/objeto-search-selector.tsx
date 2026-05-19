"use client";

import { useId, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { useCategoriasQuery } from "@/features/categorias/queries";
import { useBuscarObjetosQuery } from "@/features/objetos/queries";
import type { ObjetoMuseoResponseDTO, ObjetoSortField, ObjetosSort } from "@/features/objetos/types";
import { getApiErrorMessage, resumenDescripcion } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";

type FiltrosObjetos = {
  nombre: string;
  numeroInventario: string;
  categoriaIds: number[];
};

type ObjetoSearchSelectorProps = {
  title?: string;
  description?: string;
  selectedObjeto?: ObjetoMuseoResponseDTO | null;
  excludeObjetoId?: number;
  selectLabel?: string;
  emptyLabel?: string;
  renderActions?: (objeto: ObjetoMuseoResponseDTO) => ReactNode;
  onSelect?: (objeto: ObjetoMuseoResponseDTO) => void;
};

const filtrosIniciales: FiltrosObjetos = {
  nombre: "",
  numeroInventario: "",
  categoriaIds: []
};

export function ObjetoSearchSelector({
  description,
  emptyLabel = "No hay objetos que coincidan con los filtros aplicados.",
  excludeObjetoId,
  onSelect,
  renderActions,
  selectedObjeto,
  selectLabel = "Seleccionar",
  title = "Buscar objeto"
}: ObjetoSearchSelectorProps) {
  const categoriaBusquedaId = useId();
  const [filtrosFormulario, setFiltrosFormulario] = useState<FiltrosObjetos>(filtrosIniciales);
  const [filtrosAplicados, setFiltrosAplicados] = useState<FiltrosObjetos>(filtrosIniciales);
  const [categoriaBusqueda, setCategoriaBusqueda] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
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
  const { data, error, isError, isFetching, isLoading } = useBuscarObjetosQuery(buscarParams);
  const objetos = useMemo(
    () => (data?.content ?? []).filter((objeto) => objeto.id !== excludeObjetoId),
    [data?.content, excludeObjetoId]
  );
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
    <section className="space-y-4 rounded-lg border bg-surface p-4 shadow-sm">
      <div className="space-y-1">
        <h2 className="text-base font-semibold text-primary">{title}</h2>
        {description ? <p className="text-sm text-muted-foreground">{description}</p> : null}
      </div>

      {selectedObjeto ? (
        <div className="rounded-md border border-primary/20 bg-secondary/10 p-3 text-sm">
          <p className="font-medium text-primary">Objeto seleccionado</p>
          <p className="mt-1">
            {selectedObjeto.numeroInventario} - {selectedObjeto.denominacionObjeto}
          </p>
        </div>
      ) : null}

      <form className="space-y-4" onSubmit={handleSubmit}>
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
            <span>Numero de inventario</span>
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
              Limpiar
            </button>
          </div>
        </div>

        <section className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <label className="text-sm font-medium text-primary" htmlFor={categoriaBusquedaId}>
              Categorias
            </label>
            <span className="text-xs text-muted-foreground">{filtrosFormulario.categoriaIds.length} seleccionada(s)</span>
          </div>
          <input
            className="h-10 w-full rounded-md border bg-white px-3 text-sm outline-none focus:border-primary"
            id={categoriaBusquedaId}
            onChange={(event) => setCategoriaBusqueda(event.target.value)}
            placeholder="Buscar categoria"
            type="text"
            value={categoriaBusqueda}
          />
          <div className="max-h-40 overflow-y-auto rounded-md border bg-white p-2">
            {categoriasQuery.isLoading ? <p className="px-2 py-2 text-sm text-muted-foreground">Cargando categorias...</p> : null}
            {!categoriasQuery.isLoading && categoriasFiltradas.length === 0 ? (
              <p className="px-2 py-2 text-sm text-muted-foreground">Sin categorias disponibles.</p>
            ) : null}
            <div className="grid gap-1 sm:grid-cols-2 lg:grid-cols-3">
              {categoriasFiltradas.map((categoria) => (
                <label className="flex min-h-9 items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-muted" key={categoria.id}>
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
                  {categoria.nombre} x
                </button>
              ))}
            </div>
          ) : null}
        </section>
      </form>

      {isLoading ? <LoadingState label="Cargando objetos..." /> : null}
      {isError ? (
        <ErrorState
          message={getApiErrorMessage(error)}
          requestId={error instanceof ApiClientError ? error.requestId : undefined}
        />
      ) : null}
      {!isLoading && !isError && objetos.length === 0 ? (
        <div className="rounded-md border border-dashed p-5 text-center text-sm text-muted-foreground">
          {hayFiltros ? emptyLabel : "Use los filtros para buscar y seleccionar un objeto."}
        </div>
      ) : null}
      {!isLoading && !isError && objetos.length > 0 ? (
        <div className="space-y-3">
          <div className="overflow-hidden rounded-lg border">
            <table className="w-full border-collapse text-sm">
              <thead className="bg-muted/60">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">
                    <button className="font-medium hover:underline" onClick={() => handleSortChange("numeroInventario")} type="button">
                      Numero de inventario
                    </button>
                  </th>
                  <th className="px-4 py-3 text-left font-medium">
                    <button className="font-medium hover:underline" onClick={() => handleSortChange("denominacionObjeto")} type="button">
                      Denominacion
                    </button>
                  </th>
                  <th className="px-4 py-3 text-left font-medium">Categorias</th>
                  <th className="px-4 py-3 text-left font-medium">Descripcion</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {objetos.map((objeto) => (
                  <tr className="border-t" key={objeto.id}>
                    <td className="px-4 py-3 align-top font-medium">{objeto.numeroInventario}</td>
                    <td className="px-4 py-3 align-top">{objeto.denominacionObjeto}</td>
                    <td className="px-4 py-3 align-top text-muted-foreground">
                      {objeto.categorias?.length ? objeto.categorias.map((categoria) => categoria.nombre).join(", ") : "Sin categoria"}
                    </td>
                    <td className="px-4 py-3 align-top text-muted-foreground">{resumenDescripcion(objeto.descripcion)}</td>
                    <td className="px-4 py-3 text-right align-top">
                      {renderActions ? (
                        renderActions(objeto)
                      ) : onSelect ? (
                        <button
                          className="rounded-md border px-3 py-1.5 text-xs font-medium hover:bg-muted"
                          onClick={() => onSelect(objeto)}
                          type="button"
                        >
                          {selectLabel}
                        </button>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
            <div className="text-muted-foreground">{isFetching ? "Actualizando..." : `${data?.totalElements ?? 0} resultado(s)`}</div>
            <div className="flex items-center gap-2">
              <label className="flex items-center gap-2">
                <span>Tamano</span>
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
    </section>
  );
}

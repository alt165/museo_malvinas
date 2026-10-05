"use client";
import { FormLabel } from "@/components/common/form-label";

import { Pencil, Search, Trash2, X } from "lucide-react";
import { useMemo, useState } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { RowActionButton, RowActions } from "@/components/common/row-actions";
import { AppShell } from "@/components/layout/app-shell";
import { routePermissions } from "@/lib/routes";
import {
  useActualizarDetalleConservacionMutation,
  useBajaLogicaDetalleConservacionMutation,
  useCrearDetalleConservacionMutation,
  useDetallesConservacionQuery
} from "../queries";
import type { DetalleConservacionRequestDTO, DetalleConservacionResponseDTO } from "../types";

export function DetallesConservacionAdminPage() {
  const query = useDetallesConservacionQuery();
  const crear = useCrearDetalleConservacionMutation();
  const actualizar = useActualizarDetalleConservacionMutation();
  const baja = useBajaLogicaDetalleConservacionMutation();
  const [editing, setEditing] = useState<DetalleConservacionResponseDTO | null>(null);
  const [form, setForm] = useState<DetalleConservacionRequestDTO>({ nombre: "", codigo: "", descripcion: "" });
  const [textoBusqueda, setTextoBusqueda] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const error = crear.error || actualizar.error || baja.error || query.error;
  const isSubmitting = crear.isPending || actualizar.isPending;
  const detallesFiltrados = useMemo(() => {
    const filtro = normalizarTextoBusqueda(textoBusqueda);
    const detalles = query.data ?? [];
    if (!filtro) {
      return detalles;
    }
    return detalles.filter((detalle) =>
      [detalle.codigo, detalle.nombre, detalle.descripcion]
        .some((valor) => normalizarTextoBusqueda(valor ?? "").includes(filtro))
    );
  }, [query.data, textoBusqueda]);
  const totalPaginas = Math.max(Math.ceil(detallesFiltrados.length / size), 1);
  const paginaActual = Math.min(page, totalPaginas - 1);
  const detallesPaginados = useMemo(() => {
    const inicio = paginaActual * size;
    return detallesFiltrados.slice(inicio, inicio + size);
  }, [detallesFiltrados, paginaActual, size]);

  function resetForm() {
    setEditing(null);
    setForm({ nombre: "", codigo: "", descripcion: "" });
  }
  function startEditing(detalle: DetalleConservacionResponseDTO) {
    setEditing(detalle);
    setForm({ nombre: detalle.nombre, codigo: detalle.codigo, descripcion: detalle.descripcion ?? "" });
  }


  function submit(event: React.FormEvent) {
    event.preventDefault();
    const payload = {
      nombre: form.nombre.trim(),
      codigo: form.codigo?.trim() || null,
      descripcion: form.descripcion?.trim() || null
    };
    if (editing) {
      actualizar.mutate({ id: editing.id, payload }, { onSuccess: resetForm });
      return;
    }
    crear.mutate(payload, { onSuccess: resetForm });
  }

  function confirmarBaja(detalle: DetalleConservacionResponseDTO) {
    if (window.confirm(`Dar de baja el detalle ${detalle.nombre}?`)) baja.mutate(detalle.id);
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader description="Detalles seleccionables en el estado de conservación de objetos." title="Detalles de conservación" />
        {error ? <ErrorState message="No se pudo completar la operación." /> : null}
        <form className="space-y-4 rounded-lg border bg-white p-5" onSubmit={submit}>
          <div className="grid gap-4 md:grid-cols-2">
            <Field required label="Nombre"><input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" required maxLength={160} value={form.nombre} onChange={(event) => setForm((current) => ({ ...current, nombre: event.target.value }))} /></Field>
            <Field label="Código"><input className="h-10 w-full rounded-md border bg-background px-3 text-sm uppercase outline-none focus:ring-2 focus:ring-ring" maxLength={80} value={form.codigo ?? ""} onChange={(event) => setForm((current) => ({ ...current, codigo: event.target.value.toUpperCase() }))} /></Field>
          </div>
          <Field label="Descripción"><textarea className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" value={form.descripcion ?? ""} onChange={(event) => setForm((current) => ({ ...current, descripcion: event.target.value }))} /></Field>
          <div className="flex gap-2"><button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-60" disabled={isSubmitting} type="submit">{isSubmitting ? "Guardando..." : editing ? "Guardar cambios" : "Crear"}</button>{editing ? <button className="h-10 rounded-md border px-4 text-sm hover:bg-muted" onClick={resetForm} type="button">Cancelar</button> : null}</div>
        </form>
        {query.isLoading ? <LoadingState label="Cargando detalles..." /> : null}
        <div className="max-w-2xl">
          <label className="text-sm font-medium" htmlFor="buscar-detalles-conservacion">
            Buscar detalles de conservación
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20"
                id="buscar-detalles-conservacion"
                onChange={(event) => {
                  setTextoBusqueda(event.target.value);
                  setPage(0);
                }}
                placeholder="Buscar por código, nombre o descripción"
                type="search"
                value={textoBusqueda}
              />
            </div>
            {textoBusqueda ? (
              <button
                className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-4 text-sm font-medium hover:bg-muted"
                onClick={() => {
                  setTextoBusqueda("");
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
        <div className="overflow-hidden rounded-lg border bg-white">
          <table className="w-full border-collapse text-sm">
            <thead className="bg-primary text-primary-foreground"><tr><Th>Nombre</Th><Th>Código</Th><Th>Descripción</Th><Th align="right">Acciones</Th></tr></thead>
            <tbody>
              {detallesPaginados.map((detalle) => <tr className="border-t" key={detalle.id}><Td>{detalle.nombre}</Td><Td>{detalle.codigo}</Td><Td>{detalle.descripcion || "-"}</Td><Td align="right"><RowActions><RowActionButton icon={Pencil} label="Editar" onClick={() => startEditing(detalle)} /><RowActionButton icon={Trash2} label="Baja" onClick={() => confirmarBaja(detalle)} variant="destructive" /></RowActions></Td></tr>)}
              {!query.isLoading && !query.isError && detallesFiltrados.length === 0 ? (
                <tr className="border-t"><td className="px-4 py-3 text-center text-muted-foreground" colSpan={4}>No se encontraron detalles.</td></tr>
              ) : null}
            </tbody>
          </table>
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
          <div className="text-muted-foreground">{detallesFiltrados.length} elementos encontrados</div>
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
              disabled={paginaActual === 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              type="button"
            >
              Anterior
            </button>
            <span>Página {paginaActual + 1} de {totalPaginas}</span>
            <button
              className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
              disabled={paginaActual >= totalPaginas - 1}
              onClick={() => setPage((current) => current + 1)}
              type="button"
            >
              Siguiente
            </button>
          </div>
        </div>
      </div>
    </AppShell>
  );
}

function normalizarTextoBusqueda(texto: string) {
  return texto.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLocaleLowerCase("es").trim();
}

function Field({ children, label, required = false }: { children: React.ReactNode; label: string; required?: boolean }) {
  return <FormLabel label={label} required={required}>{children}</FormLabel>;
}

function Th({ align = "left", children }: { align?: "left" | "right"; children: React.ReactNode }) {
  return <th className={`px-4 py-3 font-semibold text-white ${align === "right" ? "text-right" : "text-left"}`}>{children}</th>;
}

function Td({ align = "left", children }: { align?: "left" | "right"; children: React.ReactNode }) {
  return <td className={`px-4 py-3 align-top ${align === "right" ? "text-right" : "text-left"}`}>{children}</td>;
}

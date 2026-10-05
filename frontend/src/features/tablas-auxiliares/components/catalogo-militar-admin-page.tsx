"use client";
import { FormLabel } from "@/components/common/form-label";

import { Pencil, Search, Trash2, X } from "lucide-react";
import { useMemo, useState } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { RowActionButton, RowActions } from "@/components/common/row-actions";
import { AppShell } from "@/components/layout/app-shell";
import { fuerzas, type Fuerza } from "@/features/veteranos/types";
import { routePermissions } from "@/lib/routes";
import type { RangoMilitarRequestDTO, RangoMilitarResponseDTO, UnidadMilitarRequestDTO, UnidadMilitarResponseDTO } from "../types";
import {
  useActualizarRangoMilitarMutation,
  useActualizarUnidadMilitarMutation,
  useBajaLogicaRangoMilitarMutation,
  useBajaLogicaUnidadMilitarMutation,
  useCrearRangoMilitarMutation,
  useCrearUnidadMilitarMutation,
  useRangosMilitaresAdminQuery,
  useUnidadesMilitaresAdminQuery
} from "../queries";

const fuerzaLabels: Record<Fuerza, string> = {
  EJERCITO: "Ejército",
  ARMADA: "Armada",
  FUERZA_AEREA: "Fuerza Aérea",
  PREFECTURA: "Prefectura",
  GENDARMERIA: "Gendarmería",
  CIVIL: "Civil"
};

type CatalogoMilitarAdminPageProps = {
  tipo: "rangos" | "unidades";
};

export function CatalogoMilitarAdminPage({ tipo }: CatalogoMilitarAdminPageProps) {
  return tipo === "rangos" ? <RangosPage /> : <UnidadesPage />;
}

function RangosPage() {
  const query = useRangosMilitaresAdminQuery();
  const crear = useCrearRangoMilitarMutation();
  const actualizar = useActualizarRangoMilitarMutation();
  const baja = useBajaLogicaRangoMilitarMutation();
  const [editing, setEditing] = useState<RangoMilitarResponseDTO | null>(null);
  const [form, setForm] = useState<RangoMilitarRequestDTO>({ fuerza: "EJERCITO", nombre: "", ordenJerarquico: 0 });
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [busqueda, setBusqueda] = useState("");
  const rangos = useMemo(() => query.data ?? [], [query.data]);
  const rangosFiltrados = useMemo(() => {
    const termino = normalizarTextoBusqueda(busqueda);
    if (!termino) return rangos;
    return rangos.filter((rango) =>
      [rango.nombre, fuerzaLabels[rango.fuerza]].some((campo) => normalizarTextoBusqueda(campo).includes(termino))
    );
  }, [busqueda, rangos]);
  const totalPaginas = Math.max(Math.ceil(rangosFiltrados.length / size), 1);
  const paginaActual = Math.min(page, totalPaginas - 1);
  const rangosPaginados = useMemo(() => {
    const inicio = paginaActual * size;
    return rangosFiltrados.slice(inicio, inicio + size);
  }, [paginaActual, rangosFiltrados, size]);

  const isSubmitting = crear.isPending || actualizar.isPending;
  const error = crear.error || actualizar.error || baja.error || query.error;

  function resetForm() {
    setEditing(null);
    setForm({ fuerza: "EJERCITO", nombre: "", ordenJerarquico: 0 });
  }
  function startEditing(rango: RangoMilitarResponseDTO) {
    setEditing(rango);
    setForm({ fuerza: rango.fuerza, nombre: rango.nombre, ordenJerarquico: rango.ordenJerarquico });
  }


  function submit(event: React.FormEvent) {
    event.preventDefault();
    const payload = { ...form, nombre: form.nombre.trim(), ordenJerarquico: Number(form.ordenJerarquico) };
    if (editing) {
      actualizar.mutate({ id: editing.id, payload }, { onSuccess: resetForm });
      return;
    }
    crear.mutate(payload, { onSuccess: resetForm });
  }

  function confirmarBaja(rango: RangoMilitarResponseDTO) {
    if (window.confirm(`Dar de baja el rango ${rango.nombre}?`)) {
      baja.mutate(rango.id, {
        onSuccess: () => {
          setPage((current) => Math.min(current, ultimaPaginaValida(rangosFiltrados.length - 1, size)));
        }
      });
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader description="Rangos disponibles para actuaciones de veteranos." title="Rangos militares" />
        {error ? <ErrorState message="No se pudo completar la operación." /> : null}
        <form className="grid gap-4 rounded-lg border bg-white p-5 md:grid-cols-[180px_1fr_160px_auto] md:items-end" onSubmit={submit}>
          <Field required label="Fuerza"><SelectFuerza value={form.fuerza} onChange={(fuerza) => setForm((current) => ({ ...current, fuerza }))} /></Field>
          <Field required label="Nombre"><input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" required maxLength={120} value={form.nombre} onChange={(event) => setForm((current) => ({ ...current, nombre: event.target.value }))} /></Field>
          <Field required label="Orden"><input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" min={0} required type="number" value={form.ordenJerarquico} onChange={(event) => setForm((current) => ({ ...current, ordenJerarquico: Number(event.target.value) }))} /></Field>
          <Actions isEditing={Boolean(editing)} isSubmitting={isSubmitting} onCancel={resetForm} />
        </form>
        {query.isLoading ? <LoadingState label="Cargando rangos..." /> : null}
        <BuscadorCatalogo
          id="buscar-rangos-militares"
          onChange={(value) => { setBusqueda(value); setPage(0); }}
          onClear={() => { setBusqueda(""); setPage(0); }}
          placeholder="Buscar por nombre o fuerza"
          value={busqueda}
        />
        <div className="overflow-hidden rounded-lg border bg-white">
          <table className="w-full border-collapse text-sm">
            <thead className="bg-primary text-primary-foreground"><tr><Th>Fuerza</Th><Th>Nombre</Th><Th>Orden</Th><Th align="right">Acciones</Th></tr></thead>
          <tbody>{rangosPaginados.map((rango) => <tr className="border-t" key={rango.id}><Td>{fuerzaLabels[rango.fuerza]}</Td><Td>{rango.nombre}</Td><Td>{rango.ordenJerarquico}</Td><Td align="right"><CatalogoRowActions onEdit={() => startEditing(rango)} onDelete={() => confirmarBaja(rango)} /></Td></tr>)}{rangosFiltrados.length === 0 ? <tr className="border-t"><Td colSpan={4}>No se encontraron rangos.</Td></tr> : null}</tbody>
          </table>
        </div>
        <PaginacionCatalogo
          onPageChange={setPage}
          onSizeChange={(nuevoTamano) => {
            setPage(0);
            setSize(nuevoTamano);
          }}
          page={paginaActual}
          size={size}
          totalElements={rangosFiltrados.length}
          totalPages={totalPaginas}
        />
      </div>
    </AppShell>
  );
}

function UnidadesPage() {
  const query = useUnidadesMilitaresAdminQuery();
  const crear = useCrearUnidadMilitarMutation();
  const actualizar = useActualizarUnidadMilitarMutation();
  const baja = useBajaLogicaUnidadMilitarMutation();
  const [editing, setEditing] = useState<UnidadMilitarResponseDTO | null>(null);
  const [form, setForm] = useState<UnidadMilitarRequestDTO>({ fuerza: "EJERCITO", nombre: "", sigla: "", tipoUnidad: "", descripcion: "" });
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [busqueda, setBusqueda] = useState("");
  const unidades = useMemo(() => query.data ?? [], [query.data]);
  const unidadesFiltradas = useMemo(() => {
    const termino = normalizarTextoBusqueda(busqueda);
    if (!termino) return unidades;
    return unidades.filter((unidad) =>
      [unidad.nombre, unidad.sigla, unidad.fuerza, fuerzaLabels[unidad.fuerza], unidad.tipoUnidad]
        .some((campo) => normalizarTextoBusqueda(campo ?? "").includes(termino))
    );
  }, [busqueda, unidades]);
  const totalPaginas = Math.max(Math.ceil(unidadesFiltradas.length / size), 1);
  const paginaActual = Math.min(page, totalPaginas - 1);
  const unidadesPaginadas = useMemo(() => {
    const inicio = paginaActual * size;
    return unidadesFiltradas.slice(inicio, inicio + size);
  }, [paginaActual, size, unidadesFiltradas]);

  const error = crear.error || actualizar.error || baja.error || query.error;
  const isSubmitting = crear.isPending || actualizar.isPending;

  function resetForm() {
    setEditing(null);
    setForm({ fuerza: "EJERCITO", nombre: "", sigla: "", tipoUnidad: "", descripcion: "" });
  }
  function startEditing(unidad: UnidadMilitarResponseDTO) {
    setEditing(unidad);
    setForm({ fuerza: unidad.fuerza, nombre: unidad.nombre, sigla: unidad.sigla ?? "", tipoUnidad: unidad.tipoUnidad ?? "", descripcion: unidad.descripcion ?? "" });
  }


  function submit(event: React.FormEvent) {
    event.preventDefault();
    const payload = {
      fuerza: form.fuerza,
      nombre: form.nombre.trim(),
      sigla: form.sigla?.trim() || null,
      tipoUnidad: form.tipoUnidad?.trim() || null,
      descripcion: form.descripcion?.trim() || null
    };
    if (editing) {
      actualizar.mutate({ id: editing.id, payload }, { onSuccess: resetForm });
      return;
    }
    crear.mutate(payload, { onSuccess: resetForm });
  }

  function confirmarBaja(unidad: UnidadMilitarResponseDTO) {
    if (window.confirm(`Dar de baja la unidad ${unidad.nombre}?`)) {
      baja.mutate(unidad.id, {
        onSuccess: () => {
          setPage((current) => Math.min(current, ultimaPaginaValida(unidadesFiltradas.length - 1, size)));
        }
      });
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader description="Unidades disponibles para actuaciones de veteranos." title="Unidades militares" />
        {error ? <ErrorState message="No se pudo completar la operación." /> : null}
        <form className="space-y-4 rounded-lg border bg-white p-5" onSubmit={submit}>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4"><Field required label="Fuerza"><SelectFuerza value={form.fuerza} onChange={(fuerza) => setForm((current) => ({ ...current, fuerza }))} /></Field><Field required label="Nombre"><input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" required maxLength={180} value={form.nombre} onChange={(event) => setForm((current) => ({ ...current, nombre: event.target.value }))} /></Field><Field label="Sigla"><input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" maxLength={40} value={form.sigla ?? ""} onChange={(event) => setForm((current) => ({ ...current, sigla: event.target.value }))} /></Field><Field label="Tipo"><input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" maxLength={80} value={form.tipoUnidad ?? ""} onChange={(event) => setForm((current) => ({ ...current, tipoUnidad: event.target.value }))} /></Field></div>
          <Field label="Descripción"><textarea className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" value={form.descripcion ?? ""} onChange={(event) => setForm((current) => ({ ...current, descripcion: event.target.value }))} /></Field>
          <Actions isEditing={Boolean(editing)} isSubmitting={isSubmitting} onCancel={resetForm} />
        </form>
        {query.isLoading ? <LoadingState label="Cargando unidades..." /> : null}
        <BuscadorCatalogo
          id="buscar-unidades-militares"
          onChange={(value) => { setBusqueda(value); setPage(0); }}
          onClear={() => { setBusqueda(""); setPage(0); }}
          placeholder="Buscar por nombre, sigla, fuerza o tipo de unidad"
          value={busqueda}
        />
        <div className="overflow-hidden rounded-lg border bg-white"><table className="w-full border-collapse text-sm"><thead className="bg-primary text-primary-foreground"><tr><Th>Fuerza</Th><Th>Nombre</Th><Th>Sigla</Th><Th>Tipo</Th><Th align="right">Acciones</Th></tr></thead><tbody>{unidadesPaginadas.map((unidad) => <tr className="border-t" key={unidad.id}><Td>{fuerzaLabels[unidad.fuerza]}</Td><Td>{unidad.nombre}</Td><Td>{unidad.sigla || "-"}</Td><Td>{unidad.tipoUnidad || "-"}</Td><Td align="right"><CatalogoRowActions onEdit={() => startEditing(unidad)} onDelete={() => confirmarBaja(unidad)} /></Td></tr>)}{unidadesFiltradas.length === 0 ? <tr className="border-t"><Td colSpan={5}>No se encontraron unidades.</Td></tr> : null}</tbody></table></div>
        <PaginacionCatalogo
          onPageChange={setPage}
          onSizeChange={(nuevoTamano) => {
            setPage(0);
            setSize(nuevoTamano);
          }}
          page={paginaActual}
          size={size}
          totalElements={unidadesFiltradas.length}
          totalPages={totalPaginas}
        />
      </div>
    </AppShell>
  );
}

function SelectFuerza({ onChange, value }: { onChange: (value: Fuerza) => void; value: Fuerza }) {
  return <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" value={value} onChange={(event) => onChange(event.target.value as Fuerza)}>{fuerzas.map((fuerza) => <option key={fuerza} value={fuerza}>{fuerzaLabels[fuerza]}</option>)}</select>;
}

function Field({ children, label, required = false }: { children: React.ReactNode; label: string; required?: boolean }) {
  return <FormLabel label={label} required={required}>{children}</FormLabel>;
}

function Actions({ isEditing, isSubmitting, onCancel }: { isEditing: boolean; isSubmitting: boolean; onCancel: () => void }) {
  return <div className="flex gap-2"><button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-60" disabled={isSubmitting} type="submit">{isSubmitting ? "Guardando..." : isEditing ? "Guardar cambios" : "Crear"}</button>{isEditing ? <button className="h-10 rounded-md border px-4 text-sm hover:bg-muted" onClick={onCancel} type="button">Cancelar</button> : null}</div>;
}

function CatalogoRowActions({ onDelete, onEdit }: { onDelete: () => void; onEdit: () => void }) {
  return <RowActions><RowActionButton icon={Pencil} label="Editar" onClick={onEdit} /><RowActionButton icon={Trash2} label="Baja" onClick={onDelete} variant="destructive" /></RowActions>;
}

function normalizarTextoBusqueda(texto: string) {
  return texto.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLocaleLowerCase("es").trim();
}

function BuscadorCatalogo({
  id,
  onChange,
  onClear,
  placeholder,
  value
}: {
  id: string;
  onChange: (value: string) => void;
  onClear: () => void;
  placeholder: string;
  value: string;
}) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <label className="sr-only" htmlFor={id}>Buscar en el catálogo</label>
      <div className="relative w-full max-w-xl">
        <Search aria-hidden="true" className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <input
          className="h-10 w-full rounded-md border bg-white pl-9 pr-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          id={id}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          type="search"
          value={value}
        />
      </div>
      {value ? (
        <button
          className="inline-flex h-10 items-center gap-2 rounded-md border px-3 text-sm hover:bg-muted"
          onClick={onClear}
          onMouseDown={(event) => event.preventDefault()}
          type="button"
        >
          <X aria-hidden="true" className="h-4 w-4" />
          Limpiar
        </button>
      ) : null}
    </div>
  );
}

function ultimaPaginaValida(totalElements: number, size: number) {
  return Math.max(Math.ceil(Math.max(totalElements, 0) / size) - 1, 0);
}

function PaginacionCatalogo({
  onPageChange,
  onSizeChange,
  page,
  size,
  totalElements,
  totalPages
}: {
  onPageChange: (page: number | ((current: number) => number)) => void;
  onSizeChange: (size: number) => void;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
      <div className="text-muted-foreground">{totalElements} elementos encontrados</div>
      <div className="flex items-center gap-2">
        <label className="flex items-center gap-2">
          <span>Cantidad de elementos por página</span>
          <select
            className="h-9 rounded-md border bg-white px-2"
            onChange={(event) => onSizeChange(Number(event.target.value))}
            value={size}
          >
            <option value={10}>10</option>
            <option value={20}>20</option>
            <option value={50}>50</option>
          </select>
        </label>
        <button
          className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
          disabled={page === 0}
          onClick={() => onPageChange((current) => Math.max(0, current - 1))}
          type="button"
        >
          Anterior
        </button>
        <span>Página {page + 1} de {totalPages}</span>
        <button
          className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange((current) => current + 1)}
          type="button"
        >
          Siguiente
        </button>
      </div>
    </div>
  );
}

function Th({ align = "left", children }: { align?: "left" | "right"; children: React.ReactNode }) {
  return <th className={`px-4 py-3 font-semibold text-white ${align === "right" ? "text-right" : "text-left"}`}>{children}</th>;
}

function Td({ align = "left", children, colSpan }: { align?: "left" | "right"; children?: React.ReactNode; colSpan?: number }) {
  return <td className={`px-4 py-3 align-top ${align === "right" ? "text-right" : "text-left"}`} colSpan={colSpan}>{children}</td>;
}

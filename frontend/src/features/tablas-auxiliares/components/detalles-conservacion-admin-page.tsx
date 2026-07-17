"use client";

import { Pencil, Trash2 } from "lucide-react";
import { useState } from "react";
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
  const error = crear.error || actualizar.error || baja.error || query.error;
  const isSubmitting = crear.isPending || actualizar.isPending;

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
            <Field label="Nombre"><input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" required maxLength={160} value={form.nombre} onChange={(event) => setForm((current) => ({ ...current, nombre: event.target.value }))} /></Field>
            <Field label="Código"><input className="h-10 w-full rounded-md border bg-background px-3 text-sm uppercase outline-none focus:ring-2 focus:ring-ring" maxLength={80} value={form.codigo ?? ""} onChange={(event) => setForm((current) => ({ ...current, codigo: event.target.value.toUpperCase() }))} /></Field>
          </div>
          <Field label="Descripción"><textarea className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" value={form.descripcion ?? ""} onChange={(event) => setForm((current) => ({ ...current, descripcion: event.target.value }))} /></Field>
          <div className="flex gap-2"><button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-60" disabled={isSubmitting} type="submit">{isSubmitting ? "Guardando..." : editing ? "Guardar cambios" : "Crear"}</button>{editing ? <button className="h-10 rounded-md border px-4 text-sm hover:bg-muted" onClick={resetForm} type="button">Cancelar</button> : null}</div>
        </form>
        {query.isLoading ? <LoadingState label="Cargando detalles..." /> : null}
        <div className="overflow-hidden rounded-lg border bg-white">
          <table className="w-full border-collapse text-sm">
            <thead className="bg-primary text-primary-foreground"><tr><Th>Nombre</Th><Th>Código</Th><Th>Descripción</Th><Th align="right">Acciones</Th></tr></thead>
            <tbody>{(query.data ?? []).map((detalle) => <tr className="border-t" key={detalle.id}><Td>{detalle.nombre}</Td><Td>{detalle.codigo}</Td><Td>{detalle.descripcion || "-"}</Td><Td align="right"><RowActions><RowActionButton icon={Pencil} label="Editar" onClick={() => startEditing(detalle)} /><RowActionButton icon={Trash2} label="Baja" onClick={() => confirmarBaja(detalle)} variant="destructive" /></RowActions></Td></tr>)}</tbody>
          </table>
        </div>
      </div>
    </AppShell>
  );
}

function Field({ children, label }: { children: React.ReactNode; label: string }) {
  return <label className="space-y-2 text-sm font-medium"><span>{label}</span>{children}</label>;
}

function Th({ align = "left", children }: { align?: "left" | "right"; children: React.ReactNode }) {
  return <th className={`px-4 py-3 font-semibold text-white ${align === "right" ? "text-right" : "text-left"}`}>{children}</th>;
}

function Td({ align = "left", children }: { align?: "left" | "right"; children: React.ReactNode }) {
  return <td className={`px-4 py-3 align-top ${align === "right" ? "text-right" : "text-left"}`}>{children}</td>;
}

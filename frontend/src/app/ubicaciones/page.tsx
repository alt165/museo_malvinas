"use client";

import { Pencil, Trash2 } from "lucide-react";
import { useState, type FormEvent, type ReactNode } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { RowActionButton, RowActionLink, RowActions } from "@/components/common/row-actions";
import { AppShell } from "@/components/layout/app-shell";
import { useBajaLogicaUbicacionMutation, useCrearUbicacionMutation, useUbicacionesQuery } from "@/features/ubicaciones/queries";
import type { UbicacionRequestDTO } from "@/features/ubicaciones/types";
import { getApiErrorMessage } from "@/features/ubicaciones/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

const ubicacionInicial: UbicacionRequestDTO = { nombre: "", descripcion: "" };

export default function UbicacionesPage() {
  const { canAdminEdit: esAdmin } = useEditingMode();
  const { data = [], error, isError, isLoading } = useUbicacionesQuery();
  const crear = useCrearUbicacionMutation();
  const baja = useBajaLogicaUbicacionMutation();
  const [form, setForm] = useState<UbicacionRequestDTO>(ubicacionInicial);
  const isSubmitting = crear.isPending;

  function resetForm() {
    setForm(ubicacionInicial);
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();

    crear.mutate(
      {
        nombre: form.nombre.trim(),
        descripcion: form.descripcion?.trim() || null
      },
      { onSuccess: resetForm }
    );
  }

  function handleBaja(id: number, nombre: string) {
    if (window.confirm(`Dar de baja la ubicacion ${nombre}?`)) {
      baja.mutate(id);
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader
          description="Ubicaciones configurables para registrar donde se encuentran los objetos."
          title="Ubicaciones"
        />
        {esAdmin ? (
          <form className="space-y-4 rounded-lg border bg-white p-5" onSubmit={handleSubmit}>
            <Field label="Nombre">
              <input
                className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
                required
                value={form.nombre}
                onChange={(event) => setForm((current) => ({ ...current, nombre: event.target.value }))}
              />
            </Field>
            <Field label="Descripcion">
              <textarea
                className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                value={form.descripcion ?? ""}
                onChange={(event) => setForm((current) => ({ ...current, descripcion: event.target.value }))}
              />
            </Field>
            <div className="flex gap-2">
              <button
                className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-60"
                disabled={isSubmitting}
                type="submit"
              >
                {isSubmitting ? "Guardando..." : "Crear"}
              </button>
            </div>
          </form>
        ) : null}
        {isLoading ? <LoadingState label="Cargando ubicaciones..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {crear.isError ? <ErrorState message={getApiErrorMessage(crear.error)} requestId={crear.error instanceof ApiClientError ? crear.error.requestId : undefined} /> : null}
        {!isLoading && !isError && data.length === 0 ? (
          <EmptyState
            description="Todavia no hay ubicaciones activas."
            title="Sin ubicaciones"
          />
        ) : null}
        {!isLoading && !isError && data.length > 0 ? (
          <div className="overflow-hidden rounded-lg border">
            <table className="w-full border-collapse text-sm">
              <thead className="bg-primary text-primary-foreground">
                <tr>
                  <th className="px-4 py-3 text-left font-semibold text-white">Nombre</th>
                  <th className="px-4 py-3 text-left font-semibold text-white">Descripcion</th>
                  {esAdmin ? <th className="px-4 py-3 text-right font-semibold text-white">Acciones</th> : null}
                </tr>
              </thead>
              <tbody>
                {data.map((ubicacion) => (
                  <tr className="border-t" key={ubicacion.id}>
                    <td className="px-4 py-3 align-top font-medium">{ubicacion.nombre}</td>
                    <td className="px-4 py-3 align-top text-muted-foreground">{ubicacion.descripcion || "Sin descripcion"}</td>
                    {esAdmin ? (
                      <td className="px-4 py-3 align-top">
                        <RowActions>
                          <RowActionLink href={`/ubicaciones/${ubicacion.id}/editar`} icon={Pencil} label="Editar" />
                          <RowActionButton disabled={baja.variables === ubicacion.id} icon={Trash2} label="Baja" onClick={() => handleBaja(ubicacion.id, ubicacion.nombre)} variant="destructive" />
                        </RowActions>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

function Field({ children, label }: { children: ReactNode; label: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      {children}
    </label>
  );
}

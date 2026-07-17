"use client";

import { useState, type FormEvent, type ReactNode } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { CategoriasTable } from "@/features/categorias/components/categorias-table";
import { useBajaLogicaCategoriaMutation, useCategoriasQuery, useCrearCategoriaMutation } from "@/features/categorias/queries";
import type { CategoriaObjetoRequestDTO } from "@/features/categorias/types";
import { getApiErrorMessage } from "@/features/categorias/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

const categoriaInicial: CategoriaObjetoRequestDTO = { nombre: "", descripcion: "" };

export default function CategoriasPage() {
  const { canAdminEdit: puedeEscribir } = useEditingMode();
  const categoriasQuery = useCategoriasQuery();
  const crearMutation = useCrearCategoriaMutation();
  const bajaMutation = useBajaLogicaCategoriaMutation();
  const [form, setForm] = useState<CategoriaObjetoRequestDTO>(categoriaInicial);
  const isSubmitting = crearMutation.isPending;

  function resetForm() {
    setForm(categoriaInicial);
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();

    crearMutation.mutate(
      {
        nombre: form.nombre.trim(),
        descripcion: form.descripcion?.trim() || null
      },
      { onSuccess: resetForm }
    );
  }

  function handleDelete(id: number) {
    if (window.confirm("Dar de baja esta categoria?")) {
      bajaMutation.mutate(id);
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader description="Clasificacion de objetos del museo." title="Categorias" />
        {puedeEscribir ? (
          <form className="space-y-4 rounded-lg border bg-white p-5" onSubmit={handleSubmit}>
            <Field label="Nombre">
              <input
                className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
                maxLength={100}
                required
                value={form.nombre}
                onChange={(event) => setForm((current) => ({ ...current, nombre: event.target.value }))}
              />
            </Field>
            <Field label="Descripcion">
              <textarea
                className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                maxLength={500}
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
        {categoriasQuery.isLoading ? <LoadingState label="Cargando categorias..." /> : null}
        {categoriasQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(categoriasQuery.error)}
            requestId={categoriasQuery.error instanceof ApiClientError ? categoriasQuery.error.requestId : undefined}
          />
        ) : null}
        {crearMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(crearMutation.error)}
            requestId={crearMutation.error instanceof ApiClientError ? crearMutation.error.requestId : undefined}
          />
        ) : null}
        {bajaMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(bajaMutation.error)}
            requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined}
          />
        ) : null}
        {categoriasQuery.data?.length === 0 ? <EmptyState title="Sin categorias" description="Todavia no hay categorias registradas." /> : null}
        {categoriasQuery.data && categoriasQuery.data.length > 0 ? (
          <CategoriasTable
            canEdit={puedeEscribir}
            categorias={categoriasQuery.data}
            isDeleting={bajaMutation.isPending}
            onDelete={handleDelete}
          />
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

"use client";

import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useBajaLogicaUbicacionMutation, useUbicacionesQuery } from "@/features/ubicaciones/queries";
import { getApiErrorMessage } from "@/features/ubicaciones/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function UbicacionesPage() {
  const { canAdminEdit: esAdmin } = useEditingMode();
  const { data = [], error, isError, isLoading } = useUbicacionesQuery();
  const baja = useBajaLogicaUbicacionMutation();

  function handleBaja(id: number, nombre: string) {
    if (window.confirm(`Dar de baja la ubicacion ${nombre}?`)) {
      baja.mutate(id);
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader
          actions={esAdmin ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/ubicaciones/nueva">Nueva ubicacion</Link> : null}
          description="Ubicaciones configurables para registrar donde se encuentran los objetos."
          title="Ubicaciones"
        />
        {isLoading ? <LoadingState label="Cargando ubicaciones..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {!isLoading && !isError && data.length === 0 ? (
          <EmptyState
            action={esAdmin ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/ubicaciones/nueva">Nueva ubicacion</Link> : null}
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
                        <div className="flex justify-end gap-2">
                          <Link className="rounded-md border px-3 py-1.5 text-xs hover:bg-muted" href={`/ubicaciones/${ubicacion.id}/editar`}>Editar</Link>
                          <button className="rounded-md border border-red-200 px-3 py-1.5 text-xs text-red-700 hover:bg-red-50 disabled:opacity-60" disabled={baja.variables === ubicacion.id} onClick={() => handleBaja(ubicacion.id, ubicacion.nombre)} type="button">Baja</button>
                        </div>
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

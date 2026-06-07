"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ObjetosColeccionPanel } from "@/features/colecciones/components/objetos-coleccion-panel";
import { useColeccionQuery } from "@/features/colecciones/queries";
import { getApiErrorMessage, resumenDescripcion } from "@/features/colecciones/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { routes } from "@/lib/routes";

export default function ColeccionDetallePage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const coleccionQuery = useColeccionQuery(id);

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            <div className="flex flex-wrap gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={routes.objetosColecciones}>
                Volver
              </Link>
              {puedeEscribir ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/objetos/colecciones/${id}/editar`}>
                  Editar
                </Link>
              ) : null}
            </div>
          }
          description="Detalle y objetos asociados a la coleccion."
          title={coleccionQuery.data?.nombre ?? "Coleccion"}
        />
        {coleccionQuery.isLoading ? <LoadingState label="Cargando coleccion..." /> : null}
        {coleccionQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(coleccionQuery.error)}
            requestId={coleccionQuery.error instanceof ApiClientError ? coleccionQuery.error.requestId : undefined}
          />
        ) : null}
        {coleccionQuery.data ? (
          <>
            <section className="grid gap-4 rounded-lg border bg-surface p-5 shadow-sm sm:grid-cols-3">
              <Info label="Nombre" value={coleccionQuery.data.nombre} />
              <Info label="Objetos asociados" value={String(coleccionQuery.data.cantidadObjetos ?? 0)} />
              <Info className="sm:col-span-3" label="Descripcion" value={resumenDescripcion(coleccionQuery.data.descripcion)} />
            </section>
            <ObjetosColeccionPanel coleccionId={id} />
          </>
        ) : null}
      </div>
    </AppShell>
  );
}

function Info({ className, label, value }: { className?: string; label: string; value: string }) {
  return (
    <div className={className}>
      <p className="text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm font-medium text-primary">{value}</p>
    </div>
  );
}

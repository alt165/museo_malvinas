"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ObjetosExhibicionPanel } from "@/features/exhibiciones/components/objetos-exhibicion-panel";
import { useCancelarExhibicionMutation, useExhibicionQuery, useFinalizarExhibicionMutation } from "@/features/exhibiciones/queries";
import { formatDate, getApiErrorMessage } from "@/features/exhibiciones/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function DetalleExhibicionPage() {
  const params = useParams<{ id: string }>();
  const id = getParamId(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const { data, error, isError, isLoading } = useExhibicionQuery(id);
  const finalizarMutation = useFinalizarExhibicionMutation(id);
  const cancelarMutation = useCancelarExhibicionMutation(id);
  const hoy = new Date().toISOString().slice(0, 10);

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            <div className="flex gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/exhibiciones">
                Volver
              </Link>
              {puedeEscribir && data ? (
                <>
                  <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/exhibiciones/${data.id}/editar`}>
                    Editar
                  </Link>
                  {data.estado === "PLANIFICADA" && data.fechaInicio > hoy ? (
                    <button
                      className="rounded-md border px-4 py-2 text-sm font-medium text-destructive hover:bg-muted disabled:opacity-60"
                      disabled={cancelarMutation.isPending}
                      onClick={() => {
                        if (window.confirm("¿Confirma que desea cancelar esta exhibición? Los objetos asociados quedarán disponibles.")) {
                          cancelarMutation.mutate();
                        }
                      }}
                      type="button"
                    >
                      {cancelarMutation.isPending ? "Cancelando..." : "Cancelar exhibición"}
                    </button>
                  ) : null}
                  {data.estado === "FINALIZADA" ? (
                    <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/exhibiciones/repetir/${data.id}`}>
                      Repetir exhibición
                    </Link>
                  ) : data.estado !== "CANCELADA" ? (
                    <button
                      className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-60"
                      disabled={finalizarMutation.isPending}
                      onClick={() => {
                        if (window.confirm("Confirmar finalización de la exhibición")) {
                          finalizarMutation.mutate();
                        }
                      }}
                      type="button"
                    >
                      {finalizarMutation.isPending ? "Finalizando..." : "Finalizar"}
                    </button>
                  ) : null}
                </>
              ) : null}
            </div>
          }
          description="Detalle de exhibición y objetos asociados."
          title="Detalle de exhibición"
        />
        {isLoading ? <LoadingState label="Cargando exhibición..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {finalizarMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(finalizarMutation.error)}
            requestId={finalizarMutation.error instanceof ApiClientError ? finalizarMutation.error.requestId : undefined}
          />
        ) : null}
        {cancelarMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(cancelarMutation.error)}
            requestId={cancelarMutation.error instanceof ApiClientError ? cancelarMutation.error.requestId : undefined}
          />
        ) : null}
        {data ? (
          <>
            <div className="rounded-lg border p-5">
              <dl className="grid gap-5 text-sm sm:grid-cols-2">
                <div>
                  <dt className="text-muted-foreground">Nombre</dt>
                  <dd className="mt-1 font-medium">{data.nombre}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Estado</dt>
                  <dd className="mt-1 font-medium">{data.estado}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Tipo</dt>
                  <dd className="mt-1 font-medium">{data.tipo}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Periodo</dt>
                  <dd className="mt-1 font-medium">
                    {formatDate(data.fechaInicio)} - {formatDate(data.fechaFin)}
                  </dd>
                </div>
                <div className="sm:col-span-2">
                  <dt className="text-muted-foreground">Descripción</dt>
                  <dd className="mt-1 whitespace-pre-wrap font-medium">{data.descripcion || "Sin descripción"}</dd>
                </div>
              </dl>
            </div>
            <ObjetosExhibicionPanel canWrite={puedeEscribir} estado={data.estado} exhibicionId={data.id} />
          </>
        ) : null}
      </div>
    </AppShell>
  );
}

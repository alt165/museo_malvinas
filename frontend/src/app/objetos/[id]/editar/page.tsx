"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ObjetoArchivosPanel } from "@/features/objetos/components/objeto-archivos-panel";
import { ObjetoMuseoForm } from "@/features/objetos/components/objeto-museo-form";
import { useActualizarObjetoMutation, useObjetoQuery } from "@/features/objetos/queries";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function EditarObjetoPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { data, error, isError, isLoading } = useObjetoQuery(id);
  const mutation = useActualizarObjetoMutation(id);
  const [objetoCompletado, setObjetoCompletado] = useState<string | null>(null);

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          description="Edicion de objeto patrimonial."
          title="Editar objeto"
        />
        {isLoading ? <LoadingState label="Cargando objeto..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        {objetoCompletado ? (
          <div className="rounded-lg border border-green-200 bg-green-50 p-5 text-sm text-green-900">
            <p className="font-medium">Objeto cargado correctamente: {objetoCompletado}</p>
            <p className="mt-1">La ficha fue completada y los campos de carga ya no quedan en pantalla.</p>
            <div className="mt-4 flex flex-wrap gap-2">
              <Link className="rounded-md border bg-white px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos/pendientes">
                Volver a pendientes
              </Link>
              <Link className="rounded-md border bg-white px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos/nuevo">
                Nueva alta completa
              </Link>
            </div>
          </div>
        ) : null}
        {data && !objetoCompletado ? (
          <>
            <ObjetoMuseoForm
              initialValue={data}
              isSubmitting={mutation.isPending}
              onSubmit={(payload) =>
                mutation.mutate(payload, {
                  onSuccess: (objeto) => {
                    if (data.origenCarga === "RAPIDA" && data.datosCompletos === false) {
                      setObjetoCompletado(objeto.numeroInventario);
                      return;
                    }
                    router.push(`/objetos/${objeto.id}`);
                  }
                })
              }
              submitError={mutation.error}
              submitLabel="Guardar cambios"
            />
            <ObjetoArchivosPanel mode="edit" objeto={data} />
          </>
        ) : null}
      </div>
    </AppShell>
  );
}

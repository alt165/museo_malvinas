"use client";

import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ColeccionForm } from "@/features/colecciones/components/coleccion-form";
import { useActualizarColeccionMutation, useColeccionQuery, useObjetosColeccionQuery } from "@/features/colecciones/queries";
import { getApiErrorMessage } from "@/features/colecciones/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function EditarColeccionPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const router = useRouter();
  const coleccionQuery = useColeccionQuery(id);
  const objetosColeccionQuery = useObjetosColeccionQuery(id);
  const mutation = useActualizarColeccionMutation(id);

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Edicion de coleccion de objetos." title="Editar coleccion" />
        {coleccionQuery.isLoading || objetosColeccionQuery.isLoading ? <LoadingState label="Cargando coleccion..." /> : null}
        {coleccionQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(coleccionQuery.error)}
            requestId={coleccionQuery.error instanceof ApiClientError ? coleccionQuery.error.requestId : undefined}
          />
        ) : null}
        {objetosColeccionQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(objetosColeccionQuery.error)}
            requestId={objetosColeccionQuery.error instanceof ApiClientError ? objetosColeccionQuery.error.requestId : undefined}
          />
        ) : null}
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        {coleccionQuery.data && objetosColeccionQuery.data ? (
          <ColeccionForm
            initialObjetos={objetosColeccionQuery.data}
            initialValue={coleccionQuery.data}
            isSubmitting={mutation.isPending}
            onSubmit={(payload) => mutation.mutate(payload, { onSuccess: () => router.push(`/objetos/colecciones/${id}`) })}
            submitError={mutation.error}
            submitLabel="Guardar cambios"
          />
        ) : null}
      </div>
    </AppShell>
  );
}

"use client";

import { useParams, useRouter } from "next/navigation";
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
        {data ? (
          <>
            <ObjetoMuseoForm
              initialValue={data}
              isSubmitting={mutation.isPending}
              onSubmit={(payload) =>
                mutation.mutate(payload, {
                  onSuccess: (objeto) => router.push(`/objetos/${objeto.id}`)
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

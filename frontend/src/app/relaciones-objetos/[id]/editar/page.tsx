"use client";

import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { RelacionObjetoForm } from "@/features/relaciones-objetos/components/relacion-objeto-form";
import { useActualizarRelacionObjetoMutation, useRelacionObjetoQuery } from "@/features/relaciones-objetos/queries";
import { getApiErrorMessage } from "@/features/relaciones-objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function EditarRelacionObjetoPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { data, error, isError, isLoading } = useRelacionObjetoQuery(id);
  const mutation = useActualizarRelacionObjetoMutation(id);

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Actualizar relacion entre objetos." title="Editar relacion" />
        {isLoading ? <LoadingState label="Cargando relacion..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {mutation.isError ? <ErrorState message={getApiErrorMessage(mutation.error)} requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined} /> : null}
        {data ? (
          <RelacionObjetoForm
            initialValue={data}
            isSubmitting={mutation.isPending}
            onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (relacion) => router.push(`/relaciones-objetos/${relacion.id}`) })}
            submitError={mutation.error}
            submitLabel="Guardar cambios"
          />
        ) : null}
      </div>
    </AppShell>
  );
}

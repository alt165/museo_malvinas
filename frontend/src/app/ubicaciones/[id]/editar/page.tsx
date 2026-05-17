"use client";

import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { UbicacionForm } from "@/features/ubicaciones/components/ubicacion-form";
import { useActualizarUbicacionMutation, useUbicacionQuery } from "@/features/ubicaciones/queries";
import { getApiErrorMessage } from "@/features/ubicaciones/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { permissions } from "@/lib/auth";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function EditarUbicacionPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { data, error, isError, isLoading } = useUbicacionQuery(id);
  const mutation = useActualizarUbicacionMutation(id);

  return (
    <AppShell requiredRoles={[...permissions.adminRoles]}>
      <div className="space-y-6">
        <PageHeader description="Actualizar datos visibles de la ubicacion." title="Editar ubicacion" />
        {isLoading ? <LoadingState label="Cargando ubicacion..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {mutation.isError ? <ErrorState message={getApiErrorMessage(mutation.error)} requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined} /> : null}
        {data ? (
          <UbicacionForm
            initialValue={data}
            isSubmitting={mutation.isPending}
            onSubmit={(payload) => mutation.mutate(payload, { onSuccess: () => router.push("/ubicaciones") })}
            submitLabel="Guardar cambios"
          />
        ) : null}
      </div>
    </AppShell>
  );
}

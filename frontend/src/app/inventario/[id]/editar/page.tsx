"use client";

import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { InventarioForm } from "@/features/inventario/components/inventario-form";
import { useActualizarInventarioMutation, useInventarioQuery } from "@/features/inventario/queries";
import { getApiErrorMessage } from "@/features/inventario/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function EditarInventarioPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { data, error, isError, isLoading } = useInventarioQuery(id);
  const mutation = useActualizarInventarioMutation(id);

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          description="Actualizar ubicacion, estado y conservacion del objeto."
          title="Editar inventario"
        />
        {isLoading ? <LoadingState label="Cargando inventario..." /> : null}
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
          <InventarioForm
            initialValue={data}
            isSubmitting={mutation.isPending}
            onSubmit={(payload) =>
              mutation.mutate(payload, {
                onSuccess: (inventario) => router.push(`/inventario/${inventario.id}`)
              })
            }
            submitError={mutation.error}
            submitLabel="Guardar cambios"
          />
        ) : null}
      </div>
    </AppShell>
  );
}

"use client";

import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ExhibicionForm } from "@/features/exhibiciones/components/exhibicion-form";
import { useActualizarExhibicionMutation, useExhibicionQuery } from "@/features/exhibiciones/queries";
import { getApiErrorMessage } from "@/features/exhibiciones/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function EditarExhibicionPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { data, error, isError, isLoading } = useExhibicionQuery(id);
  const mutation = useActualizarExhibicionMutation(id);

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Edición de exhibición." title="Editar exhibición" />
        {isLoading ? <LoadingState label="Cargando exhibición..." /> : null}
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
          <ExhibicionForm
            initialValue={data}
            isSubmitting={mutation.isPending}
            onSubmit={(payload) =>
              mutation.mutate(payload, {
                onSuccess: (exhibicion) => router.push(`/exhibiciones/${exhibicion.id}`)
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

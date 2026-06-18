"use client";

import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { CategoriaForm } from "@/features/categorias/components/categoria-form";
import { useActualizarCategoriaMutation, useCategoriaQuery } from "@/features/categorias/queries";
import { getApiErrorMessage } from "@/features/categorias/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function EditarCategoriaPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { data, error, isError, isLoading } = useCategoriaQuery(id);
  const mutation = useActualizarCategoriaMutation(id);

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Actualizar categoria de objetos." title="Editar categoria" />
        {isLoading ? <LoadingState label="Cargando categoria..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {mutation.isError ? <ErrorState message={getApiErrorMessage(mutation.error)} requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined} /> : null}
        {data ? (
          <CategoriaForm
            initialValue={data}
            isSubmitting={mutation.isPending}
            onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (categoria) => router.push(`/categorias/${categoria.id}`) })}
            submitError={mutation.error}
            submitLabel="Guardar cambios"
          />
        ) : null}
      </div>
    </AppShell>
  );
}

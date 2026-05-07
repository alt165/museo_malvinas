"use client";

import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { VeteranoForm } from "@/features/veteranos/components/veterano-form";
import { useActualizarVeteranoMutation, useVeteranoQuery } from "@/features/veteranos/queries";
import { getApiErrorMessage } from "@/features/veteranos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function EditarVeteranoPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { data, error, isError, isLoading } = useVeteranoQuery(id);
  const mutation = useActualizarVeteranoMutation(id);
  return <AppShell requiredRoles={[...routePermissions.write]}><div className="space-y-6"><PageHeader title="Editar veterano" description="Actualizar datos del veterano." />{isLoading ? <LoadingState /> : null}{isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}{mutation.isError ? <ErrorState message={getApiErrorMessage(mutation.error)} requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined} /> : null}{data ? <VeteranoForm initialValue={data} isSubmitting={mutation.isPending} submitError={mutation.error} submitLabel="Guardar cambios" onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (v) => router.push(`/veteranos/${v.id}`) })} /> : null}</div></AppShell>;
}

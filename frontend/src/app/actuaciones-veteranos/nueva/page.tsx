"use client";

import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ActuacionVeteranoForm } from "@/features/veteranos/components/actuacion-veterano-form";
import { useCrearActuacionVeteranoGlobalMutation } from "@/features/veteranos/queries";
import { getApiErrorMessage } from "@/features/veteranos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevaActuacionVeteranoPage() {
  const router = useRouter();
  const mutation = useCrearActuacionVeteranoGlobalMutation();

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Registrar actuacion historica de un veterano." title="Nueva actuacion" />
        {mutation.isError ? <ErrorState message={getApiErrorMessage(mutation.error)} requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined} /> : null}
        <ActuacionVeteranoForm
          isSubmitting={mutation.isPending}
          onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (actuacion) => router.push(`/actuaciones-veteranos/${actuacion.id}`) })}
          submitError={mutation.error}
          submitLabel="Crear actuacion"
        />
      </div>
    </AppShell>
  );
}

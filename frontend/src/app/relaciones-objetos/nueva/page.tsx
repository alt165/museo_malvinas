"use client";

import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { RelacionObjetoForm } from "@/features/relaciones-objetos/components/relacion-objeto-form";
import { useCrearRelacionObjetoMutation } from "@/features/relaciones-objetos/queries";
import { getApiErrorMessage } from "@/features/relaciones-objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevaRelacionObjetoPage() {
  const router = useRouter();
  const mutation = useCrearRelacionObjetoMutation();

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Crear vinculo historico o documental entre objetos." title="Nueva relacion entre objetos" />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        <RelacionObjetoForm
          isSubmitting={mutation.isPending}
          onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (relacion) => router.push(`/relaciones-objetos/${relacion.id}`) })}
          submitError={mutation.error}
          submitLabel="Crear relacion"
        />
      </div>
    </AppShell>
  );
}

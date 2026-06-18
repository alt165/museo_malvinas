"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { RelacionObjetoForm } from "@/features/relaciones-objetos/components/relacion-objeto-form";
import { useCrearRelacionObjetoMutation } from "@/features/relaciones-objetos/queries";
import { getApiErrorMessage } from "@/features/relaciones-objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevaRelacionObjetoPage() {
  return (
    <Suspense fallback={<LoadingState label="Cargando formulario..." />}>
      <NuevaRelacionObjetoContent />
    </Suspense>
  );
}

function NuevaRelacionObjetoContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const objetoOrigenId = Number(searchParams.get("origenId") ?? searchParams.get("objetoOrigenId"));
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
          defaultObjetoOrigenId={Number.isFinite(objetoOrigenId) && objetoOrigenId > 0 ? objetoOrigenId : undefined}
          isSubmitting={mutation.isPending}
          onSubmit={(payload) =>
            mutation.mutate(payload, { onSuccess: () => router.push(`/objetos/${payload.objetoOrigenId}/relaciones?view=graph`) })
          }
          submitError={mutation.error}
          submitLabel="Crear relacion"
        />
      </div>
    </AppShell>
  );
}

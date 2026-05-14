"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { DepositanteForm } from "@/features/depositantes/components/depositante-form";
import { useCrearDepositanteMutation } from "@/features/depositantes/queries";
import { getApiErrorMessage } from "@/features/depositantes/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevoDepositantePage() {
  return (
    <Suspense>
      <NuevoDepositanteContent />
    </Suspense>
  );
}

function NuevoDepositanteContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const mutation = useCrearDepositanteMutation();
  const identificacion = searchParams.get("identificacion") ?? undefined;

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Alta de depositante." title="Nuevo depositante" />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        <DepositanteForm
          initialIdentification={identificacion}
          isSubmitting={mutation.isPending}
          onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (depositante) => router.push(`/depositantes/${depositante.id}`) })}
          submitError={mutation.error}
          submitLabel="Crear depositante"
        />
      </div>
    </AppShell>
  );
}

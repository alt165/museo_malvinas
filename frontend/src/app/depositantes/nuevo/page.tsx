"use client";

import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { DepositanteForm } from "@/features/depositantes/components/depositante-form";
import { useCrearDepositanteMutation } from "@/features/depositantes/queries";
import { getApiErrorMessage } from "@/features/depositantes/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevoDepositantePage() {
  const router = useRouter();
  const mutation = useCrearDepositanteMutation();

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
          isSubmitting={mutation.isPending}
          onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (depositante) => router.push(`/depositantes/${depositante.id}`) })}
          submitError={mutation.error}
          submitLabel="Crear depositante"
        />
      </div>
    </AppShell>
  );
}

"use client";

import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { InventarioForm } from "@/features/inventario/components/inventario-form";
import { useCrearInventarioMutation } from "@/features/inventario/queries";
import { getApiErrorMessage } from "@/features/inventario/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevoInventarioPage() {
  const router = useRouter();
  const mutation = useCrearInventarioMutation();

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          description="Crear registro de inventario para un objeto del museo."
          title="Nuevo inventario"
        />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        <InventarioForm
          isSubmitting={mutation.isPending}
          onSubmit={(payload) =>
            mutation.mutate(payload, {
              onSuccess: (inventario) => router.push(`/inventario/${inventario.id}`)
            })
          }
          submitError={mutation.error}
          submitLabel="Crear inventario"
        />
      </div>
    </AppShell>
  );
}

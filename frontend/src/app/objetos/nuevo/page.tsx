"use client";

import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ObjetoMuseoForm } from "@/features/objetos/components/objeto-museo-form";
import { useCrearObjetoMutation } from "@/features/objetos/queries";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevoObjetoPage() {
  const router = useRouter();
  const mutation = useCrearObjetoMutation();

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          description="Alta de un objeto patrimonial."
          title="Nuevo objeto"
        />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        <ObjetoMuseoForm
          isSubmitting={mutation.isPending}
          onSubmit={(payload) =>
            mutation.mutate(payload, {
              onSuccess: (objeto) => router.push(`/objetos/${objeto.id}`)
            })
          }
          submitError={mutation.error}
          submitLabel="Crear objeto"
        />
      </div>
    </AppShell>
  );
}

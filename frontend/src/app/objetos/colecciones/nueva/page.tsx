"use client";

import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ColeccionForm } from "@/features/colecciones/components/coleccion-form";
import { useCrearColeccionMutation } from "@/features/colecciones/queries";
import { getApiErrorMessage } from "@/features/colecciones/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevaColeccionPage() {
  const router = useRouter();
  const mutation = useCrearColeccionMutation();

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Alta de coleccion de objetos." title="Nueva coleccion" />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        <ColeccionForm
          isSubmitting={mutation.isPending}
          onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (coleccion) => router.push(`/objetos/colecciones/${coleccion.id}`) })}
          submitError={mutation.error}
          submitLabel="Crear coleccion"
        />
      </div>
    </AppShell>
  );
}

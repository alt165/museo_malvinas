"use client";

import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { UbicacionForm } from "@/features/ubicaciones/components/ubicacion-form";
import { useCrearUbicacionMutation } from "@/features/ubicaciones/queries";
import { getApiErrorMessage } from "@/features/ubicaciones/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { permissions } from "@/lib/auth";

export default function NuevaUbicacionPage() {
  const router = useRouter();
  const mutation = useCrearUbicacionMutation();

  return (
    <AppShell requiredRoles={[...permissions.adminRoles]}>
      <div className="space-y-6">
        <PageHeader description="Crear una ubicacion disponible para movimientos de objetos." title="Nueva ubicacion" />
        {mutation.isError ? <ErrorState message={getApiErrorMessage(mutation.error)} requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined} /> : null}
        <UbicacionForm
          isSubmitting={mutation.isPending}
          onSubmit={(payload) => mutation.mutate(payload, { onSuccess: () => router.push("/ubicaciones") })}
          submitLabel="Crear ubicacion"
        />
      </div>
    </AppShell>
  );
}

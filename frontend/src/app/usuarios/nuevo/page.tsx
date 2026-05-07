"use client";

import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { UsuarioForm } from "@/features/usuarios/components/usuario-form";
import { useCrearUsuarioMutation } from "@/features/usuarios/queries";
import { getApiErrorMessage } from "@/features/usuarios/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevoUsuarioPage() {
  const router = useRouter();
  const mutation = useCrearUsuarioMutation();

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader description="Alta de usuario en Keycloak." title="Nuevo usuario" />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        <UsuarioForm
          includeInitialPassword
          isSubmitting={mutation.isPending}
          onSubmit={({ usuario }) => mutation.mutate(usuario, { onSuccess: (nuevoUsuario) => router.push(`/usuarios/${nuevoUsuario.id}`) })}
          submitError={mutation.error}
          submitLabel="Crear usuario"
        />
      </div>
    </AppShell>
  );
}

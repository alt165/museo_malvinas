"use client";

import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { UsuarioForm } from "@/features/usuarios/components/usuario-form";
import { useActualizarUsuarioMutation, useAsignarRolUsuarioMutation, useUsuarioQuery } from "@/features/usuarios/queries";
import { getApiErrorMessage, getRolPrincipal } from "@/features/usuarios/utils";
import { useAuth } from "@/lib/auth";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value ?? "";
}

export default function EditarUsuarioPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { user } = useAuth();
  const { data, error, isError, isLoading } = useUsuarioQuery(id);
  const actualizarMutation = useActualizarUsuarioMutation(id);
  const rolMutation = useAsignarRolUsuarioMutation(id);

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader description="Actualizar datos y rol del usuario en Keycloak." title="Editar usuario" />
        {isLoading ? <LoadingState label="Cargando usuario..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {actualizarMutation.isError ? <ErrorState message={getApiErrorMessage(actualizarMutation.error)} requestId={actualizarMutation.error instanceof ApiClientError ? actualizarMutation.error.requestId : undefined} /> : null}
        {rolMutation.isError ? <ErrorState message={getApiErrorMessage(rolMutation.error)} requestId={rolMutation.error instanceof ApiClientError ? rolMutation.error.requestId : undefined} /> : null}
        {data ? (
          <UsuarioForm
            initialValue={data}
            isSubmitting={actualizarMutation.isPending || rolMutation.isPending}
            onSubmit={({ usuario, rol }) => {
              const currentRol = getRolPrincipal(data.roles);
              const isSelfAdminRemoval = user?.id === data.id && currentRol === "ADMIN" && rol !== "ADMIN";
              const confirmarQuitarAdminPropio = isSelfAdminRemoval
                ? window.confirm("Confirmar que queres quitarte tu propio rol ADMIN?")
                : false;

              if (isSelfAdminRemoval && !confirmarQuitarAdminPropio) {
                return;
              }

              actualizarMutation.mutate(usuario, {
                onSuccess: () => {
                  rolMutation.mutate(
                    { roles: [rol], confirmarQuitarAdminPropio },
                    { onSuccess: (usuarioActualizado) => router.push(`/usuarios/${usuarioActualizado.id}`) }
                  );
                }
              });
            }}
            submitError={actualizarMutation.error ?? rolMutation.error}
            submitLabel="Guardar cambios"
          />
        ) : null}
      </div>
    </AppShell>
  );
}

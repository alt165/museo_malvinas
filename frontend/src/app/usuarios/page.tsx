"use client";

import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { UsuariosTable } from "@/features/usuarios/components/usuarios-table";
import { useCambiarEstadoUsuarioMutation, useUsuariosQuery } from "@/features/usuarios/queries";
import type { UsuarioKeycloakResponseDTO } from "@/features/usuarios/types";
import { getApiErrorMessage } from "@/features/usuarios/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function UsuariosPage() {
  const usuariosQuery = useUsuariosQuery();
  const estadoMutation = useCambiarEstadoUsuarioMutation();
  const { canAdminEdit } = useEditingMode();

  function handleToggleEnabled(usuario: UsuarioKeycloakResponseDTO) {
    if (!canAdminEdit) {
      return;
    }

    const accion = usuario.habilitado ? "deshabilitar" : "habilitar";

    if (window.confirm(`Confirmar ${accion} usuario ${usuario.username}?`)) {
      estadoMutation.mutate({ id: usuario.id, habilitado: !usuario.habilitado });
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader
          actions={canAdminEdit ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/usuarios/nuevo">Nuevo usuario</Link> : null}
          description="Usuarios reales administrados en Keycloak."
          title="Usuarios"
        />
        {usuariosQuery.isLoading ? <LoadingState label="Cargando usuarios..." /> : null}
        {usuariosQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(usuariosQuery.error)}
            requestId={usuariosQuery.error instanceof ApiClientError ? usuariosQuery.error.requestId : undefined}
          />
        ) : null}
        {estadoMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(estadoMutation.error)}
            requestId={estadoMutation.error instanceof ApiClientError ? estadoMutation.error.requestId : undefined}
          />
        ) : null}
        {usuariosQuery.data?.length === 0 ? <EmptyState title="Sin usuarios" description="No hay usuarios disponibles en Keycloak." /> : null}
        {usuariosQuery.data && usuariosQuery.data.length > 0 ? (
          <UsuariosTable
            canEdit={canAdminEdit}
            isUpdating={estadoMutation.isPending}
            onToggleEnabled={handleToggleEnabled}
            usuarios={usuariosQuery.data}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

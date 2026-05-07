"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ResetPasswordForm } from "@/features/usuarios/components/reset-password-form";
import { useCambiarEstadoUsuarioMutation, useResetearPasswordUsuarioMutation, useUsuarioQuery } from "@/features/usuarios/queries";
import { getApiErrorMessage, nombreCompleto } from "@/features/usuarios/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value ?? "";
}

export default function DetalleUsuarioPage() {
  const params = useParams<{ id: string }>();
  const id = getParamId(params.id);
  const { data, error, isError, isLoading } = useUsuarioQuery(id);
  const estadoMutation = useCambiarEstadoUsuarioMutation();
  const resetMutation = useResetearPasswordUsuarioMutation(id);

  function handleToggleEnabled() {
    if (!data) {
      return;
    }

    const accion = data.habilitado ? "deshabilitar" : "habilitar";

    if (window.confirm(`Confirmar ${accion} usuario ${data.username}?`)) {
      estadoMutation.mutate({ id: data.id, habilitado: !data.habilitado });
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader
          actions={<div className="flex gap-2"><Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/usuarios">Volver</Link>{data ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/usuarios/${data.id}/editar`}>Editar</Link> : null}{data ? <button className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-60" disabled={estadoMutation.isPending} onClick={handleToggleEnabled} type="button">{data.habilitado ? "Deshabilitar" : "Habilitar"}</button> : null}</div>}
          description="Datos del usuario en Keycloak."
          title="Detalle de usuario"
        />
        {isLoading ? <LoadingState label="Cargando usuario..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {estadoMutation.isError ? <ErrorState message={getApiErrorMessage(estadoMutation.error)} requestId={estadoMutation.error instanceof ApiClientError ? estadoMutation.error.requestId : undefined} /> : null}
        {resetMutation.isError ? <ErrorState message={getApiErrorMessage(resetMutation.error)} requestId={resetMutation.error instanceof ApiClientError ? resetMutation.error.requestId : undefined} /> : null}
        {resetMutation.isSuccess ? <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">Contrasena temporal actualizada.</div> : null}
        {data ? (
          <>
            <div className="rounded-lg border p-5">
              <dl className="grid gap-5 text-sm sm:grid-cols-2">
                <div>
                  <dt className="text-muted-foreground">Usuario</dt>
                  <dd className="font-medium">{data.username}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Email</dt>
                  <dd className="font-medium">{data.email}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Nombre</dt>
                  <dd className="font-medium">{nombreCompleto(data)}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Estado</dt>
                  <dd className="font-medium">{data.habilitado ? "Habilitado" : "Deshabilitado"}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Rol</dt>
                  <dd className="font-medium">{data.roles.join(", ") || "Sin rol"}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Id Keycloak</dt>
                  <dd className="break-all font-medium">{data.id}</dd>
                </div>
              </dl>
            </div>
            <ResetPasswordForm
              isSubmitting={resetMutation.isPending}
              onSubmit={(values, onDone) => resetMutation.mutate({ contrasena: values.contrasena }, { onSuccess: onDone })}
              submitError={resetMutation.error}
            />
          </>
        ) : null}
      </div>
    </AppShell>
  );
}

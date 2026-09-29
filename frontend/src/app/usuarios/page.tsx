"use client";

import { Search, X } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { UsuariosTable } from "@/features/usuarios/components/usuarios-table";
import { useCambiarEstadoUsuarioMutation, useUsuariosQuery } from "@/features/usuarios/queries";
import { rolesUsuario, type UsuarioKeycloakResponseDTO } from "@/features/usuarios/types";
import { getApiErrorMessage } from "@/features/usuarios/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";
import { normalizarTextoBusqueda } from "@/lib/utils";
import type { UserRole } from "@/models/session";

export default function UsuariosPage() {
  const usuariosQuery = useUsuariosQuery();
  const estadoMutation = useCambiarEstadoUsuarioMutation();
  const { canAdminEdit } = useEditingMode();
  const [busqueda, setBusqueda] = useState("");
  const [rol, setRol] = useState<UserRole | "">("");
  const usuarios = useMemo(() => usuariosQuery.data ?? [], [usuariosQuery.data]);
  const valorBusqueda = normalizarTextoBusqueda(busqueda);
  const hayFiltros = valorBusqueda.length > 0 || rol !== "";
  const usuariosFiltrados = useMemo(
    () => usuarios.filter((usuario) => {
      const coincideTexto = !valorBusqueda || [usuario.username, usuario.email, usuario.dni, usuario.nombre, usuario.apellido]
        .some((valor) => valor ? normalizarTextoBusqueda(valor).includes(valorBusqueda) : false);
      const coincideRol = !rol || usuario.roles.includes(rol);
      return coincideTexto && coincideRol;
    }),
    [rol, usuarios, valorBusqueda]
  );

  function limpiarFiltros() {
    setBusqueda("");
    setRol("");
  }

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
        <div className="rounded-lg border bg-card p-4">
          <label className="block text-sm font-medium" htmlFor="buscar-usuario">
            Buscar usuario por usuario, nombre, apellido, email o DNI
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={usuariosQuery.isLoading}
                id="buscar-usuario"
                onChange={(event) => setBusqueda(event.target.value)}
                placeholder="Usuario, nombre, apellido, email o DNI"
                type="search"
                value={busqueda}
              />
            </div>
            <select
              aria-label="Filtrar usuarios por rol"
              className="h-10 rounded-md border bg-background px-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60 sm:w-44"
              disabled={usuariosQuery.isLoading}
              onChange={(event) => setRol(event.target.value as UserRole | "")}
              value={rol}
            >
              <option value="">Todos los roles</option>
              {rolesUsuario.map((rolDisponible) => <option key={rolDisponible} value={rolDisponible}>{rolDisponible}</option>)}
            </select>
            {hayFiltros ? (
              <button
                className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-3 text-sm font-medium hover:bg-muted"
                onClick={limpiarFiltros}
                type="button"
              >
                <X className="h-4 w-4" />
                Limpiar
              </button>
            ) : null}
          </div>
        </div>
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
        {!usuariosQuery.isLoading && !usuariosQuery.isError && usuarios.length === 0 && !hayFiltros ? <EmptyState title="Sin usuarios" description="No hay usuarios disponibles en Keycloak." /> : null}
        {!usuariosQuery.isLoading && !usuariosQuery.isError && hayFiltros && usuariosFiltrados.length === 0 ? <EmptyState title="Sin resultados" description="No hay usuarios que coincidan con los filtros aplicados." /> : null}
        {!usuariosQuery.isLoading && !usuariosQuery.isError && usuariosFiltrados.length > 0 ? (
          <UsuariosTable
            canEdit={canAdminEdit}
            isUpdating={estadoMutation.isPending}
            onToggleEnabled={handleToggleEnabled}
            usuarios={usuariosFiltrados}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

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
  const [busquedaAplicada, setBusquedaAplicada] = useState("");
  const [rolAplicado, setRolAplicado] = useState<UserRole | "">("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const usuarios = useMemo(() => usuariosQuery.data ?? [], [usuariosQuery.data]);
  const valorBusquedaAplicada = normalizarTextoBusqueda(busquedaAplicada);
  const hayFiltrosAplicados = valorBusquedaAplicada.length > 0 || rolAplicado !== "";
  const hayFiltrosEscritos = busqueda.length > 0 || rol !== "";
  const usuariosFiltrados = useMemo(
    () => usuarios.filter((usuario) => {
      const nombreCompleto = `${usuario.nombre ?? ""} ${usuario.apellido ?? ""}`;
      const coincideTexto = !valorBusquedaAplicada || [usuario.username, usuario.nombre, usuario.apellido, nombreCompleto, usuario.dni]
        .some((valor) => valor ? normalizarTextoBusqueda(valor).includes(valorBusquedaAplicada) : false);
      const coincideRol = !rolAplicado || usuario.roles.includes(rolAplicado);
      return coincideTexto && coincideRol;
    }),
    [rolAplicado, usuarios, valorBusquedaAplicada]
  );
  const totalPaginas = Math.max(Math.ceil(usuariosFiltrados.length / size), 1);
  const paginaActual = Math.min(page, totalPaginas - 1);
  const usuariosPaginados = useMemo(() => {
    const inicio = paginaActual * size;
    return usuariosFiltrados.slice(inicio, inicio + size);
  }, [paginaActual, size, usuariosFiltrados]);

  function aplicarFiltros(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setBusquedaAplicada(busqueda);
    setRolAplicado(rol);
  }

  function limpiarFiltros() {
    setPage(0);
    setBusqueda("");
    setRol("");
    setBusquedaAplicada("");
    setRolAplicado("");
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
        <form className="rounded-lg border bg-card p-4" onSubmit={aplicarFiltros}>
          <label className="block text-sm font-medium" htmlFor="buscar-usuario">
            Buscar usuario por usuario, nombre, apellido, nombre completo o DNI
          </label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                className="h-10 w-full rounded-md border bg-background py-2 pl-9 pr-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60"
                id="buscar-usuario"
                onChange={(event) => setBusqueda(event.target.value)}
                placeholder="Usuario, nombre, apellido, nombre completo o DNI"
                type="search"
                value={busqueda}
              />
            </div>
            <button className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90" type="submit">Buscar</button>
            <select
              aria-label="Filtrar usuarios por rol"
              className="h-10 rounded-md border bg-background px-3 text-sm outline-none transition focus:border-ring focus:ring-2 focus:ring-ring/20 disabled:cursor-not-allowed disabled:opacity-60 sm:w-44"
              onChange={(event) => setRol(event.target.value as UserRole | "")}
              value={rol}
            >
              <option value="">Todos los roles</option>
              {rolesUsuario.map((rolDisponible) => <option key={rolDisponible} value={rolDisponible}>{rolDisponible}</option>)}
            </select>
            {hayFiltrosEscritos || hayFiltrosAplicados ? (
              <button
                className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-3 text-sm font-medium hover:bg-muted"
                onClick={limpiarFiltros}
                type="button"
              >
                <X className="h-4 w-4" />
                Limpiar filtros
              </button>
            ) : null}
          </div>
        </form>
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
        {!usuariosQuery.isLoading && !usuariosQuery.isError && usuarios.length === 0 && !hayFiltrosAplicados ? <EmptyState title="Sin usuarios" description="No hay usuarios disponibles en Keycloak." /> : null}
        {!usuariosQuery.isLoading && !usuariosQuery.isError && hayFiltrosAplicados && usuariosFiltrados.length === 0 ? <EmptyState title="Sin resultados" description="No hay usuarios que coincidan con los filtros aplicados." /> : null}
        {!usuariosQuery.isLoading && !usuariosQuery.isError && usuariosFiltrados.length > 0 ? (
          <UsuariosTable
            canEdit={canAdminEdit}
            isUpdating={estadoMutation.isPending}
            onToggleEnabled={handleToggleEnabled}
            usuarios={usuariosPaginados}
          />
        ) : null}
        {!usuariosQuery.isLoading && !usuariosQuery.isError && usuariosQuery.data ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface px-4 py-3 text-sm">
            <div className="text-muted-foreground">{usuariosFiltrados.length} usuarios encontrados</div>
            <div className="flex items-center gap-2">
              <label className="flex items-center gap-2">
                <span>Cantidad por página</span>
                <select
                  className="h-9 rounded-md border bg-white px-2"
                  onChange={(event) => {
                    setPage(0);
                    setSize(Number(event.target.value));
                  }}
                  value={size}
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>
              </label>
              <button
                className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                disabled={paginaActual === 0}
                onClick={() => setPage(Math.max(0, paginaActual - 1))}
                type="button"
              >Anterior</button>
              <span>Página {paginaActual + 1} de {totalPaginas}</span>
              <button
                className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                disabled={paginaActual >= totalPaginas - 1}
                onClick={() => setPage(Math.min(paginaActual + 1, totalPaginas - 1))}
                type="button"
              >Siguiente</button>
            </div>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarUsuario,
  asignarRolUsuario,
  cambiarEstadoUsuario,
  crearUsuario,
  listarUsuarios,
  obtenerUsuarioPorId,
  resetearPasswordUsuario
} from "./api";
import type { AsignarRolRequestDTO, ResetPasswordRequestDTO, UsuarioKeycloakRequestDTO } from "./types";

export const usuariosQueryKeys = {
  all: ["usuarios"] as const,
  lists: () => [...usuariosQueryKeys.all, "list"] as const,
  detail: (id: string) => [...usuariosQueryKeys.all, "detail", id] as const
};

export function useUsuariosQuery() {
  return useQuery({
    queryKey: usuariosQueryKeys.lists(),
    queryFn: listarUsuarios
  });
}

export function useUsuarioQuery(id: string) {
  return useQuery({
    queryKey: usuariosQueryKeys.detail(id),
    queryFn: () => obtenerUsuarioPorId(id),
    enabled: Boolean(id)
  });
}

export function useCrearUsuarioMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: crearUsuario,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: usuariosQueryKeys.all });
    }
  });
}

export function useActualizarUsuarioMutation(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UsuarioKeycloakRequestDTO) => actualizarUsuario(id, payload),
    onSuccess: (usuario) => {
      queryClient.setQueryData(usuariosQueryKeys.detail(id), usuario);
      void queryClient.invalidateQueries({ queryKey: usuariosQueryKeys.lists() });
    }
  });
}

export function useCambiarEstadoUsuarioMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, habilitado }: { id: string; habilitado: boolean }) => cambiarEstadoUsuario(id, habilitado),
    onSuccess: (usuario) => {
      queryClient.setQueryData(usuariosQueryKeys.detail(usuario.id), usuario);
      void queryClient.invalidateQueries({ queryKey: usuariosQueryKeys.lists() });
    }
  });
}

export function useAsignarRolUsuarioMutation(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: AsignarRolRequestDTO) => asignarRolUsuario(id, payload),
    onSuccess: (usuario) => {
      queryClient.setQueryData(usuariosQueryKeys.detail(id), usuario);
      void queryClient.invalidateQueries({ queryKey: usuariosQueryKeys.lists() });
    }
  });
}

export function useResetearPasswordUsuarioMutation(id: string) {
  return useMutation({
    mutationFn: (payload: ResetPasswordRequestDTO) => resetearPasswordUsuario(id, payload)
  });
}

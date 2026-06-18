import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarUbicacion,
  bajaLogicaUbicacion,
  crearUbicacion,
  listarUbicaciones,
  obtenerUbicacionPorId
} from "./api";
import type { UbicacionRequestDTO } from "./types";

export const ubicacionesQueryKeys = {
  all: ["ubicaciones"] as const,
  lists: () => [...ubicacionesQueryKeys.all, "list"] as const,
  detail: (id: number) => [...ubicacionesQueryKeys.all, "detail", id] as const
};

export function useUbicacionesQuery() {
  return useQuery({
    queryKey: ubicacionesQueryKeys.lists(),
    queryFn: listarUbicaciones
  });
}

export function useUbicacionQuery(id: number) {
  return useQuery({
    queryKey: ubicacionesQueryKeys.detail(id),
    queryFn: () => obtenerUbicacionPorId(id),
    enabled: Number.isFinite(id)
  });
}

export function useCrearUbicacionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UbicacionRequestDTO) => crearUbicacion(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ubicacionesQueryKeys.all });
    }
  });
}

export function useActualizarUbicacionMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UbicacionRequestDTO) => actualizarUbicacion(id, payload),
    onSuccess: (ubicacion) => {
      queryClient.setQueryData(ubicacionesQueryKeys.detail(id), ubicacion);
      void queryClient.invalidateQueries({ queryKey: ubicacionesQueryKeys.lists() });
    }
  });
}

export function useBajaLogicaUbicacionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: bajaLogicaUbicacion,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ubicacionesQueryKeys.all });
    }
  });
}

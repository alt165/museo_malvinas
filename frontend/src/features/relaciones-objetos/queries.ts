import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarRelacionObjeto,
  bajaLogicaRelacionObjeto,
  crearRelacionObjeto,
  listarRelacionesObjeto,
  listarRelacionesPorObjeto,
  obtenerRelacionObjetoPorId
} from "./api";
import type { RelacionObjetoRequestDTO } from "./types";

export const relacionesObjetosQueryKeys = {
  all: ["relaciones-objetos"] as const,
  lists: () => [...relacionesObjetosQueryKeys.all, "list"] as const,
  byObjeto: (objetoId: number) => [...relacionesObjetosQueryKeys.all, "objeto", objetoId] as const,
  detail: (id: number) => [...relacionesObjetosQueryKeys.all, "detail", id] as const
};

export function useRelacionesObjetoQuery() {
  return useQuery({
    queryKey: relacionesObjetosQueryKeys.lists(),
    queryFn: listarRelacionesObjeto
  });
}

export function useRelacionObjetoQuery(id: number) {
  return useQuery({
    queryKey: relacionesObjetosQueryKeys.detail(id),
    queryFn: () => obtenerRelacionObjetoPorId(id),
    enabled: Number.isFinite(id)
  });
}

export function useRelacionesPorObjetoQuery(objetoId: number) {
  return useQuery({
    queryKey: relacionesObjetosQueryKeys.byObjeto(objetoId),
    queryFn: () => listarRelacionesPorObjeto(objetoId),
    enabled: Number.isFinite(objetoId)
  });
}

export function useCrearRelacionObjetoMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: crearRelacionObjeto,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: relacionesObjetosQueryKeys.all });
    }
  });
}

export function useActualizarRelacionObjetoMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: RelacionObjetoRequestDTO) => actualizarRelacionObjeto(id, payload),
    onSuccess: (relacion) => {
      queryClient.setQueryData(relacionesObjetosQueryKeys.detail(id), relacion);
      void queryClient.invalidateQueries({ queryKey: relacionesObjetosQueryKeys.all });
    }
  });
}

export function useBajaLogicaRelacionObjetoMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: bajaLogicaRelacionObjeto,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: relacionesObjetosQueryKeys.all });
    }
  });
}

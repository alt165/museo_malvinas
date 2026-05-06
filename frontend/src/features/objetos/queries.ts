import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarObjeto,
  bajaLogicaObjeto,
  crearObjeto,
  listarObjetos,
  obtenerObjetoPorId
} from "./api";
import type { ObjetoMuseoRequestDTO } from "./types";

export const objetosQueryKeys = {
  all: ["objetos"] as const,
  lists: () => [...objetosQueryKeys.all, "list"] as const,
  detail: (id: number) => [...objetosQueryKeys.all, "detail", id] as const
};

export function useObjetosQuery() {
  return useQuery({
    queryKey: objetosQueryKeys.lists(),
    queryFn: listarObjetos
  });
}

export function useObjetoQuery(id: number) {
  return useQuery({
    queryKey: objetosQueryKeys.detail(id),
    queryFn: () => obtenerObjetoPorId(id),
    enabled: Number.isFinite(id)
  });
}

export function useCrearObjetoMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: crearObjeto,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.all });
    }
  });
}

export function useActualizarObjetoMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ObjetoMuseoRequestDTO) => actualizarObjeto(id, payload),
    onSuccess: (objeto) => {
      queryClient.setQueryData(objetosQueryKeys.detail(id), objeto);
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.lists() });
    }
  });
}

export function useBajaLogicaObjetoMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: bajaLogicaObjeto,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.all });
    }
  });
}

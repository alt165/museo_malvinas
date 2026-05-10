import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarObjeto,
  bajaLogicaObjeto,
  cargaRapidaObjeto,
  crearObjeto,
  eliminarFotoObjeto,
  listarFotosObjeto,
  listarObjetos,
  listarRecibosObjeto,
  obtenerObjetoPorId
} from "./api";
import type { CargaRapidaObjetoRequestDTO, ObjetoMuseoRequestDTO } from "./types";

export const objetosQueryKeys = {
  all: ["objetos"] as const,
  lists: () => [...objetosQueryKeys.all, "list"] as const,
  detail: (id: number) => [...objetosQueryKeys.all, "detail", id] as const,
  fotos: (id: number) => [...objetosQueryKeys.all, "detail", id, "fotos"] as const,
  recibos: (id: number) => [...objetosQueryKeys.all, "detail", id, "recibos"] as const
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

export function useCargaRapidaObjetoMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CargaRapidaObjetoRequestDTO) => cargaRapidaObjeto(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.all });
    }
  });
}

export function useFotosObjetoQuery(id: number) {
  return useQuery({
    queryKey: objetosQueryKeys.fotos(id),
    queryFn: () => listarFotosObjeto(id),
    enabled: Number.isFinite(id)
  });
}

export function useRecibosObjetoQuery(id: number) {
  return useQuery({
    queryKey: objetosQueryKeys.recibos(id),
    queryFn: () => listarRecibosObjeto(id),
    enabled: Number.isFinite(id)
  });
}

export function useEliminarFotoObjetoMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (fotoId: number) => eliminarFotoObjeto(id, fotoId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.fotos(id) });
    }
  });
}

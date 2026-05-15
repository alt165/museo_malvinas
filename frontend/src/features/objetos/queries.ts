import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarObjeto,
  bajaLogicaObjeto,
  buscarObjetos,
  cargaRapidaObjeto,
  crearObjeto,
  eliminarFotoObjeto,
  listarFotosObjeto,
  listarObjetos,
  listarObjetosEliminados,
  listarObjetosPendientesCompletar,
  listarRecibosObjeto,
  obtenerObjetoPorId,
  obtenerReciboEscaneadoObjeto,
  restaurarObjeto
} from "./api";
import type { BuscarObjetosParams, CargaRapidaObjetoRequestDTO, ObjetoMuseoRequestDTO } from "./types";

export const objetosQueryKeys = {
  all: ["objetos"] as const,
  lists: () => [...objetosQueryKeys.all, "list"] as const,
  search: (params: BuscarObjetosParams) => [...objetosQueryKeys.all, "search", params] as const,
  deleted: (params: { page?: number; size?: number; sort?: string }) => [...objetosQueryKeys.all, "deleted", params] as const,
  pending: (params: { page?: number; size?: number; sort?: string }) => [...objetosQueryKeys.all, "pending", params] as const,
  detail: (id: number) => [...objetosQueryKeys.all, "detail", id] as const,
  fotos: (id: number) => [...objetosQueryKeys.all, "detail", id, "fotos"] as const,
  reciboEscaneado: (id: number) => [...objetosQueryKeys.all, "detail", id, "recibo-escaneado"] as const,
  recibos: (id: number) => [...objetosQueryKeys.all, "detail", id, "recibos"] as const
};

export function useObjetosQuery() {
  return useQuery({
    queryKey: objetosQueryKeys.lists(),
    queryFn: listarObjetos
  });
}

export function useBuscarObjetosQuery(params: BuscarObjetosParams) {
  return useQuery({
    queryKey: objetosQueryKeys.search(params),
    queryFn: () => buscarObjetos(params)
  });
}

export function useObjetosEliminadosQuery(params: { page?: number; size?: number; sort?: string }) {
  return useQuery({
    queryKey: objetosQueryKeys.deleted(params),
    queryFn: () => listarObjetosEliminados(params)
  });
}

export function useObjetosPendientesCompletarQuery(params: { page?: number; size?: number; sort?: string }) {
  return useQuery({
    queryKey: objetosQueryKeys.pending(params),
    queryFn: () => listarObjetosPendientesCompletar(params)
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
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.all });
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

export function useRestaurarObjetoMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: restaurarObjeto,
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

export function useReciboEscaneadoObjetoQuery(id: number) {
  return useQuery({
    queryKey: objetosQueryKeys.reciboEscaneado(id),
    queryFn: () => obtenerReciboEscaneadoObjeto(id),
    enabled: Number.isFinite(id),
    retry: false
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

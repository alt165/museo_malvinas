import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarExhibicion,
  agregarObjetoAExhibicion,
  buscarExhibicionesFinalizadas,
  buscarObjetosDisponibilidadExhibicion,
  cancelarExhibicion,
  crearExhibicion,
  finalizarExhibicion,
  listarExhibiciones,
  listarExhibicionesProximasInicio,
  listarObjetosDeExhibicion,
  obtenerExhibicionPorId,
  obtenerObjetosParaRepetirExhibicion,
  revertirDevolucionObjeto,
  verificarDevolucionObjeto
} from "./api";
import type { ExhibicionObjetoRequestDTO, ExhibicionRequestDTO } from "./types";
import type { BuscarDisponibilidadExhibicionParams, BuscarExhibicionesFinalizadasParams, ObjetosParaRepetirParams } from "./api/exhibiciones-api";

export const exhibicionesQueryKeys = {
  all: ["exhibiciones"] as const,
  lists: () => [...exhibicionesQueryKeys.all, "list"] as const,
  detail: (id: number) => [...exhibicionesQueryKeys.all, "detail", id] as const,
  objetos: (id: number) => [...exhibicionesQueryKeys.all, "objetos", id] as const,
  disponibilidad: (params: BuscarDisponibilidadExhibicionParams) => [...exhibicionesQueryKeys.all, "disponibilidad", params] as const,
  finalizadas: (params: BuscarExhibicionesFinalizadasParams) => [...exhibicionesQueryKeys.all, "finalizadas", params] as const,
  objetosParaRepetir: (params: ObjetosParaRepetirParams) => [...exhibicionesQueryKeys.all, "objetos-para-repetir", params] as const,
  proximasInicio: () => [...exhibicionesQueryKeys.all, "proximas-inicio"] as const
};

export function useExhibicionesQuery() {
  return useQuery({
    queryKey: exhibicionesQueryKeys.lists(),
    queryFn: listarExhibiciones
  });
}

export function useExhibicionQuery(id: number) {
  return useQuery({
    queryKey: exhibicionesQueryKeys.detail(id),
    queryFn: () => obtenerExhibicionPorId(id),
    enabled: Number.isFinite(id)
  });
}

export function useObjetosExhibicionQuery(exhibicionId: number) {
  return useQuery({
    queryKey: exhibicionesQueryKeys.objetos(exhibicionId),
    queryFn: async () => {
      const objetos = await listarObjetosDeExhibicion();
      return objetos.filter((objeto) => objeto.exhibicionId === exhibicionId);
    },
    enabled: Number.isFinite(exhibicionId)
  });
}

export function useObjetosDisponibilidadExhibicionQuery(params: BuscarDisponibilidadExhibicionParams, enabled: boolean) {
  return useQuery({
    queryKey: exhibicionesQueryKeys.disponibilidad(params),
    queryFn: () => buscarObjetosDisponibilidadExhibicion(params),
    enabled
  });
}

export function useExhibicionesFinalizadasQuery(params: BuscarExhibicionesFinalizadasParams, enabled: boolean) {
  return useQuery({
    queryKey: exhibicionesQueryKeys.finalizadas(params),
    queryFn: () => buscarExhibicionesFinalizadas(params),
    enabled
  });
}

export function useObjetosParaRepetirExhibicionQuery(params: ObjetosParaRepetirParams, enabled: boolean) {
  return useQuery({
    queryKey: exhibicionesQueryKeys.objetosParaRepetir(params),
    queryFn: () => obtenerObjetosParaRepetirExhibicion(params),
    enabled
  });
}

export function useExhibicionesProximasInicioQuery(enabled: boolean) {
  return useQuery({
    queryKey: exhibicionesQueryKeys.proximasInicio(),
    queryFn: listarExhibicionesProximasInicio,
    enabled
  });
}

export function useCrearExhibicionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: crearExhibicion,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.all });
    }
  });
}

export function useActualizarExhibicionMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ExhibicionRequestDTO) => actualizarExhibicion(id, payload),
    onSuccess: (exhibicion) => {
      queryClient.setQueryData(exhibicionesQueryKeys.detail(id), exhibicion);
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.lists() });
    }
  });
}

export function useFinalizarExhibicionMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => finalizarExhibicion(id),
    onSuccess: (exhibicion) => {
      queryClient.setQueryData(exhibicionesQueryKeys.detail(id), exhibicion);
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.proximasInicio() });
    }
  });
}

export function useCancelarExhibicionMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => cancelarExhibicion(id),
    onSuccess: (exhibicion) => {
      queryClient.setQueryData(exhibicionesQueryKeys.detail(id), exhibicion);
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.proximasInicio() });
    }
  });
}

export function useFinalizarExhibicionPorIdMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: finalizarExhibicion,
    onSuccess: (exhibicion) => {
      queryClient.setQueryData(exhibicionesQueryKeys.detail(exhibicion.id), exhibicion);
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.proximasInicio() });
    }
  });
}

export function useCancelarExhibicionPorIdMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: cancelarExhibicion,
    onSuccess: (exhibicion) => {
      queryClient.setQueryData(exhibicionesQueryKeys.detail(exhibicion.id), exhibicion);
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.proximasInicio() });
    }
  });
}

export function useAgregarObjetoAExhibicionMutation(exhibicionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ExhibicionObjetoRequestDTO) => agregarObjetoAExhibicion(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.objetos(exhibicionId) });
    }
  });
}

export function useVerificarDevolucionMutation(exhibicionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, observaciones }: { id: number; observaciones?: string }) => verificarDevolucionObjeto(id, observaciones),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.objetos(exhibicionId) });
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.detail(exhibicionId) });
    }
  });
}

export function useRevertirDevolucionMutation(exhibicionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: revertirDevolucionObjeto,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.objetos(exhibicionId) });
      void queryClient.invalidateQueries({ queryKey: exhibicionesQueryKeys.detail(exhibicionId) });
    }
  });
}

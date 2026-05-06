import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarExhibicion,
  agregarObjetoAExhibicion,
  crearExhibicion,
  finalizarExhibicion,
  listarExhibiciones,
  listarObjetosDeExhibicion,
  obtenerExhibicionPorId,
  revertirDevolucionObjeto,
  verificarDevolucionObjeto
} from "./api";
import type { ExhibicionObjetoRequestDTO, ExhibicionRequestDTO } from "./types";

export const exhibicionesQueryKeys = {
  all: ["exhibiciones"] as const,
  lists: () => [...exhibicionesQueryKeys.all, "list"] as const,
  detail: (id: number) => [...exhibicionesQueryKeys.all, "detail", id] as const,
  objetos: (id: number) => [...exhibicionesQueryKeys.all, "objetos", id] as const
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

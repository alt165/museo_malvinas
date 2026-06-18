import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listarObjetosSinColeccion } from "@/features/objetos/api";
import {
  actualizarColeccion,
  agregarObjetosColeccion,
  bajaLogicaColeccion,
  crearColeccion,
  listarColecciones,
  listarObjetosColeccion,
  obtenerColeccionPorId,
  quitarObjetoColeccion
} from "./api";
import type { AgregarObjetosColeccionRequestDTO, ColeccionObjetoRequestDTO } from "./types";

export const coleccionesQueryKeys = {
  all: ["colecciones"] as const,
  lists: () => [...coleccionesQueryKeys.all, "list"] as const,
  detail: (id: number) => [...coleccionesQueryKeys.all, "detail", id] as const,
  objetos: (id: number) => [...coleccionesQueryKeys.all, "detail", id, "objetos"] as const,
  objetosDisponibles: () => [...coleccionesQueryKeys.all, "objetos-disponibles"] as const
};

export function useColeccionesQuery() {
  return useQuery({
    queryKey: coleccionesQueryKeys.lists(),
    queryFn: listarColecciones
  });
}

export function useColeccionQuery(id: number) {
  return useQuery({
    queryKey: coleccionesQueryKeys.detail(id),
    queryFn: () => obtenerColeccionPorId(id),
    enabled: Number.isFinite(id)
  });
}

export function useObjetosColeccionQuery(id: number) {
  return useQuery({
    queryKey: coleccionesQueryKeys.objetos(id),
    queryFn: () => listarObjetosColeccion(id),
    enabled: Number.isFinite(id)
  });
}

export function useObjetosSinColeccionQuery() {
  return useQuery({
    queryKey: coleccionesQueryKeys.objetosDisponibles(),
    queryFn: listarObjetosSinColeccion
  });
}

export function useCrearColeccionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ColeccionObjetoRequestDTO) => crearColeccion(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.all });
    }
  });
}

export function useActualizarColeccionMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ColeccionObjetoRequestDTO) => actualizarColeccion(id, payload),
    onSuccess: (coleccion) => {
      queryClient.setQueryData(coleccionesQueryKeys.detail(id), coleccion);
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.lists() });
    }
  });
}

export function useBajaLogicaColeccionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: bajaLogicaColeccion,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.objetosDisponibles() });
    }
  });
}

export function useAgregarObjetosColeccionMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: AgregarObjetosColeccionRequestDTO) => agregarObjetosColeccion(id, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.detail(id) });
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.objetos(id) });
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.objetosDisponibles() });
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.lists() });
    }
  });
}

export function useQuitarObjetoColeccionMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (objetoId: number) => quitarObjetoColeccion(id, objetoId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.detail(id) });
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.objetos(id) });
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.objetosDisponibles() });
      void queryClient.invalidateQueries({ queryKey: coleccionesQueryKeys.lists() });
    }
  });
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarRelacionObjeto,
  bajaLogicaRelacionObjeto,
  crearRelacionObjeto,
  listarRelacionesDeObjeto,
  listarRelacionesDePersona,
  listarRelacionesObjeto,
  obtenerGrafoRelacionesObjeto,
  obtenerGrafoRelacionesPersona,
  obtenerRelacionObjetoPorId
} from "./api";
import type { RelacionObjetoRequestDTO } from "./types";

export const relacionesObjetosQueryKeys = {
  all: ["relaciones-objetos"] as const,
  lists: () => [...relacionesObjetosQueryKeys.all, "list"] as const,
  byObjeto: (objetoId: number) => [...relacionesObjetosQueryKeys.all, "objeto", objetoId] as const,
  grafoObjeto: (objetoId: number, profundidad: number) => [...relacionesObjetosQueryKeys.all, "objeto", objetoId, "grafo", profundidad] as const,
  byPersona: (personaId: number) => [...relacionesObjetosQueryKeys.all, "persona", personaId] as const,
  grafoPersona: (personaId: number, profundidad: number) => [...relacionesObjetosQueryKeys.all, "persona", personaId, "grafo", profundidad] as const,
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
    queryFn: () => listarRelacionesDeObjeto(objetoId),
    enabled: Number.isFinite(objetoId)
  });
}

export function useGrafoRelacionesObjetoQuery(objetoId: number, profundidad: number) {
  return useQuery({
    queryKey: relacionesObjetosQueryKeys.grafoObjeto(objetoId, profundidad),
    queryFn: () => obtenerGrafoRelacionesObjeto(objetoId, profundidad),
    enabled: Number.isFinite(objetoId) && profundidad >= 1 && profundidad <= 3
  });
}

export function useRelacionesPorPersonaQuery(personaId: number) {
  return useQuery({
    queryKey: relacionesObjetosQueryKeys.byPersona(personaId),
    queryFn: () => listarRelacionesDePersona(personaId),
    enabled: Number.isFinite(personaId)
  });
}

export function useGrafoRelacionesPersonaQuery(personaId: number, profundidad: number) {
  return useQuery({
    queryKey: relacionesObjetosQueryKeys.grafoPersona(personaId, profundidad),
    queryFn: () => obtenerGrafoRelacionesPersona(personaId, profundidad),
    enabled: Number.isFinite(personaId) && profundidad >= 1 && profundidad <= 3
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

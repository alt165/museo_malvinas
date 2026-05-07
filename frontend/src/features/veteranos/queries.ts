import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarActuacionVeterano,
  actualizarVeterano,
  asociarObjetoAVeterano,
  bajaLogicaActuacionVeterano,
  bajaLogicaVeterano,
  crearActuacionVeterano,
  crearVeterano,
  eliminarRelacionObjetoVeterano,
  listarActuacionesPorVeterano,
  listarActuacionesVeteranos,
  listarObjetosRelacionadosAVeterano,
  listarRelacionesObjetoVeterano,
  listarVeteranos,
  obtenerActuacionVeteranoPorId,
  obtenerVeteranoPorId
} from "./api";
import type { ActuacionVeteranoRequestDTO, ObjetoVeteranoRequestDTO, VeteranoRequestDTO } from "./types";

export const veteranosQueryKeys = {
  all: ["veteranos"] as const,
  lists: () => [...veteranosQueryKeys.all, "list"] as const,
  detail: (id: number) => [...veteranosQueryKeys.all, "detail", id] as const,
  actuaciones: (id: number) => [...veteranosQueryKeys.all, "actuaciones", id] as const,
  objetos: (id: number) => [...veteranosQueryKeys.all, "objetos", id] as const
};

export const actuacionesVeteranosQueryKeys = {
  all: ["actuaciones-veteranos"] as const,
  lists: () => [...actuacionesVeteranosQueryKeys.all, "list"] as const,
  detail: (id: number) => [...actuacionesVeteranosQueryKeys.all, "detail", id] as const
};

export const objetosVeteranosQueryKeys = {
  all: ["objetos-veteranos"] as const,
  lists: () => [...objetosVeteranosQueryKeys.all, "list"] as const
};

export function useVeteranosQuery() {
  return useQuery({ queryKey: veteranosQueryKeys.lists(), queryFn: listarVeteranos });
}

export function useVeteranoQuery(id: number) {
  return useQuery({ queryKey: veteranosQueryKeys.detail(id), queryFn: () => obtenerVeteranoPorId(id), enabled: Number.isFinite(id) });
}

export function useActuacionesVeteranosQuery() {
  return useQuery({ queryKey: actuacionesVeteranosQueryKeys.lists(), queryFn: listarActuacionesVeteranos });
}

export function useActuacionVeteranoQuery(id: number) {
  return useQuery({
    queryKey: actuacionesVeteranosQueryKeys.detail(id),
    queryFn: () => obtenerActuacionVeteranoPorId(id),
    enabled: Number.isFinite(id)
  });
}

export function useActuacionesVeteranoQuery(id: number) {
  return useQuery({ queryKey: veteranosQueryKeys.actuaciones(id), queryFn: () => listarActuacionesPorVeterano(id), enabled: Number.isFinite(id) });
}

export function useObjetosVeteranoQuery(id: number) {
  return useQuery({ queryKey: veteranosQueryKeys.objetos(id), queryFn: () => listarObjetosRelacionadosAVeterano(id), enabled: Number.isFinite(id) });
}

export function useRelacionesObjetoVeteranoQuery() {
  return useQuery({ queryKey: objetosVeteranosQueryKeys.lists(), queryFn: listarRelacionesObjetoVeterano });
}

export function useCrearVeteranoMutation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: crearVeterano, onSuccess: () => void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.all }) });
}

export function useActualizarVeteranoMutation(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: VeteranoRequestDTO) => actualizarVeterano(id, payload),
    onSuccess: (veterano) => {
      queryClient.setQueryData(veteranosQueryKeys.detail(id), veterano);
      void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.lists() });
    }
  });
}

export function useBajaLogicaVeteranoMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: bajaLogicaVeterano,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.all })
  });
}

export function useCrearActuacionVeteranoMutation(veteranoId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ActuacionVeteranoRequestDTO) => crearActuacionVeterano(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.actuaciones(veteranoId) });
      void queryClient.invalidateQueries({ queryKey: actuacionesVeteranosQueryKeys.all });
    }
  });
}

export function useCrearActuacionVeteranoGlobalMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: crearActuacionVeterano,
    onSuccess: (actuacion) => {
      void queryClient.invalidateQueries({ queryKey: actuacionesVeteranosQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.actuaciones(actuacion.veteranoId) });
    }
  });
}

export function useActualizarActuacionVeteranoMutation(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ActuacionVeteranoRequestDTO) => actualizarActuacionVeterano(id, payload),
    onSuccess: (actuacion) => {
      queryClient.setQueryData(actuacionesVeteranosQueryKeys.detail(id), actuacion);
      void queryClient.invalidateQueries({ queryKey: actuacionesVeteranosQueryKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.actuaciones(actuacion.veteranoId) });
    }
  });
}

export function useBajaLogicaActuacionVeteranoMutation(veteranoId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: bajaLogicaActuacionVeterano,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: actuacionesVeteranosQueryKeys.all });
      if (veteranoId) {
        void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.actuaciones(veteranoId) });
      }
    }
  });
}

export function useAsociarObjetoVeteranoMutation(veteranoId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ObjetoVeteranoRequestDTO) => asociarObjetoAVeterano(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.objetos(veteranoId) });
      void queryClient.invalidateQueries({ queryKey: objetosVeteranosQueryKeys.all });
    }
  });
}

export function useEliminarRelacionObjetoVeteranoMutation(veteranoId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: eliminarRelacionObjetoVeterano,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.objetos(veteranoId) });
      void queryClient.invalidateQueries({ queryKey: objetosVeteranosQueryKeys.all });
    }
  });
}

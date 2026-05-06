import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarVeterano,
  asociarObjetoAVeterano,
  crearActuacionVeterano,
  crearVeterano,
  eliminarRelacionObjetoVeterano,
  listarActuacionesPorVeterano,
  listarActuacionesVeteranos,
  listarObjetosRelacionadosAVeterano,
  listarRelacionesObjetoVeterano,
  listarVeteranos,
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

export function useVeteranosQuery() {
  return useQuery({ queryKey: veteranosQueryKeys.lists(), queryFn: listarVeteranos });
}

export function useVeteranoQuery(id: number) {
  return useQuery({ queryKey: veteranosQueryKeys.detail(id), queryFn: () => obtenerVeteranoPorId(id), enabled: Number.isFinite(id) });
}

export function useActuacionesVeteranosQuery() {
  return useQuery({ queryKey: ["actuaciones-veteranos"], queryFn: listarActuacionesVeteranos });
}

export function useActuacionesVeteranoQuery(id: number) {
  return useQuery({ queryKey: veteranosQueryKeys.actuaciones(id), queryFn: () => listarActuacionesPorVeterano(id), enabled: Number.isFinite(id) });
}

export function useObjetosVeteranoQuery(id: number) {
  return useQuery({ queryKey: veteranosQueryKeys.objetos(id), queryFn: () => listarObjetosRelacionadosAVeterano(id), enabled: Number.isFinite(id) });
}

export function useRelacionesObjetoVeteranoQuery() {
  return useQuery({ queryKey: ["objetos-veteranos"], queryFn: listarRelacionesObjetoVeterano });
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

export function useCrearActuacionVeteranoMutation(veteranoId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ActuacionVeteranoRequestDTO) => crearActuacionVeterano(payload),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.actuaciones(veteranoId) })
  });
}

export function useAsociarObjetoVeteranoMutation(veteranoId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ObjetoVeteranoRequestDTO) => asociarObjetoAVeterano(payload),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.objetos(veteranoId) })
  });
}

export function useEliminarRelacionObjetoVeteranoMutation(veteranoId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: eliminarRelacionObjetoVeterano,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: veteranosQueryKeys.objetos(veteranoId) })
  });
}

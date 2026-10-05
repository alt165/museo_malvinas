import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarDepositante,
  bajaLogicaDepositante,
  buscarDepositantePorIdentificacion,
  buscarDepositantes,
  buscarDepositantesPorNombre,
  crearDepositante,
  listarDepositantes,
  listarObjetosDepositante,
  obtenerDepositantePorId,
  type BuscarDepositantesParams
} from "./api";
import type { DepositanteRequestDTO } from "./types";

export const depositantesQueryKeys = {
  all: ["depositantes"] as const,
  lists: () => [...depositantesQueryKeys.all, "list"] as const,
  search: (params: BuscarDepositantesParams) => [...depositantesQueryKeys.lists(), "search", params] as const,
  identificacion: (valor: string) => [...depositantesQueryKeys.all, "identificacion", valor] as const,
  nombre: (valor: string) => [...depositantesQueryKeys.all, "nombre", valor] as const,
  detail: (id: number) => [...depositantesQueryKeys.all, "detail", id] as const,
  objetos: (id: number) => [...depositantesQueryKeys.all, "detail", id, "objetos"] as const
};

export function useDepositantesQuery() {
  return useQuery({
    queryKey: depositantesQueryKeys.lists(),
    queryFn: listarDepositantes
  });
}

export function useBuscarDepositantesQuery(params: BuscarDepositantesParams) {
  return useQuery({
    queryKey: depositantesQueryKeys.search(params),
    queryFn: () => buscarDepositantes(params)
  });
}

export function useDepositanteQuery(id: number) {
  return useQuery({
    queryKey: depositantesQueryKeys.detail(id),
    queryFn: () => obtenerDepositantePorId(id),
    enabled: Number.isFinite(id)
  });
}

export function useBuscarDepositantePorIdentificacionMutation() {
  return useMutation({
    mutationFn: buscarDepositantePorIdentificacion
  });
}

export function useBuscarDepositantesPorNombreQuery(valor: string) {
  const valorNormalizado = valor.trim();

  return useQuery({
    queryKey: depositantesQueryKeys.nombre(valorNormalizado),
    queryFn: () => buscarDepositantesPorNombre(valorNormalizado),
    enabled: valorNormalizado.length > 0
  });
}

export function useObjetosDepositanteQuery(id: number) {
  return useQuery({
    queryKey: depositantesQueryKeys.objetos(id),
    queryFn: () => listarObjetosDepositante(id),
    enabled: Number.isFinite(id)
  });
}

export function useCrearDepositanteMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: crearDepositante,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: depositantesQueryKeys.all });
    }
  });
}

export function useActualizarDepositanteMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: DepositanteRequestDTO) => actualizarDepositante(id, payload),
    onSuccess: (depositante) => {
      queryClient.setQueryData(depositantesQueryKeys.detail(id), depositante);
      void queryClient.invalidateQueries({ queryKey: depositantesQueryKeys.lists() });
    }
  });
}

export function useBajaLogicaDepositanteMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: bajaLogicaDepositante,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: depositantesQueryKeys.all });
    }
  });
}

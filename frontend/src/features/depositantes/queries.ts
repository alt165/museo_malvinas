import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarDepositante,
  bajaLogicaDepositante,
  buscarDepositantePorIdentificacion,
  crearDepositante,
  listarDepositantes,
  obtenerDepositantePorId
} from "./api";
import type { DepositanteRequestDTO } from "./types";

export const depositantesQueryKeys = {
  all: ["depositantes"] as const,
  lists: () => [...depositantesQueryKeys.all, "list"] as const,
  identificacion: (valor: string) => [...depositantesQueryKeys.all, "identificacion", valor] as const,
  detail: (id: number) => [...depositantesQueryKeys.all, "detail", id] as const
};

export function useDepositantesQuery() {
  return useQuery({
    queryKey: depositantesQueryKeys.lists(),
    queryFn: listarDepositantes
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

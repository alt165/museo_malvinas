import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarInventario,
  crearInventario,
  listarInventarios,
  listarMovimientosInventario,
  obtenerInventarioPorId,
  obtenerMovimientoPorId
} from "./api";
import type { InventarioRequestDTO } from "./types";

export const inventarioQueryKeys = {
  all: ["inventario"] as const,
  lists: () => [...inventarioQueryKeys.all, "list"] as const,
  detail: (id: number) => [...inventarioQueryKeys.all, "detail", id] as const
};

export const movimientosInventarioQueryKeys = {
  all: ["movimientos-inventario"] as const,
  lists: () => [...movimientosInventarioQueryKeys.all, "list"] as const,
  detail: (id: number) => [...movimientosInventarioQueryKeys.all, "detail", id] as const
};

export function useInventariosQuery() {
  return useQuery({
    queryKey: inventarioQueryKeys.lists(),
    queryFn: listarInventarios
  });
}

export function useInventarioQuery(id: number) {
  return useQuery({
    queryKey: inventarioQueryKeys.detail(id),
    queryFn: () => obtenerInventarioPorId(id),
    enabled: Number.isFinite(id)
  });
}

export function useCrearInventarioMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: crearInventario,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: inventarioQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: movimientosInventarioQueryKeys.all });
    }
  });
}

export function useActualizarInventarioMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: InventarioRequestDTO) => actualizarInventario(id, payload),
    onSuccess: (inventario) => {
      queryClient.setQueryData(inventarioQueryKeys.detail(id), inventario);
      void queryClient.invalidateQueries({ queryKey: inventarioQueryKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: movimientosInventarioQueryKeys.all });
    }
  });
}

export function useMovimientosInventarioQuery() {
  return useQuery({
    queryKey: movimientosInventarioQueryKeys.lists(),
    queryFn: listarMovimientosInventario
  });
}

export function useMovimientoInventarioQuery(id: number) {
  return useQuery({
    queryKey: movimientosInventarioQueryKeys.detail(id),
    queryFn: () => obtenerMovimientoPorId(id),
    enabled: Number.isFinite(id)
  });
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarDetalleConservacion,
  actualizarRangoMilitar,
  actualizarUnidadMilitar,
  bajaLogicaDetalleConservacion,
  bajaLogicaRangoMilitar,
  bajaLogicaUnidadMilitar,
  crearDetalleConservacion,
  crearRangoMilitar,
  crearUnidadMilitar,
  listarDetallesConservacion,
  listarRangosMilitaresAdmin,
  listarUnidadesMilitaresAdmin
} from "./api";
import type { DetalleConservacionRequestDTO, RangoMilitarRequestDTO, UnidadMilitarRequestDTO } from "./types";

export const tablasAuxiliaresQueryKeys = {
  all: ["tablas-auxiliares"] as const,
  rangos: () => [...tablasAuxiliaresQueryKeys.all, "rangos"] as const,
  unidades: () => [...tablasAuxiliaresQueryKeys.all, "unidades"] as const,
  detallesConservacion: () => [...tablasAuxiliaresQueryKeys.all, "detalles-conservacion"] as const
};

export function useRangosMilitaresAdminQuery() {
  return useQuery({ queryKey: tablasAuxiliaresQueryKeys.rangos(), queryFn: listarRangosMilitaresAdmin });
}

export function useUnidadesMilitaresAdminQuery() {
  return useQuery({ queryKey: tablasAuxiliaresQueryKeys.unidades(), queryFn: listarUnidadesMilitaresAdmin });
}

export function useDetallesConservacionQuery() {
  return useQuery({ queryKey: tablasAuxiliaresQueryKeys.detallesConservacion(), queryFn: listarDetallesConservacion });
}

export function useCrearRangoMilitarMutation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: crearRangoMilitar, onSuccess: () => void queryClient.invalidateQueries({ queryKey: tablasAuxiliaresQueryKeys.rangos() }) });
}

export function useActualizarRangoMilitarMutation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ id, payload }: { id: number; payload: RangoMilitarRequestDTO }) => actualizarRangoMilitar(id, payload), onSuccess: () => void queryClient.invalidateQueries({ queryKey: tablasAuxiliaresQueryKeys.rangos() }) });
}

export function useBajaLogicaRangoMilitarMutation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: bajaLogicaRangoMilitar, onSuccess: () => void queryClient.invalidateQueries({ queryKey: tablasAuxiliaresQueryKeys.rangos() }) });
}

export function useCrearUnidadMilitarMutation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: crearUnidadMilitar, onSuccess: () => void queryClient.invalidateQueries({ queryKey: tablasAuxiliaresQueryKeys.unidades() }) });
}

export function useActualizarUnidadMilitarMutation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ id, payload }: { id: number; payload: UnidadMilitarRequestDTO }) => actualizarUnidadMilitar(id, payload), onSuccess: () => void queryClient.invalidateQueries({ queryKey: tablasAuxiliaresQueryKeys.unidades() }) });
}

export function useBajaLogicaUnidadMilitarMutation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: bajaLogicaUnidadMilitar, onSuccess: () => void queryClient.invalidateQueries({ queryKey: tablasAuxiliaresQueryKeys.unidades() }) });
}

export function useCrearDetalleConservacionMutation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: crearDetalleConservacion, onSuccess: () => void queryClient.invalidateQueries({ queryKey: tablasAuxiliaresQueryKeys.detallesConservacion() }) });
}

export function useActualizarDetalleConservacionMutation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ id, payload }: { id: number; payload: DetalleConservacionRequestDTO }) => actualizarDetalleConservacion(id, payload), onSuccess: () => void queryClient.invalidateQueries({ queryKey: tablasAuxiliaresQueryKeys.detallesConservacion() }) });
}

export function useBajaLogicaDetalleConservacionMutation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: bajaLogicaDetalleConservacion, onSuccess: () => void queryClient.invalidateQueries({ queryKey: tablasAuxiliaresQueryKeys.detallesConservacion() }) });
}

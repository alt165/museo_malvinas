import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  actualizarCategoria,
  bajaLogicaCategoria,
  crearCategoria,
  listarCategorias,
  obtenerCategoriaPorId
} from "./api";
import type { CategoriaObjetoRequestDTO } from "./types";

export const categoriasQueryKeys = {
  all: ["categorias"] as const,
  lists: () => [...categoriasQueryKeys.all, "list"] as const,
  detail: (id: number) => [...categoriasQueryKeys.all, "detail", id] as const
};

export function useCategoriasQuery() {
  return useQuery({
    queryKey: categoriasQueryKeys.lists(),
    queryFn: listarCategorias
  });
}

export function useCategoriaQuery(id: number) {
  return useQuery({
    queryKey: categoriasQueryKeys.detail(id),
    queryFn: () => obtenerCategoriaPorId(id),
    enabled: Number.isFinite(id)
  });
}

export function useCrearCategoriaMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: crearCategoria,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: categoriasQueryKeys.all });
    }
  });
}

export function useActualizarCategoriaMutation(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CategoriaObjetoRequestDTO) => actualizarCategoria(id, payload),
    onSuccess: (categoria) => {
      queryClient.setQueryData(categoriasQueryKeys.detail(id), categoria);
      void queryClient.invalidateQueries({ queryKey: categoriasQueryKeys.lists() });
    }
  });
}

export function useBajaLogicaCategoriaMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: bajaLogicaCategoria,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: categoriasQueryKeys.all });
    }
  });
}

import { useQuery } from "@tanstack/react-query";
import { listarUbicaciones } from "./api";

export const ubicacionesQueryKeys = {
  all: ["ubicaciones"] as const,
  lists: () => [...ubicacionesQueryKeys.all, "list"] as const
};

export function useUbicacionesQuery() {
  return useQuery({
    queryKey: ubicacionesQueryKeys.lists(),
    queryFn: listarUbicaciones
  });
}

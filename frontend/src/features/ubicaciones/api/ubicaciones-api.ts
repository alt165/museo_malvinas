import { apiRequest } from "@/lib/api";
import type { UbicacionResponseDTO } from "../types";

export function listarUbicaciones() {
  return apiRequest<UbicacionResponseDTO[]>("/api/ubicaciones");
}

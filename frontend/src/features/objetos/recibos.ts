import { descargarReciboPdf } from "./api";
import type { ReciboIngresoObjetoResponseDTO } from "./types";

function descargarBlob(blob: Blob, nombre: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = nombre;
  link.click();
  URL.revokeObjectURL(url);
}

export function nombreArchivoRecibo(recibo: Pick<ReciboIngresoObjetoResponseDTO, "id">) {
  return `recibo-${recibo.id}.pdf`;
}

export async function descargarReciboIngresoPdf(recibo: Pick<ReciboIngresoObjetoResponseDTO, "id">) {
  const blob = await descargarReciboPdf(recibo.id);
  descargarBlob(blob, nombreArchivoRecibo(recibo));
}

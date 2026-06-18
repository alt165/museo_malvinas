"use client";

import { useState, type FormEvent } from "react";
import { useUbicacionesQuery } from "@/features/ubicaciones/queries";
import { useMoverObjetoMutation } from "../queries";
import type { ObjetoMuseoResponseDTO } from "../types";
import { getApiErrorMessage } from "../utils";

type MoverObjetoModalProps = {
  objeto: ObjetoMuseoResponseDTO;
  onClose: () => void;
};

export function MoverObjetoModal({ objeto, onClose }: MoverObjetoModalProps) {
  const ubicacionesQuery = useUbicacionesQuery();
  const mutation = useMoverObjetoMutation(objeto.id);
  const [ubicacionDestinoId, setUbicacionDestinoId] = useState(0);
  const [descripcion, setDescripcion] = useState("");
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSuccessMessage(null);
    if (!ubicacionDestinoId) {
      return;
    }
    mutation.mutate(
      {
        ubicacionDestinoId,
        descripcion: descripcion.trim() || null
      },
      {
        onSuccess: () => {
          setSuccessMessage("Objeto movido correctamente.");
          window.setTimeout(onClose, 700);
        }
      }
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-lg border bg-background p-5 shadow-lg">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-primary">Mover objeto</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {objeto.numeroInventario} - {objeto.denominacionObjeto}
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              Ubicacion actual: {objeto.ubicacionNombre || "Sin ubicacion registrada"}
            </p>
          </div>
          <button className="rounded-md border px-2 py-1 text-sm hover:bg-muted" onClick={onClose} type="button">
            Cerrar
          </button>
        </div>
        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor="ubicacionDestinoId">Ubicacion destino</label>
            <select
              className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
              disabled={ubicacionesQuery.isLoading || mutation.isPending}
              id="ubicacionDestinoId"
              onChange={(event) => setUbicacionDestinoId(Number(event.target.value))}
              value={ubicacionDestinoId}
            >
              <option value={0}>Seleccionar ubicacion</option>
              {(ubicacionesQuery.data ?? []).map((ubicacion) => (
                <option key={ubicacion.id} value={ubicacion.id}>
                  {ubicacion.nombre}
                </option>
              ))}
            </select>
            {!ubicacionDestinoId ? <p className="text-xs text-muted-foreground">La ubicacion destino es obligatoria.</p> : null}
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor="descripcionMovimiento">Descripcion</label>
            <textarea
              className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
              id="descripcionMovimiento"
              onChange={(event) => setDescripcion(event.target.value)}
              value={descripcion}
            />
          </div>
          {ubicacionesQuery.isError ? <p className="text-sm text-destructive">No se pudieron cargar las ubicaciones.</p> : null}
          {mutation.isError ? <p className="text-sm text-destructive">{getApiErrorMessage(mutation.error)}</p> : null}
          {successMessage ? <p className="text-sm text-green-700">{successMessage}</p> : null}
          <div className="flex justify-end gap-2">
            <button className="rounded-md border px-4 py-2 text-sm hover:bg-muted" disabled={mutation.isPending} onClick={onClose} type="button">
              Cancelar
            </button>
            <button className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60" disabled={!ubicacionDestinoId || mutation.isPending} type="submit">
              {mutation.isPending ? "Moviendo..." : "Mover"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

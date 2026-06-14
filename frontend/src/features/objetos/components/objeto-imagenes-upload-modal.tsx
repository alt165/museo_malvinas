"use client";

/* eslint-disable @next/next/no-img-element */

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Trash2, Upload, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { subirFotosObjeto } from "@/features/objetos/api";
import { objetosQueryKeys } from "@/features/objetos/queries";
import { getApiErrorMessage } from "@/features/objetos/utils";

type ImagenSeleccionada = {
  id: string;
  file: File;
  previewUrl: string;
};

type ObjetoImagenesUploadModalProps = {
  objetoId: number;
  open: boolean;
  onClose: () => void;
  onUploaded: (fotoId?: number) => void;
};

const formatosImagenPermitidos = new Set(["image/jpeg", "image/png", "image/webp"]);
const tamanioMaximoImagenBytes = 10 * 1024 * 1024;

export function ObjetoImagenesUploadModal({ objetoId, open, onClose, onUploaded }: ObjetoImagenesUploadModalProps) {
  const queryClient = useQueryClient();
  const [imagenes, setImagenes] = useState<ImagenSeleccionada[]>([]);
  const [error, setError] = useState<string | null>(null);

  const limpiarImagenes = useCallback(() => {
    setImagenes((actuales) => {
      actuales.forEach((imagen) => URL.revokeObjectURL(imagen.previewUrl));
      return [];
    });
  }, []);

  const subirFotosMutation = useMutation({
    mutationFn: (archivos: File[]) => subirFotosObjeto(objetoId, archivos),
    onSuccess: (fotosCargadas) => {
      limpiarImagenes();
      setError(null);
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.fotos(objetoId) });
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.detail(objetoId) });
      onUploaded(fotosCargadas[0]?.id);
      onClose();
    },
    onError: (mutationError) => {
      setError(getApiErrorMessage(mutationError));
    }
  });

  const cerrarModal = useCallback(() => {
    if (subirFotosMutation.isPending) {
      return;
    }
    limpiarImagenes();
    setError(null);
    onClose();
  }, [limpiarImagenes, onClose, subirFotosMutation.isPending]);

  useEffect(() => {
    if (!open) {
      return undefined;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        cerrarModal();
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [cerrarModal, open]);

  if (!open) {
    return null;
  }

  function agregarArchivos(files: FileList | null) {
    if (!files?.length) {
      return;
    }

    const nuevasImagenes: ImagenSeleccionada[] = [];
    const errores: string[] = [];

    Array.from(files).forEach((file) => {
      if (!formatosImagenPermitidos.has(file.type)) {
        errores.push(`${file.name}: formato no permitido.`);
        return;
      }
      if (file.size > tamanioMaximoImagenBytes) {
        errores.push(`${file.name}: supera el tamano maximo de 10 MB.`);
        return;
      }
      nuevasImagenes.push({
        id: `${file.name}-${file.size}-${file.lastModified}-${Math.random()}`,
        file,
        previewUrl: URL.createObjectURL(file)
      });
    });

    if (nuevasImagenes.length > 0) {
      setImagenes((actuales) => [...actuales, ...nuevasImagenes]);
    }
    setError(errores.length > 0 ? errores.join(" ") : null);
  }

  function quitarImagen(id: string) {
    setImagenes((actuales) => {
      const imagen = actuales.find((item) => item.id === id);
      if (imagen) {
        URL.revokeObjectURL(imagen.previewUrl);
      }
      return actuales.filter((item) => item.id !== id);
    });
  }

  return (
    <div
      aria-label="Carga de imagenes del objeto"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={cerrarModal}
      role="dialog"
    >
      <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-lg border bg-background p-5 shadow-lg" onClick={(event) => event.stopPropagation()}>
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold">Agregar imagenes</h2>
            <p className="mt-1 text-sm text-muted-foreground">Selecciona una o varias imagenes para cargar en este objeto.</p>
          </div>
          <button
            aria-label="Cerrar carga de imagenes"
            className="rounded-full border p-2 hover:bg-muted disabled:opacity-60"
            disabled={subirFotosMutation.isPending}
            onClick={cerrarModal}
            type="button"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </button>
        </div>

        <label className="mt-5 flex min-h-36 cursor-pointer flex-col items-center justify-center gap-3 rounded-lg border border-dashed bg-muted/30 p-6 text-center hover:bg-muted/50">
          <Upload className="h-8 w-8 text-muted-foreground" aria-hidden="true" />
          <span className="text-sm font-medium">Seleccionar imagenes</span>
          <span className="text-xs text-muted-foreground">JPG, JPEG, PNG o WEBP. Maximo 10 MB por imagen.</span>
          <input
            accept="image/jpeg,image/png,image/webp"
            className="sr-only"
            disabled={subirFotosMutation.isPending}
            multiple
            onChange={(event) => {
              agregarArchivos(event.target.files);
              event.currentTarget.value = "";
            }}
            type="file"
          />
        </label>

        {imagenes.length > 0 ? (
          <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {imagenes.map((imagen) => (
              <div className="overflow-hidden rounded-md border bg-background" key={imagen.id}>
                <img alt={imagen.file.name} className="h-36 w-full bg-muted object-cover" src={imagen.previewUrl} />
                <div className="flex items-center justify-between gap-2 p-3 text-sm">
                  <p className="truncate font-medium" title={imagen.file.name}>{imagen.file.name}</p>
                  <button
                    aria-label={`Quitar ${imagen.file.name}`}
                    className="rounded-md border p-1.5 text-destructive hover:bg-destructive/10 disabled:opacity-60"
                    disabled={subirFotosMutation.isPending}
                    onClick={() => quitarImagen(imagen.id)}
                    type="button"
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : null}

        {error ? <p className="mt-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p> : null}

        <div className="mt-5 flex flex-wrap justify-end gap-2">
          <button
            className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-60"
            disabled={subirFotosMutation.isPending}
            onClick={cerrarModal}
            type="button"
          >
            Cancelar
          </button>
          <button
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
            disabled={imagenes.length === 0 || subirFotosMutation.isPending}
            onClick={() => subirFotosMutation.mutate(imagenes.map((imagen) => imagen.file))}
            type="button"
          >
            {subirFotosMutation.isPending ? "Cargando..." : "Cargar imagenes"}
          </button>
        </div>
      </div>
    </div>
  );
}

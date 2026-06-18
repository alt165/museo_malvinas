"use client";

/* eslint-disable @next/next/no-img-element */

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import {
  descargarFotoObjeto,
  descargarReciboEscaneadoObjeto,
  eliminarReciboEscaneadoObjeto,
  subirFotoObjeto,
  subirReciboEscaneadoObjeto
} from "@/features/objetos/api";
import {
  objetosQueryKeys,
  useEliminarFotoObjetoMutation,
  useFotosObjetoQuery
} from "@/features/objetos/queries";
import type { ObjetoMuseoResponseDTO } from "@/features/objetos/types";

type ObjetoArchivosPanelProps = {
  mode: "view" | "edit";
  objeto: ObjetoMuseoResponseDTO;
};

function descargarBlob(blob: Blob, nombre: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = nombre;
  link.click();
  URL.revokeObjectURL(url);
}

async function abrirBlobEnNuevaVentana(blob: Blob) {
  const url = URL.createObjectURL(blob);
  const nuevaVentana = window.open(url, "_blank", "noopener,noreferrer");
  if (!nuevaVentana) {
    URL.revokeObjectURL(url);
    return;
  }
  nuevaVentana.addEventListener("beforeunload", () => URL.revokeObjectURL(url), { once: true });
}

export function ObjetoArchivosPanel({ mode, objeto }: ObjetoArchivosPanelProps) {
  const editable = mode === "edit";
  const queryClient = useQueryClient();
  const [fotoDescripcion, setFotoDescripcion] = useState("");
  const [fotoThumbUrls, setFotoThumbUrls] = useState<Record<number, string>>({});
  const fotoThumbUrlsRef = useRef<Record<number, string>>({});
  const { data: fotos = [] } = useFotosObjetoQuery(objeto.id);
  const eliminarFotoMutation = useEliminarFotoObjetoMutation(objeto.id);
  const subirFotoMutation = useMutation({
    mutationFn: (archivo: File) => subirFotoObjeto(objeto.id, archivo, fotoDescripcion.trim() || undefined),
    onSuccess: () => {
      setFotoDescripcion("");
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.fotos(objeto.id) });
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.detail(objeto.id) });
    }
  });
  const subirReciboEscaneadoMutation = useMutation({
    mutationFn: (archivo: File) => subirReciboEscaneadoObjeto(objeto.id, archivo),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.detail(objeto.id) });
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.reciboEscaneado(objeto.id) });
    }
  });
  const eliminarReciboEscaneadoMutation = useMutation({
    mutationFn: (archivoId: number) => eliminarReciboEscaneadoObjeto(objeto.id, archivoId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.detail(objeto.id) });
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.reciboEscaneado(objeto.id) });
    }
  });

  useEffect(() => {
    let activo = true;
    const urlsNuevas: string[] = [];

    async function cargarMiniaturas() {
      const entries = await Promise.all(
        fotos.map(async (foto) => {
          const blob = await descargarFotoObjeto(objeto.id, foto.id);
          const url = URL.createObjectURL(blob);
          urlsNuevas.push(url);
          return [foto.id, url] as const;
        })
      );
      if (activo) {
        const nextUrls = Object.fromEntries(entries);
        Object.values(fotoThumbUrlsRef.current).forEach((url) => URL.revokeObjectURL(url));
        fotoThumbUrlsRef.current = nextUrls;
        setFotoThumbUrls(nextUrls);
      } else {
        urlsNuevas.forEach((url) => URL.revokeObjectURL(url));
      }
    }

    if (fotos.length > 0) {
      void cargarMiniaturas();
    } else {
      Promise.resolve().then(() => {
        if (activo) {
          Object.values(fotoThumbUrlsRef.current).forEach((url) => URL.revokeObjectURL(url));
          fotoThumbUrlsRef.current = {};
          setFotoThumbUrls({});
        }
      });
    }

    return () => {
      activo = false;
      urlsNuevas.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [fotos, objeto.id]);

  return (
    <div className="space-y-6">
      <section className="rounded-lg border p-5">
        <h2 className="text-base font-semibold">Fotos</h2>
        {editable ? (
          <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_2fr_auto]">
            <input
              accept="image/jpeg,image/png,image/webp"
              capture="environment"
              className="h-10 rounded-md border bg-background px-3 py-2 text-sm"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) {
                  subirFotoMutation.mutate(file);
                }
                event.currentTarget.value = "";
              }}
              type="file"
            />
            <input
              className="h-10 rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
              onChange={(event) => setFotoDescripcion(event.target.value)}
              placeholder="Descripcion opcional"
              value={fotoDescripcion}
            />
            <span className="self-center text-sm text-muted-foreground">{subirFotoMutation.isPending ? "Subiendo..." : ""}</span>
          </div>
        ) : null}
        <div className="mt-4">
          {fotos.length === 0 ? <p className="text-sm text-muted-foreground">Sin fotos adjuntas.</p> : null}
          {fotos.length > 0 ? (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {fotos.map((foto) => (
                <div className="overflow-hidden rounded-md border bg-background text-sm" key={foto.id}>
                  <button
                    className="block h-40 w-full bg-muted"
                    onClick={async () => abrirBlobEnNuevaVentana(await descargarFotoObjeto(objeto.id, foto.id))}
                    title="Abrir imagen completa"
                    type="button"
                  >
                    {fotoThumbUrls[foto.id] ? (
                      <img alt={foto.nombreArchivo} className="h-full w-full object-cover" src={fotoThumbUrls[foto.id]} />
                    ) : (
                      <span className="flex h-full items-center justify-center text-sm text-muted-foreground">Cargando imagen...</span>
                    )}
                  </button>
                  <div className="space-y-2 p-3">
                    <div>
                      <p className="truncate font-medium">{foto.nombreArchivo}</p>
                      <p className="truncate text-muted-foreground">{foto.descripcion || foto.contentType}</p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <button className="rounded-md border px-3 py-1.5 hover:bg-muted" onClick={async () => abrirBlobEnNuevaVentana(await descargarFotoObjeto(objeto.id, foto.id))} type="button">
                        Ver foto
                      </button>
                      <button className="rounded-md border px-3 py-1.5 hover:bg-muted" onClick={async () => descargarBlob(await descargarFotoObjeto(objeto.id, foto.id), foto.nombreArchivo)} type="button">
                        Descargar
                      </button>
                      {editable ? (
                        <button className="rounded-md border px-3 py-1.5 text-destructive hover:bg-destructive/10" onClick={() => eliminarFotoMutation.mutate(foto.id)} type="button">
                          Eliminar
                        </button>
                      ) : null}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      </section>

      <section className="rounded-lg border p-5">
        <h2 className="text-base font-semibold">Recibo escaneado</h2>
        <div className="mt-4 grid gap-3">
          {objeto.reciboEscaneado ? (
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-md border p-3 text-sm">
              <div>
                <p className="font-medium">{objeto.reciboEscaneado.nombreArchivoOriginal}</p>
                <p className="text-muted-foreground">{objeto.reciboEscaneado.contentType}</p>
              </div>
              <div className="flex gap-2">
                <button className="rounded-md border px-3 py-1.5 hover:bg-muted" onClick={async () => descargarBlob(await descargarReciboEscaneadoObjeto(objeto.id), objeto.reciboEscaneado?.nombreArchivoOriginal || `recibo-escaneado-${objeto.id}`)} type="button">
                  Descargar recibo
                </button>
                {editable ? (
                  <button className="rounded-md border px-3 py-1.5 text-destructive hover:bg-destructive/10" onClick={() => objeto.reciboEscaneado ? eliminarReciboEscaneadoMutation.mutate(objeto.reciboEscaneado.id) : undefined} type="button">
                    Quitar
                  </button>
                ) : null}
              </div>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">Sin recibo escaneado adjunto.</p>
          )}
          {editable ? (
            <input
              accept="application/pdf,image/jpeg,image/png,image/webp"
              className="h-10 rounded-md border bg-background px-3 py-2 text-sm"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) {
                  subirReciboEscaneadoMutation.mutate(file);
                }
                event.currentTarget.value = "";
              }}
              type="file"
            />
          ) : null}
        </div>
      </section>
    </div>
  );
}

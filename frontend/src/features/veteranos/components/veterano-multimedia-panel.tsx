"use client";

/* eslint-disable @next/next/no-img-element */

import { Camera, ChevronLeft, ChevronRight, Trash2, Upload, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { descargarImagenVeterano } from "@/features/veteranos/api";
import {
  useActualizarVideoVeteranoMutation,
  useCrearVideoVeteranoMutation,
  useEliminarImagenVeteranoMutation,
  useEliminarVideoVeteranoMutation,
  useImagenesVeteranoQuery,
  useSubirImagenesVeteranoMutation,
  useVideosVeteranoQuery
} from "@/features/veteranos/queries";
import type { VeteranoImagenResponseDTO, VeteranoVideoRequestDTO, VeteranoVideoResponseDTO } from "@/features/veteranos/types";
import { formatDate, getApiErrorMessage } from "@/features/veteranos/utils";
import { ApiClientError } from "@/lib/errors/api-error";

type VeteranoMultimediaPanelProps = {
  canWrite: boolean;
  veteranoId: number;
};

type ImagenSeleccionada = {
  id: string;
  file: File;
  previewUrl: string;
};

type VideoFormState = {
  titulo: string;
  urlYoutube: string;
  descripcion: string;
  fechaEntrevista: string;
};

const formatosImagenPermitidos = new Set(["image/jpeg", "image/png", "image/webp"]);
const tamanioMaximoImagenBytes = 10 * 1024 * 1024;

function ordenarImagenes(imagenes: VeteranoImagenResponseDTO[]) {
  return [...imagenes].sort((a, b) => a.orden - b.orden || new Date(a.fechaCarga).getTime() - new Date(b.fechaCarga).getTime() || a.id - b.id);
}

function youtubeUrlValida(url: string) {
  try {
    const parsed = new URL(url);
    const host = parsed.hostname.toLowerCase().replace(/^www\./, "");
    if (host === "youtu.be") {
      return parsed.pathname.slice(1).length > 0;
    }
    if (host === "youtube.com" || host === "m.youtube.com") {
      return Boolean(parsed.searchParams.get("v")) || parsed.pathname.startsWith("/embed/");
    }
    return false;
  } catch {
    return false;
  }
}

export function VeteranoMultimediaPanel({ canWrite, veteranoId }: VeteranoMultimediaPanelProps) {
  return (
    <div className="space-y-8">
      <VeteranoFotosPanel canWrite={canWrite} veteranoId={veteranoId} />
      <VeteranoVideosPanel canWrite={canWrite} veteranoId={veteranoId} />
    </div>
  );
}

function VeteranoFotosPanel({ canWrite, veteranoId }: VeteranoMultimediaPanelProps) {
  const imagenesQuery = useImagenesVeteranoQuery(veteranoId);
  const eliminarImagen = useEliminarImagenVeteranoMutation(veteranoId);
  const imagenes = useMemo(() => ordenarImagenes(imagenesQuery.data ?? []), [imagenesQuery.data]);
  const [imagenPrincipalId, setImagenPrincipalId] = useState<number | null>(null);
  const [modalCargaAbierto, setModalCargaAbierto] = useState(false);
  const [visorAbierto, setVisorAbierto] = useState(false);
  const [visorImagenId, setVisorImagenId] = useState<number | null>(null);
  const [imagenUrls, setImagenUrls] = useState<Record<number, string>>({});
  const imagenUrlsRef = useRef<Record<number, string>>({});

  useEffect(() => {
    let activo = true;
    const urlsNuevas: string[] = [];

    async function cargarImagenes() {
      try {
        const entries = await Promise.all(imagenes.map(async (imagen) => {
          const blob = await descargarImagenVeterano(veteranoId, imagen.id);
          const url = URL.createObjectURL(blob);
          urlsNuevas.push(url);
          return [imagen.id, url] as const;
        }));
        if (activo) {
          const nextUrls = Object.fromEntries(entries);
          Object.values(imagenUrlsRef.current).forEach((url) => URL.revokeObjectURL(url));
          imagenUrlsRef.current = nextUrls;
          setImagenUrls(nextUrls);
        } else {
          urlsNuevas.forEach((url) => URL.revokeObjectURL(url));
        }
      } catch {
        if (activo) {
          Object.values(imagenUrlsRef.current).forEach((url) => URL.revokeObjectURL(url));
          imagenUrlsRef.current = {};
          setImagenUrls({});
        }
      }
    }

    if (imagenes.length > 0) {
      void cargarImagenes();
    } else {
      Promise.resolve().then(() => {
        if (activo) {
          Object.values(imagenUrlsRef.current).forEach((url) => URL.revokeObjectURL(url));
          imagenUrlsRef.current = {};
          setImagenUrls({});
        }
      });
    }

    return () => {
      activo = false;
      urlsNuevas.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [imagenes, veteranoId]);

  const imagenPrincipal = imagenes.find((imagen) => imagen.id === imagenPrincipalId) ?? imagenes[0];
  const imagenesCarrusel = imagenPrincipal ? imagenes.filter((imagen) => imagen.id !== imagenPrincipal.id) : [];
  const visorImagenActual = imagenes.find((imagen) => imagen.id === visorImagenId) ?? imagenPrincipal;
  const visorIndex = visorImagenActual ? imagenes.findIndex((imagen) => imagen.id === visorImagenActual.id) : -1;
  const hayVariasImagenes = imagenes.length > 1;

  const irAImagenAnterior = useCallback(() => {
    if (!hayVariasImagenes || visorIndex < 0) {
      return;
    }
    const nextIndex = (visorIndex - 1 + imagenes.length) % imagenes.length;
    setVisorImagenId(imagenes[nextIndex].id);
  }, [hayVariasImagenes, imagenes, visorIndex]);

  const irAImagenSiguiente = useCallback(() => {
    if (!hayVariasImagenes || visorIndex < 0) {
      return;
    }
    const nextIndex = (visorIndex + 1) % imagenes.length;
    setVisorImagenId(imagenes[nextIndex].id);
  }, [hayVariasImagenes, imagenes, visorIndex]);

  useEffect(() => {
    if (!visorAbierto) {
      return undefined;
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setVisorAbierto(false);
      }
      if (event.key === "ArrowLeft") {
        irAImagenAnterior();
      }
      if (event.key === "ArrowRight") {
        irAImagenSiguiente();
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [irAImagenAnterior, irAImagenSiguiente, visorAbierto]);

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Fotos</h2>
        {canWrite ? (
          <button className="rounded-md border px-3 py-1.5 text-sm font-medium hover:bg-muted" onClick={() => setModalCargaAbierto(true)} type="button">
            {imagenes.length > 0 ? "Agregar mas fotos" : "Agregar fotos"}
          </button>
        ) : null}
      </div>

      {imagenesQuery.isLoading ? <LoadingState /> : null}
      {imagenesQuery.isError ? <ErrorState message={getApiErrorMessage(imagenesQuery.error)} requestId={imagenesQuery.error instanceof ApiClientError ? imagenesQuery.error.requestId : undefined} /> : null}
      {eliminarImagen.isError ? <ErrorState message={getApiErrorMessage(eliminarImagen.error)} requestId={eliminarImagen.error instanceof ApiClientError ? eliminarImagen.error.requestId : undefined} /> : null}

      {!imagenesQuery.isLoading && imagenes.length === 0 ? (
        <div className="flex min-h-64 flex-col items-center justify-center gap-3 rounded-lg border bg-muted/30 p-6 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full border bg-background text-muted-foreground">
            <Camera className="h-8 w-8" aria-hidden="true" />
          </div>
          <p className="font-medium">Sin fotos registradas</p>
        </div>
      ) : null}

      {imagenPrincipal ? (
        <div className="space-y-4">
          <button
            aria-label="Abrir foto principal en visor ampliado"
            className="block h-[320px] w-full overflow-hidden rounded-lg border bg-muted/30 focus:outline-none focus:ring-2 focus:ring-ring sm:h-[420px]"
            onClick={() => {
              setVisorImagenId(imagenPrincipal.id);
              setVisorAbierto(true);
            }}
            type="button"
          >
            {imagenUrls[imagenPrincipal.id] ? (
              <img alt={imagenPrincipal.descripcion || imagenPrincipal.nombreArchivo} className="h-full w-full object-contain" src={imagenUrls[imagenPrincipal.id]} />
            ) : (
              <span className="flex h-full items-center justify-center text-sm text-muted-foreground">Cargando imagen...</span>
            )}
          </button>

          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-muted-foreground">Imagenes del veterano</p>
            {canWrite ? (
              <button
                className="rounded-md border px-3 py-1.5 text-sm text-destructive hover:bg-destructive/10 disabled:opacity-60"
                disabled={eliminarImagen.isPending}
                onClick={() => {
                  if (window.confirm("Eliminar foto del veterano?")) {
                    eliminarImagen.mutate(imagenPrincipal.id);
                  }
                }}
                type="button"
              >
                Eliminar foto principal
              </button>
            ) : null}
          </div>

          {hayVariasImagenes ? (
            <div className="flex gap-3 overflow-x-auto pb-1">
              {imagenesCarrusel.map((imagen) => (
                <button
                  aria-label={`Usar ${imagen.nombreArchivo} como foto principal`}
                  className="h-24 w-32 shrink-0 overflow-hidden rounded-md border bg-muted/30 focus:outline-none focus:ring-2 focus:ring-ring"
                  key={imagen.id}
                  onClick={() => setImagenPrincipalId(imagen.id)}
                  type="button"
                >
                  {imagenUrls[imagen.id] ? <img alt={imagen.descripcion || imagen.nombreArchivo} className="h-full w-full object-cover" src={imagenUrls[imagen.id]} /> : null}
                </button>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}

      <VeteranoImagenesUploadModal
        onClose={() => setModalCargaAbierto(false)}
        onUploaded={(imagenId) => {
          if (imagenId) {
            setImagenPrincipalId(imagenId);
          }
        }}
        open={modalCargaAbierto}
        veteranoId={veteranoId}
      />

      {visorAbierto && visorImagenActual ? (
        <div aria-label="Visor de foto del veterano" aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4" onClick={() => setVisorAbierto(false)} role="dialog">
          <button aria-label="Cerrar visor" className="absolute right-4 top-4 rounded-full border border-white/30 bg-black/40 p-2 text-white hover:bg-black/70" onClick={() => setVisorAbierto(false)} type="button">
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
          {hayVariasImagenes ? <button aria-label="Foto anterior" className="absolute left-3 top-1/2 hidden -translate-y-1/2 rounded-full border border-white/30 bg-black/40 p-3 text-white hover:bg-black/70 sm:block" onClick={(event) => { event.stopPropagation(); irAImagenAnterior(); }} type="button"><ChevronLeft className="h-7 w-7" aria-hidden="true" /></button> : null}
          <div className="flex max-h-[92vh] w-full max-w-6xl flex-col gap-4" onClick={(event) => event.stopPropagation()}>
            <div className="flex min-h-0 flex-1 items-center justify-center">
              {imagenUrls[visorImagenActual.id] ? <img alt={visorImagenActual.descripcion || visorImagenActual.nombreArchivo} className="max-h-[78vh] max-w-full object-contain" src={imagenUrls[visorImagenActual.id]} /> : <span className="text-sm text-white">Cargando imagen...</span>}
            </div>
          </div>
          {hayVariasImagenes ? <button aria-label="Foto siguiente" className="absolute right-3 top-1/2 hidden -translate-y-1/2 rounded-full border border-white/30 bg-black/40 p-3 text-white hover:bg-black/70 sm:block" onClick={(event) => { event.stopPropagation(); irAImagenSiguiente(); }} type="button"><ChevronRight className="h-7 w-7" aria-hidden="true" /></button> : null}
        </div>
      ) : null}
    </section>
  );
}

function VeteranoImagenesUploadModal({ onClose, onUploaded, open, veteranoId }: { veteranoId: number; open: boolean; onClose: () => void; onUploaded: (imagenId?: number) => void }) {
  const [imagenes, setImagenes] = useState<ImagenSeleccionada[]>([]);
  const [error, setError] = useState<string | null>(null);
  const subirImagenes = useSubirImagenesVeteranoMutation(veteranoId);

  const limpiarImagenes = useCallback(() => {
    setImagenes((actuales) => {
      actuales.forEach((imagen) => URL.revokeObjectURL(imagen.previewUrl));
      return [];
    });
  }, []);

  const cerrar = useCallback(() => {
    if (subirImagenes.isPending) {
      return;
    }
    limpiarImagenes();
    setError(null);
    onClose();
  }, [limpiarImagenes, onClose, subirImagenes.isPending]);

  useEffect(() => {
    if (!open) {
      return undefined;
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        cerrar();
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [cerrar, open]);

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
      nuevasImagenes.push({ id: `${file.name}-${file.size}-${file.lastModified}-${Math.random()}`, file, previewUrl: URL.createObjectURL(file) });
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
    <div aria-label="Carga de fotos del veterano" aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={cerrar} role="dialog">
      <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-lg border bg-background p-5 shadow-lg" onClick={(event) => event.stopPropagation()}>
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold">Agregar fotos</h2>
            <p className="mt-1 text-sm text-muted-foreground">Selecciona una o varias imagenes del veterano.</p>
          </div>
          <button aria-label="Cerrar carga de fotos" className="rounded-full border p-2 hover:bg-muted disabled:opacity-60" disabled={subirImagenes.isPending} onClick={cerrar} type="button"><X className="h-4 w-4" aria-hidden="true" /></button>
        </div>
        <label className="mt-5 flex min-h-36 cursor-pointer flex-col items-center justify-center gap-3 rounded-lg border border-dashed bg-muted/30 p-6 text-center hover:bg-muted/50">
          <Upload className="h-8 w-8 text-muted-foreground" aria-hidden="true" />
          <span className="text-sm font-medium">Seleccionar fotos</span>
          <span className="text-xs text-muted-foreground">JPG, JPEG, PNG o WEBP. Maximo 10 MB por imagen.</span>
          <input accept="image/jpeg,image/png,image/webp" className="sr-only" disabled={subirImagenes.isPending} multiple onChange={(event) => { agregarArchivos(event.target.files); event.currentTarget.value = ""; }} type="file" />
        </label>
        {imagenes.length > 0 ? <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{imagenes.map((imagen) => <div className="overflow-hidden rounded-md border bg-background" key={imagen.id}><img alt={imagen.file.name} className="h-36 w-full bg-muted object-cover" src={imagen.previewUrl} /><div className="flex items-center justify-between gap-2 p-3 text-sm"><p className="truncate font-medium" title={imagen.file.name}>{imagen.file.name}</p><button aria-label={`Quitar ${imagen.file.name}`} className="rounded-md border p-1.5 text-destructive hover:bg-destructive/10 disabled:opacity-60" disabled={subirImagenes.isPending} onClick={() => quitarImagen(imagen.id)} type="button"><Trash2 className="h-4 w-4" aria-hidden="true" /></button></div></div>)}</div> : null}
        {error ? <p className="mt-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p> : null}
        {subirImagenes.isError ? <p className="mt-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{getApiErrorMessage(subirImagenes.error)}</p> : null}
        <div className="mt-5 flex flex-wrap justify-end gap-2">
          <button className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-60" disabled={subirImagenes.isPending} onClick={cerrar} type="button">Cancelar</button>
          <button className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60" disabled={imagenes.length === 0 || subirImagenes.isPending} onClick={() => subirImagenes.mutate({ archivos: imagenes.map((imagen) => imagen.file) }, { onSuccess: (cargadas) => { limpiarImagenes(); setError(null); onUploaded(cargadas[0]?.id); onClose(); } })} type="button">{subirImagenes.isPending ? "Cargando..." : "Cargar fotos"}</button>
        </div>
      </div>
    </div>
  );
}

function VeteranoVideosPanel({ canWrite, veteranoId }: VeteranoMultimediaPanelProps) {
  const videosQuery = useVideosVeteranoQuery(veteranoId);
  const crearVideo = useCrearVideoVeteranoMutation(veteranoId);
  const actualizarVideo = useActualizarVideoVeteranoMutation(veteranoId);
  const eliminarVideo = useEliminarVideoVeteranoMutation(veteranoId);
  const [modalVideoAbierto, setModalVideoAbierto] = useState(false);
  const [videoEditando, setVideoEditando] = useState<VeteranoVideoResponseDTO | null>(null);

  function cerrarModal() {
    setModalVideoAbierto(false);
    setVideoEditando(null);
  }

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Videos / Entrevistas</h2>
        {canWrite ? <button className="rounded-md border px-3 py-1.5 text-sm font-medium hover:bg-muted" onClick={() => setModalVideoAbierto(true)} type="button">Agregar video</button> : null}
      </div>
      {videosQuery.isLoading ? <LoadingState /> : null}
      {videosQuery.isError ? <ErrorState message={getApiErrorMessage(videosQuery.error)} requestId={videosQuery.error instanceof ApiClientError ? videosQuery.error.requestId : undefined} /> : null}
      {crearVideo.isError ? <ErrorState message={getApiErrorMessage(crearVideo.error)} requestId={crearVideo.error instanceof ApiClientError ? crearVideo.error.requestId : undefined} /> : null}
      {actualizarVideo.isError ? <ErrorState message={getApiErrorMessage(actualizarVideo.error)} requestId={actualizarVideo.error instanceof ApiClientError ? actualizarVideo.error.requestId : undefined} /> : null}
      {eliminarVideo.isError ? <ErrorState message={getApiErrorMessage(eliminarVideo.error)} requestId={eliminarVideo.error instanceof ApiClientError ? eliminarVideo.error.requestId : undefined} /> : null}
      {!videosQuery.isLoading && videosQuery.data?.length === 0 ? <div className="rounded-lg border p-5 text-sm text-muted-foreground">Sin entrevistas registradas.</div> : null}
      {videosQuery.data && videosQuery.data.length > 0 ? (
        <div className="grid gap-4 lg:grid-cols-2">
          {videosQuery.data.map((video) => (
            <article className="overflow-hidden rounded-lg border bg-background" key={video.id}>
              <div className="aspect-video bg-muted">
                <iframe allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen className="h-full w-full" src={`https://www.youtube.com/embed/${video.videoId}`} title={video.titulo} />
              </div>
              <div className="space-y-2 p-4 text-sm">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="font-semibold">{video.titulo}</h3>
                    {video.fechaEntrevista ? <p className="text-muted-foreground">Entrevista: {formatDate(video.fechaEntrevista)}</p> : null}
                  </div>
                  {canWrite ? <div className="flex gap-2"><button className="rounded-md border px-3 py-1.5 text-xs hover:bg-muted" onClick={() => { setVideoEditando(video); setModalVideoAbierto(true); }} type="button">Editar</button><button className="rounded-md border px-3 py-1.5 text-xs text-destructive hover:bg-destructive/10" disabled={eliminarVideo.isPending} onClick={() => { if (window.confirm("Eliminar video del veterano?")) eliminarVideo.mutate(video.id); }} type="button">Eliminar</button></div> : null}
                </div>
                <p className="whitespace-pre-wrap text-muted-foreground">{video.descripcion || "Sin descripcion"}</p>
              </div>
            </article>
          ))}
        </div>
      ) : null}
      {modalVideoAbierto ? (
        <VeteranoVideoModal
          isPending={crearVideo.isPending || actualizarVideo.isPending}
          onClose={cerrarModal}
          onSubmit={(values) => {
            if (videoEditando) {
              actualizarVideo.mutate({ videoId: videoEditando.id, payload: values }, { onSuccess: cerrarModal });
            } else {
              crearVideo.mutate(values, { onSuccess: cerrarModal });
            }
          }}
          video={videoEditando}
        />
      ) : null}
    </section>
  );
}

function VeteranoVideoModal({ isPending, onClose, onSubmit, video }: { video: VeteranoVideoResponseDTO | null; isPending: boolean; onClose: () => void; onSubmit: (values: VeteranoVideoRequestDTO) => void }) {
  const [form, setForm] = useState<VideoFormState>({ titulo: video?.titulo ?? "", urlYoutube: video?.urlYoutube ?? "", descripcion: video?.descripcion ?? "", fechaEntrevista: video?.fechaEntrevista ?? "" });
  const [error, setError] = useState<string | null>(null);

  function submit() {
    if (!form.titulo.trim()) {
      setError("El titulo es obligatorio.");
      return;
    }
    if (!youtubeUrlValida(form.urlYoutube.trim())) {
      setError("La URL debe ser de YouTube.");
      return;
    }
    setError(null);
    onSubmit({ titulo: form.titulo.trim(), urlYoutube: form.urlYoutube.trim(), descripcion: form.descripcion.trim() || null, fechaEntrevista: form.fechaEntrevista || null });
  }

  return (
    <div aria-label="Formulario de video del veterano" aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={() => !isPending && onClose()} role="dialog">
      <div className="w-full max-w-xl rounded-lg border bg-background p-5 shadow-lg" onClick={(event) => event.stopPropagation()}>
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold">{video ? "Editar video" : "Agregar video"}</h2>
            <p className="mt-1 text-sm text-muted-foreground">Registra un enlace de YouTube sin descargar el video.</p>
          </div>
          <button aria-label="Cerrar formulario de video" className="rounded-full border p-2 hover:bg-muted disabled:opacity-60" disabled={isPending} onClick={onClose} type="button"><X className="h-4 w-4" aria-hidden="true" /></button>
        </div>
        <div className="mt-5 grid gap-4">
          <Input label="Titulo"><input className="h-10 rounded-md border bg-background px-3 text-sm" disabled={isPending} onChange={(event) => setForm((current) => ({ ...current, titulo: event.target.value }))} value={form.titulo} /></Input>
          <Input label="URL de YouTube"><input className="h-10 rounded-md border bg-background px-3 text-sm" disabled={isPending} onChange={(event) => setForm((current) => ({ ...current, urlYoutube: event.target.value }))} placeholder="https://www.youtube.com/watch?v=..." value={form.urlYoutube} /></Input>
          <Input label="Fecha de entrevista"><input className="h-10 rounded-md border bg-background px-3 text-sm" disabled={isPending} onChange={(event) => setForm((current) => ({ ...current, fechaEntrevista: event.target.value }))} type="date" value={form.fechaEntrevista} /></Input>
          <Input label="Descripcion"><textarea className="min-h-24 rounded-md border bg-background px-3 py-2 text-sm" disabled={isPending} onChange={(event) => setForm((current) => ({ ...current, descripcion: event.target.value }))} value={form.descripcion} /></Input>
        </div>
        {error ? <p className="mt-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p> : null}
        <div className="mt-5 flex justify-end gap-2">
          <button className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-60" disabled={isPending} onClick={onClose} type="button">Cancelar</button>
          <button className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60" disabled={isPending} onClick={submit} type="button">{isPending ? "Guardando..." : "Guardar"}</button>
        </div>
      </div>
    </div>
  );
}

function Input({ children, label }: { children: React.ReactNode; label: string }) {
  return <label className="grid gap-2 text-sm font-medium"><span>{label}</span>{children}</label>;
}

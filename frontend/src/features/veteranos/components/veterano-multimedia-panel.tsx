"use client";

/* eslint-disable @next/next/no-img-element */

import { Camera, ChevronLeft, ChevronRight, Film, ImageIcon, Trash2, Upload, X } from "lucide-react";
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
import type { VeteranoImagenResponseDTO, VeteranoResponseDTO, VeteranoVideoRequestDTO, VeteranoVideoResponseDTO } from "@/features/veteranos/types";
import { formatDate, getApiErrorMessage } from "@/features/veteranos/utils";
import { ApiClientError } from "@/lib/errors/api-error";

type VeteranoMultimediaPanelProps = {
  canWrite: boolean;
  veterano: VeteranoResponseDTO;
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

type MultimediaItem = {
  key: string;
  type: "IMAGE" | "VIDEO";
  id: number;
  title: string;
  description?: string | null;
  orden: number;
  fecha?: string | null;
  imageUrl?: string;
  thumbnailUrl?: string;
  videoId?: string;
  video?: VeteranoVideoResponseDTO;
};

const formatosImagenPermitidos = new Set(["image/jpeg", "image/png", "image/webp"]);
const tamanioMaximoImagenBytes = 10 * 1024 * 1024;

function ordenarImagenes(imagenes: VeteranoImagenResponseDTO[]) {
  return [...imagenes].sort((a, b) => a.orden - b.orden || new Date(a.fechaCarga).getTime() - new Date(b.fechaCarga).getTime() || a.id - b.id);
}

function ordenarVideos(videos: VeteranoVideoResponseDTO[]) {
  return [...videos].sort((a, b) => a.orden - b.orden || a.id - b.id);
}

function youtubeThumbnail(videoId: string) {
  return `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
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

export function VeteranoMultimediaPanel({ canWrite, veterano }: VeteranoMultimediaPanelProps) {
  const veteranoId = veterano.id;
  const imagenesQuery = useImagenesVeteranoQuery(veteranoId);
  const videosQuery = useVideosVeteranoQuery(veteranoId);
  const subirImagenes = useSubirImagenesVeteranoMutation(veteranoId);
  const eliminarImagen = useEliminarImagenVeteranoMutation(veteranoId);
  const crearVideo = useCrearVideoVeteranoMutation(veteranoId);
  const actualizarVideo = useActualizarVideoVeteranoMutation(veteranoId);
  const eliminarVideo = useEliminarVideoVeteranoMutation(veteranoId);

  const imagenes = useMemo(() => ordenarImagenes(imagenesQuery.data ?? []), [imagenesQuery.data]);
  const videos = useMemo(() => ordenarVideos(videosQuery.data ?? []), [videosQuery.data]);
  const [imagenUrls, setImagenUrls] = useState<Record<number, string>>({});
  const imagenUrlsRef = useRef<Record<number, string>>({});
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [modalCargaAbierto, setModalCargaAbierto] = useState(false);
  const [modalVideoAbierto, setModalVideoAbierto] = useState(false);
  const [videoEditando, setVideoEditando] = useState<VeteranoVideoResponseDTO | null>(null);
  const [visorAbierto, setVisorAbierto] = useState(false);
  const [visorImagenId, setVisorImagenId] = useState<number | null>(null);

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

  const mediaItems = useMemo<MultimediaItem[]>(() => {
    const imageItems = imagenes.map((imagen) => ({
      key: `image-${imagen.id}`,
      type: "IMAGE" as const,
      id: imagen.id,
      title: imagen.descripcion || imagen.nombreArchivo,
      description: imagen.descripcion,
      orden: imagen.orden,
      fecha: imagen.fechaCarga,
      imageUrl: imagenUrls[imagen.id],
      thumbnailUrl: imagenUrls[imagen.id]
    }));
    const videoItems = videos.map((video) => ({
      key: `video-${video.id}`,
      type: "VIDEO" as const,
      id: video.id,
      title: video.titulo,
      description: video.descripcion,
      orden: video.orden,
      fecha: video.fechaEntrevista,
      thumbnailUrl: youtubeThumbnail(video.videoId),
      videoId: video.videoId,
      video
    }));
    return [...imageItems, ...videoItems].sort((a, b) => a.orden - b.orden || (a.type === b.type ? a.id - b.id : a.type === "IMAGE" ? -1 : 1));
  }, [imagenes, imagenUrls, videos]);

  const selectedItem = mediaItems.find((item) => item.key === selectedKey) ?? mediaItems[0] ?? null;
  const carouselItems = selectedItem ? mediaItems.filter((item) => item.key !== selectedItem.key) : mediaItems;
  const imagenesVisor = mediaItems.filter((item) => item.type === "IMAGE");
  const visorImagenActual = imagenesVisor.find((item) => item.id === visorImagenId) ?? (selectedItem?.type === "IMAGE" ? selectedItem : imagenesVisor[0]);
  const visorIndex = visorImagenActual ? imagenesVisor.findIndex((item) => item.id === visorImagenActual.id) : -1;
  const cargandoMultimedia = imagenesQuery.isLoading || videosQuery.isLoading;
  const sinMultimedia = !cargandoMultimedia && mediaItems.length === 0;

  const abrirCargaFotos = () => setModalCargaAbierto(true);
  const abrirNuevoVideo = () => {
    setVideoEditando(null);
    setModalVideoAbierto(true);
  };
  const cerrarModalVideo = () => {
    setModalVideoAbierto(false);
    setVideoEditando(null);
  };

  const irAImagenAnterior = useCallback(() => {
    if (imagenesVisor.length <= 1 || visorIndex < 0) {
      return;
    }
    const nextIndex = (visorIndex - 1 + imagenesVisor.length) % imagenesVisor.length;
    setVisorImagenId(imagenesVisor[nextIndex].id);
  }, [imagenesVisor, visorIndex]);

  const irAImagenSiguiente = useCallback(() => {
    if (imagenesVisor.length <= 1 || visorIndex < 0) {
      return;
    }
    const nextIndex = (visorIndex + 1) % imagenesVisor.length;
    setVisorImagenId(imagenesVisor[nextIndex].id);
  }, [imagenesVisor, visorIndex]);

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
    <section className="space-y-6">
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.15fr)_minmax(360px,0.85fr)]">
        <div className="space-y-4">
          <div className="relative flex min-h-[340px] items-center justify-center overflow-hidden rounded-lg border bg-muted/30 sm:min-h-[440px]">
            {cargandoMultimedia ? <LoadingState label="Cargando multimedia..." /> : null}

            {selectedItem?.type === "IMAGE" ? (
              <button
                aria-label="Abrir foto principal en visor ampliado"
                className="h-full min-h-[340px] w-full focus:outline-none focus:ring-2 focus:ring-ring sm:min-h-[440px]"
                onClick={() => {
                  setVisorImagenId(selectedItem.id);
                  setVisorAbierto(true);
                }}
                type="button"
              >
                {selectedItem.imageUrl ? (
                  <img alt={selectedItem.title} className="h-full max-h-[440px] w-full object-contain p-2" src={selectedItem.imageUrl} />
                ) : (
                  <span className="flex h-full min-h-[340px] items-center justify-center text-sm text-muted-foreground sm:min-h-[440px]">Cargando imagen...</span>
                )}
              </button>
            ) : null}

            {selectedItem?.type === "VIDEO" && selectedItem.videoId ? (
              <div className="aspect-video w-full bg-black">
                <iframe
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                  allowFullScreen
                  className="h-full w-full"
                  src={`https://www.youtube.com/embed/${selectedItem.videoId}`}
                  title={selectedItem.title}
                />
              </div>
            ) : null}

            {sinMultimedia ? (
              <div className="flex flex-col items-center justify-center gap-3 p-6 text-center">
                <div className="flex h-16 w-16 items-center justify-center rounded-full border bg-background text-muted-foreground">
                  <Camera className="h-8 w-8" aria-hidden="true" />
                </div>
                <p className="font-medium">Sin material multimedia registrado</p>
                {canWrite ? (
                  <div className="flex flex-wrap justify-center gap-2">
                    <button className="rounded-md border px-3 py-1.5 text-sm font-medium hover:bg-muted" onClick={abrirCargaFotos} type="button">Agregar fotos</button>
                    <button className="rounded-md border px-3 py-1.5 text-sm font-medium hover:bg-muted" onClick={abrirNuevoVideo} type="button">Agregar video</button>
                  </div>
                ) : null}
              </div>
            ) : null}
          </div>

          {selectedItem ? (
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="font-medium">{selectedItem.title}</p>
                <p className="text-sm text-muted-foreground">{selectedItem.type === "IMAGE" ? "Foto" : "Video / entrevista"}{selectedItem.fecha ? ` · ${formatDate(selectedItem.fecha)}` : ""}</p>
              </div>
              {canWrite ? (
                <div className="flex flex-wrap gap-2">
                  <button className="rounded-md border px-3 py-1.5 text-sm font-medium hover:bg-muted" onClick={abrirCargaFotos} type="button">Agregar fotos</button>
                  <button className="rounded-md border px-3 py-1.5 text-sm font-medium hover:bg-muted" onClick={abrirNuevoVideo} type="button">Agregar video</button>
                  {selectedItem.type === "VIDEO" && selectedItem.video ? (
                    <button className="rounded-md border px-3 py-1.5 text-sm hover:bg-muted" onClick={() => { setVideoEditando(selectedItem.video ?? null); setModalVideoAbierto(true); }} type="button">Editar video</button>
                  ) : null}
                  <button
                    className="rounded-md border px-3 py-1.5 text-sm text-destructive hover:bg-destructive/10 disabled:opacity-60"
                    disabled={eliminarImagen.isPending || eliminarVideo.isPending}
                    onClick={() => {
                      if (selectedItem.type === "IMAGE" && window.confirm("Eliminar foto del veterano?")) {
                        eliminarImagen.mutate(selectedItem.id, { onSuccess: () => setSelectedKey(null) });
                      }
                      if (selectedItem.type === "VIDEO" && window.confirm("Eliminar video del veterano?")) {
                        eliminarVideo.mutate(selectedItem.id, { onSuccess: () => setSelectedKey(null) });
                      }
                    }}
                    type="button"
                  >
                    Eliminar
                  </button>
                </div>
              ) : null}
            </div>
          ) : null}
        </div>

        <aside className="rounded-lg border p-5">
          <h2 className="text-lg font-semibold">Datos principales</h2>
          <dl className="mt-5 grid gap-4 text-sm">
            <Dato label="Nombre completo" value={veterano.nombreCompleto} />
            <Dato label="Nombre" value={veterano.nombre} />
            <Dato label="Apellido" value={veterano.apellido} />
            <Dato label="Fuerza" value={veterano.fuerza} />
            <Dato label="Nacimiento" value={formatDate(veterano.fechaNacimiento)} />
            <Dato label="Fallecimiento" value={formatDate(veterano.fechaFallecimiento)} />
          </dl>
        </aside>
      </div>

      {imagenesQuery.isError ? <ErrorState message={getApiErrorMessage(imagenesQuery.error)} requestId={imagenesQuery.error instanceof ApiClientError ? imagenesQuery.error.requestId : undefined} /> : null}
      {videosQuery.isError ? <ErrorState message={getApiErrorMessage(videosQuery.error)} requestId={videosQuery.error instanceof ApiClientError ? videosQuery.error.requestId : undefined} /> : null}
      {subirImagenes.isError ? <ErrorState message={getApiErrorMessage(subirImagenes.error)} requestId={subirImagenes.error instanceof ApiClientError ? subirImagenes.error.requestId : undefined} /> : null}
      {crearVideo.isError ? <ErrorState message={getApiErrorMessage(crearVideo.error)} requestId={crearVideo.error instanceof ApiClientError ? crearVideo.error.requestId : undefined} /> : null}
      {actualizarVideo.isError ? <ErrorState message={getApiErrorMessage(actualizarVideo.error)} requestId={actualizarVideo.error instanceof ApiClientError ? actualizarVideo.error.requestId : undefined} /> : null}
      {eliminarImagen.isError ? <ErrorState message={getApiErrorMessage(eliminarImagen.error)} requestId={eliminarImagen.error instanceof ApiClientError ? eliminarImagen.error.requestId : undefined} /> : null}
      {eliminarVideo.isError ? <ErrorState message={getApiErrorMessage(eliminarVideo.error)} requestId={eliminarVideo.error instanceof ApiClientError ? eliminarVideo.error.requestId : undefined} /> : null}

      {carouselItems.length > 0 ? (
        <div className="space-y-3">
          <h2 className="text-lg font-semibold">Multimedia del veterano</h2>
          <div className="flex gap-3 overflow-x-auto pb-1">
            {carouselItems.map((item) => (
              <button
                aria-label={`Mostrar ${item.title} como contenido principal`}
                className="group relative h-28 w-40 shrink-0 overflow-hidden rounded-md border bg-muted/30 text-left focus:outline-none focus:ring-2 focus:ring-ring"
                key={item.key}
                onClick={() => setSelectedKey(item.key)}
                type="button"
              >
                {item.thumbnailUrl ? <img alt={item.title} className="h-full w-full object-cover transition group-hover:scale-105" src={item.thumbnailUrl} /> : <div className="flex h-full items-center justify-center text-muted-foreground"><ImageIcon className="h-7 w-7" aria-hidden="true" /></div>}
                <span className="absolute left-2 top-2 inline-flex items-center gap-1 rounded bg-black/70 px-2 py-1 text-xs font-medium text-white">
                  {item.type === "IMAGE" ? <ImageIcon className="h-3 w-3" aria-hidden="true" /> : <Film className="h-3 w-3" aria-hidden="true" />}
                  {item.type === "IMAGE" ? "Foto" : "Video"}
                </span>
                <span className="absolute inset-x-0 bottom-0 line-clamp-2 bg-black/70 px-2 py-1 text-xs text-white">{item.title}</span>
              </button>
            ))}
          </div>
        </div>
      ) : null}

      <section className="rounded-lg border p-5">
        <h2 className="text-lg font-semibold">Datos personales</h2>
        <dl className="mt-5 grid gap-4 text-sm sm:grid-cols-2">
          <Dato label="Nombre" value={veterano.nombre} />
          <Dato label="Apellido" value={veterano.apellido} />
          <Dato label="Fuerza" value={veterano.fuerza} />
          <Dato label="Nacimiento" value={formatDate(veterano.fechaNacimiento)} />
          <Dato label="Fallecimiento" value={formatDate(veterano.fechaFallecimiento)} />
          <div className="sm:col-span-2">
            <dt className="text-muted-foreground">Observaciones / historia</dt>
            <dd className="mt-1 whitespace-pre-wrap font-medium">{veterano.historia || "Sin historia registrada"}</dd>
          </div>
        </dl>
      </section>

      <VeteranoImagenesUploadModal
        isPending={subirImagenes.isPending}
        mutationError={subirImagenes.error}
        onClose={() => setModalCargaAbierto(false)}
        onSubmit={(archivos, onSuccess) => {
          subirImagenes.mutate({ archivos }, {
            onSuccess: (cargadas) => {
              const primera = cargadas[0];
              if (primera) {
                setSelectedKey(`image-${primera.id}`);
              }
              onSuccess();
              setModalCargaAbierto(false);
            }
          });
        }}
        open={modalCargaAbierto}
      />

      {modalVideoAbierto ? (
        <VeteranoVideoModal
          isPending={crearVideo.isPending || actualizarVideo.isPending}
          onClose={cerrarModalVideo}
          onSubmit={(values) => {
            if (videoEditando) {
              actualizarVideo.mutate({ videoId: videoEditando.id, payload: values }, { onSuccess: cerrarModalVideo });
            } else {
              crearVideo.mutate(values, {
                onSuccess: (creado) => {
                  setSelectedKey(`video-${creado.id}`);
                  cerrarModalVideo();
                }
              });
            }
          }}
          video={videoEditando}
        />
      ) : null}

      {visorAbierto && visorImagenActual ? (
        <div aria-label="Visor de foto del veterano" aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4" onClick={() => setVisorAbierto(false)} role="dialog">
          <button aria-label="Cerrar visor" className="absolute right-4 top-4 rounded-full border border-white/30 bg-black/40 p-2 text-white hover:bg-black/70" onClick={() => setVisorAbierto(false)} type="button">
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
          {imagenesVisor.length > 1 ? <button aria-label="Foto anterior" className="absolute left-3 top-1/2 hidden -translate-y-1/2 rounded-full border border-white/30 bg-black/40 p-3 text-white hover:bg-black/70 sm:block" onClick={(event) => { event.stopPropagation(); irAImagenAnterior(); }} type="button"><ChevronLeft className="h-7 w-7" aria-hidden="true" /></button> : null}
          <div className="flex max-h-[92vh] w-full max-w-6xl flex-col gap-4" onClick={(event) => event.stopPropagation()}>
            <div className="flex min-h-0 flex-1 items-center justify-center">
              {visorImagenActual.imageUrl ? <img alt={visorImagenActual.title} className="max-h-[78vh] max-w-full object-contain" src={visorImagenActual.imageUrl} /> : <span className="text-sm text-white">Cargando imagen...</span>}
            </div>
            {imagenesVisor.length > 1 ? (
              <div className="flex justify-center gap-2 overflow-x-auto">
                {imagenesVisor.map((item) => (
                  <button aria-label={`Ver ${item.title}`} className={`h-16 w-20 shrink-0 overflow-hidden rounded border ${item.id === visorImagenActual.id ? "border-white" : "border-white/20"}`} key={item.key} onClick={() => setVisorImagenId(item.id)} type="button">
                    {item.thumbnailUrl ? <img alt={item.title} className="h-full w-full object-cover" src={item.thumbnailUrl} /> : null}
                  </button>
                ))}
              </div>
            ) : null}
          </div>
          {imagenesVisor.length > 1 ? <button aria-label="Foto siguiente" className="absolute right-3 top-1/2 hidden -translate-y-1/2 rounded-full border border-white/30 bg-black/40 p-3 text-white hover:bg-black/70 sm:block" onClick={(event) => { event.stopPropagation(); irAImagenSiguiente(); }} type="button"><ChevronRight className="h-7 w-7" aria-hidden="true" /></button> : null}
        </div>
      ) : null}
    </section>
  );
}

function Dato({ label, value }: { label: string; value?: React.ReactNode }) {
  return (
    <div>
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="mt-1 font-medium">{value || "Sin registrar"}</dd>
    </div>
  );
}

function VeteranoImagenesUploadModal({ isPending, mutationError, onClose, onSubmit, open }: { open: boolean; isPending: boolean; mutationError: unknown; onClose: () => void; onSubmit: (archivos: File[], onSuccess: () => void) => void }) {
  const [imagenes, setImagenes] = useState<ImagenSeleccionada[]>([]);
  const [error, setError] = useState<string | null>(null);

  const limpiarImagenes = useCallback(() => {
    setImagenes((actuales) => {
      actuales.forEach((imagen) => URL.revokeObjectURL(imagen.previewUrl));
      return [];
    });
  }, []);

  const cerrar = useCallback(() => {
    if (isPending) {
      return;
    }
    limpiarImagenes();
    setError(null);
    onClose();
  }, [isPending, limpiarImagenes, onClose]);

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
          <button aria-label="Cerrar carga de fotos" className="rounded-full border p-2 hover:bg-muted disabled:opacity-60" disabled={isPending} onClick={cerrar} type="button"><X className="h-4 w-4" aria-hidden="true" /></button>
        </div>
        <label className="mt-5 flex min-h-36 cursor-pointer flex-col items-center justify-center gap-3 rounded-lg border border-dashed bg-muted/30 p-6 text-center hover:bg-muted/50">
          <Upload className="h-8 w-8 text-muted-foreground" aria-hidden="true" />
          <span className="text-sm font-medium">Seleccionar fotos</span>
          <span className="text-xs text-muted-foreground">JPG, JPEG, PNG o WEBP. Maximo 10 MB por imagen.</span>
          <input accept="image/jpeg,image/png,image/webp" className="sr-only" disabled={isPending} multiple onChange={(event) => { agregarArchivos(event.target.files); event.currentTarget.value = ""; }} type="file" />
        </label>
        {imagenes.length > 0 ? <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{imagenes.map((imagen) => <div className="overflow-hidden rounded-md border bg-background" key={imagen.id}><img alt={imagen.file.name} className="h-36 w-full bg-muted object-cover" src={imagen.previewUrl} /><div className="flex items-center justify-between gap-2 p-3 text-sm"><p className="truncate font-medium" title={imagen.file.name}>{imagen.file.name}</p><button aria-label={`Quitar ${imagen.file.name}`} className="rounded-md border p-1.5 text-destructive hover:bg-destructive/10 disabled:opacity-60" disabled={isPending} onClick={() => quitarImagen(imagen.id)} type="button"><Trash2 className="h-4 w-4" aria-hidden="true" /></button></div></div>)}</div> : null}
        {error ? <p className="mt-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p> : null}
        {mutationError ? <p className="mt-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{getApiErrorMessage(mutationError)}</p> : null}
        <div className="mt-5 flex flex-wrap justify-end gap-2">
          <button className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-60" disabled={isPending} onClick={cerrar} type="button">Cancelar</button>
          <button className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60" disabled={imagenes.length === 0 || isPending} onClick={() => onSubmit(imagenes.map((imagen) => imagen.file), () => { limpiarImagenes(); setError(null); })} type="button">{isPending ? "Cargando..." : "Cargar fotos"}</button>
        </div>
      </div>
    </div>
  );
}

function VeteranoVideoModal({ isPending, onClose, onSubmit, video }: { video: VeteranoVideoResponseDTO | null; isPending: boolean; onClose: () => void; onSubmit: (values: VeteranoVideoRequestDTO) => void }) {
  const [form, setForm] = useState<VideoFormState>({ titulo: video?.titulo ?? "", urlYoutube: video?.urlYoutube ?? "", descripcion: video?.descripcion ?? "", fechaEntrevista: video?.fechaEntrevista ?? "" });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !isPending) {
        onClose();
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isPending, onClose]);

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

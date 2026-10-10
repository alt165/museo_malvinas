"use client";

/* eslint-disable @next/next/no-img-element */

import { Camera, ChevronLeft, ChevronRight, ClipboardList, Download, FileText, Leaf, Receipt, Scale, Trash2, X, type LucideIcon } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { AppShell } from "@/components/layout/app-shell";
import { useDetallesConservacionQuery } from "@/features/tablas-auxiliares/queries";
import { hasRole, useAuth } from "@/lib/auth";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import {
  descargarCopiaFirmadaRecibo,
  descargarFichaObjetoPdf,
  descargarFotoObjeto,
  descargarFotoOriginalObjeto,
  descargarReciboEscaneadoObjetoPorId,
  descargarReciboPdf
} from "@/features/objetos/api";
import {
  objetosQueryKeys,
  useEliminarFotoObjetoMutation,
  useObjetoQuery,
  useRecibosEscaneadosObjetoQuery,
  useRecibosObjetoQuery
} from "@/features/objetos/queries";
import { ObjetoImagenesUploadModal } from "@/features/objetos/components/objeto-imagenes-upload-modal";
import type { FotoObjetoMuseoResponseDTO, ObjetoMuseoResponseDTO } from "@/features/objetos/types";
import { getApiErrorMessage } from "@/features/objetos/utils";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

function descargarBlob(blob: Blob, nombre: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = nombre;
  link.click();
  URL.revokeObjectURL(url);
}

async function visualizarReciboEscaneado(objetoId: number, reciboId: number) {
  const ventana = window.open("about:blank", "_blank");
  const blob = await descargarReciboEscaneadoObjetoPorId(objetoId, reciboId);
  if (!ventana) {
    descargarBlob(blob, `recibo-escaneado-${reciboId}`);
    return;
  }
  ventana.opener = null;
  const url = URL.createObjectURL(blob);
  ventana.location.href = url;
  ventana.addEventListener("beforeunload", () => URL.revokeObjectURL(url), { once: true });
}

function hasDisplayValue(value: React.ReactNode) {
  if (value === null || value === undefined || value === "") {
    return false;
  }
  if (Array.isArray(value)) {
    return value.length > 0;
  }
  return true;
}

function enumLabel(value?: string | null) {
  return value ? value.replaceAll("_", " ").toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase()) : null;
}

function getEstadoConservacionBadgeClass(value?: string | null) {
  const normalizedValue = value?.trim().toLocaleUpperCase("es-AR");
  if (normalizedValue === "EXCELENTE" || normalizedValue === "BUENO") {
    return "border-emerald-200 bg-emerald-50 text-emerald-800";
  }
  if (normalizedValue === "REGULAR") {
    return "border-amber-200 bg-amber-50 text-amber-800";
  }
  if (normalizedValue === "MALO" || normalizedValue === "CRITICO" || normalizedValue === "CRÍTICO") {
    return "border-red-200 bg-red-50 text-red-800";
  }
  return "border-slate-200 bg-slate-50 text-slate-700";
}

function boolLabel(value?: boolean | null) {
  if (value === true) return "Sí";
  if (value === false) return "No";
  return null;
}

function formatFecha(value?: string | null) {
  if (!value) {
    return "No especificada";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("es-AR").format(date);
}

function ordenarFotos(fotos?: FotoObjetoMuseoResponseDTO[]) {
  return [...(fotos ?? [])].sort((a, b) => {
    const fechaA = new Date(a.fechaCarga).getTime();
    const fechaB = new Date(b.fechaCarga).getTime();
    if (fechaA !== fechaB) {
      return fechaA - fechaB;
    }
    return a.id - b.id;
  });
}

type DatoDetalle = {
  label: string;
  value: React.ReactNode;
  wide?: boolean;
  longText?: boolean;
  alignWideValue?: boolean;
  splitSeparator?: boolean;
  hideBottomSeparator?: boolean;
  topSeparator?: boolean;
};

function ObjetoDato({ label, value, wide, longText, alignWideValue, splitSeparator, hideBottomSeparator, topSeparator }: DatoDetalle) {
  return (
    <div className={`grid items-start gap-1 py-3 sm:gap-4 ${hideBottomSeparator ? "" : "border-b border-primary/10 last:border-b-0"} ${topSeparator ? "border-t border-primary/10" : ""} ${alignWideValue ? "sm:grid-cols-[max(120px,calc(((((100%_-_1.5rem)_/_2)_-_1rem)_*_0.4)))_minmax(0,1fr)]" : "sm:grid-cols-[minmax(120px,0.8fr)_minmax(0,1.2fr)]"} ${wide ? "sm:col-span-2" : ""} ${splitSeparator ? "relative sm:border-b-0 sm:before:absolute sm:before:bottom-0 sm:before:left-0 sm:before:w-[calc((100%_-_1.5rem)/2)] sm:before:border-b sm:before:[border-color:inherit] sm:before:content-[''] sm:after:absolute sm:after:bottom-0 sm:after:right-0 sm:after:w-[calc((100%_-_1.5rem)/2)] sm:after:border-b sm:after:[border-color:inherit] sm:after:content-['']" : ""}`}>
      <dt className="font-medium text-muted-foreground">{label}</dt>
      <dd className={`whitespace-pre-wrap break-words font-medium text-foreground ${longText ? "max-h-48 overflow-y-auto pr-2" : ""}`}>{value}</dd>
    </div>
  );
}

function SectionHeader({ icon: Icon, title }: { icon: LucideIcon; title: string }) {
  return (
    <div className="flex items-center gap-2.5 border-b border-primary/15 px-5 py-4">
      <Icon aria-hidden="true" className="h-5 w-5 shrink-0 text-primary" strokeWidth={2.1} />
      <h2 className="text-base font-semibold text-primary">{title}</h2>
    </div>
  );
}



type GaleriaObjetoProps = {
  objeto: ObjetoMuseoResponseDTO;
  puedeEscribir: boolean;
  esAdmin: boolean;
};

function GaleriaObjeto({ objeto, puedeEscribir, esAdmin }: GaleriaObjetoProps) {
  const queryClient = useQueryClient();
  const eliminarFotoMutation = useEliminarFotoObjetoMutation(objeto.id);
  const fotosOrdenadas = useMemo(() => ordenarFotos(objeto.fotos), [objeto.fotos]);
  const [fotoPrincipalId, setFotoPrincipalId] = useState<number | null>(null);
  const [visorAbierto, setVisorAbierto] = useState(false);
  const [modalCargaAbierto, setModalCargaAbierto] = useState(false);
  const [visorFotoId, setVisorFotoId] = useState<number | null>(null);
  const [fotoUrls, setFotoUrls] = useState<Record<number, string>>({});
  const [descargandoOriginal, setDescargandoOriginal] = useState(false);
  const [errorDescargaOriginal, setErrorDescargaOriginal] = useState<unknown>(null);
  const fotoUrlsRef = useRef<Record<number, string>>({});
  const miniaturaRefs = useRef<Record<number, HTMLButtonElement | null>>({});

  useEffect(() => {
    let activo = true;
    const urlsNuevas: string[] = [];

    async function cargarFotos() {
      try {
        const entries = await Promise.all(
          fotosOrdenadas.map(async (foto) => {
            const blob = await descargarFotoObjeto(objeto.id, foto.id);
            const url = URL.createObjectURL(blob);
            urlsNuevas.push(url);
            return [foto.id, url] as const;
          })
        );

        if (activo) {
          const nextUrls = Object.fromEntries(entries);
          Object.values(fotoUrlsRef.current).forEach((url) => URL.revokeObjectURL(url));
          fotoUrlsRef.current = nextUrls;
          setFotoUrls(nextUrls);
        } else {
          urlsNuevas.forEach((url) => URL.revokeObjectURL(url));
        }
      } catch {
        if (activo) {
          Object.values(fotoUrlsRef.current).forEach((url) => URL.revokeObjectURL(url));
          fotoUrlsRef.current = {};
          setFotoUrls({});
        }
      }
    }

    if (fotosOrdenadas.length > 0) {
      void cargarFotos();
    } else {
      Promise.resolve().then(() => {
        if (activo) {
          Object.values(fotoUrlsRef.current).forEach((url) => URL.revokeObjectURL(url));
          fotoUrlsRef.current = {};
          setFotoUrls({});
        }
      });
    }

    return () => {
      activo = false;
      urlsNuevas.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [fotosOrdenadas, objeto.id]);

  const fotoPrincipal = fotosOrdenadas.find((foto) => foto.id === fotoPrincipalId) ?? fotosOrdenadas[0];
  const fotoPrincipalIndex = fotoPrincipal ? fotosOrdenadas.findIndex((foto) => foto.id === fotoPrincipal.id) : -1;
  const visorFotoActual = fotosOrdenadas.find((foto) => foto.id === visorFotoId) ?? fotoPrincipal;
  const visorIndex = visorFotoActual ? fotosOrdenadas.findIndex((foto) => foto.id === visorFotoActual.id) : -1;
  const hayVariasFotos = fotosOrdenadas.length > 1;

  function seleccionarFotoAnterior() {
    if (!hayVariasFotos || fotoPrincipalIndex < 0) return;
    const nextIndex = (fotoPrincipalIndex - 1 + fotosOrdenadas.length) % fotosOrdenadas.length;
    setFotoPrincipalId(fotosOrdenadas[nextIndex].id);
  }

  function seleccionarFotoSiguiente() {
    if (!hayVariasFotos || fotoPrincipalIndex < 0) return;
    const nextIndex = (fotoPrincipalIndex + 1) % fotosOrdenadas.length;
    setFotoPrincipalId(fotosOrdenadas[nextIndex].id);
  }

  function eliminarFotoSeleccionada() {
    if (!fotoPrincipal || !window.confirm("¿Eliminar la imagen seleccionada del objeto?")) return;
    const siguienteFoto = fotosOrdenadas[fotoPrincipalIndex + 1] ?? fotosOrdenadas[fotoPrincipalIndex - 1] ?? null;
    eliminarFotoMutation.mutate(fotoPrincipal.id, {
      onSuccess: () => {
        setFotoPrincipalId(siguienteFoto?.id ?? null);
        void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.detail(objeto.id) });
      }
    });
  }

  async function descargarOriginalSeleccionado() {
    if (!fotoPrincipal || descargandoOriginal) return;
    setDescargandoOriginal(true);
    setErrorDescargaOriginal(null);
    try {
      const blob = await descargarFotoOriginalObjeto(objeto.id, fotoPrincipal.id);
      descargarBlob(blob, fotoPrincipal.nombreArchivo || `objeto-${objeto.numeroInventario}-foto-${fotoPrincipal.id}`);
    } catch (error) {
      setErrorDescargaOriginal(error);
    } finally {
      setDescargandoOriginal(false);
    }
  }

  useEffect(() => {
    if (!fotoPrincipal) return;
    const miniatura = miniaturaRefs.current[fotoPrincipal.id];
    const contenedor = miniatura?.parentElement;
    if (!miniatura || !contenedor) return;
    contenedor.scrollTo({
      behavior: "smooth",
      left: miniatura.offsetLeft - (contenedor.clientWidth - miniatura.clientWidth) / 2
    });
  }, [fotoPrincipal]);

  const irAImagenAnterior = useCallback(() => {
    if (!hayVariasFotos || visorIndex < 0) {
      return;
    }
    const nextIndex = (visorIndex - 1 + fotosOrdenadas.length) % fotosOrdenadas.length;
    setVisorFotoId(fotosOrdenadas[nextIndex].id);
  }, [fotosOrdenadas, hayVariasFotos, visorIndex]);

  const irAImagenSiguiente = useCallback(() => {
    if (!hayVariasFotos || visorIndex < 0) {
      return;
    }
    const nextIndex = (visorIndex + 1) % fotosOrdenadas.length;
    setVisorFotoId(fotosOrdenadas[nextIndex].id);
  }, [fotosOrdenadas, hayVariasFotos, visorIndex]);

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
  }, [visorAbierto, irAImagenAnterior, irAImagenSiguiente]);

  if (!fotoPrincipal) {
    return (
      <div className="flex min-h-[360px] flex-col items-center justify-center gap-4 rounded-lg border border-primary/15 bg-white p-8 text-center shadow-sm">
        <div className="flex h-20 w-20 items-center justify-center rounded-full border bg-background text-muted-foreground">
          <Camera className="h-10 w-10" aria-hidden="true" />
        </div>
        <div>
          <p className="text-base font-semibold">Sin imagen disponible</p>
          <p className="mt-1 text-sm text-muted-foreground">No hay fotos registradas para este objeto.</p>
        </div>
        {puedeEscribir ? (
          <button className="rounded-md border bg-background px-4 py-2 text-sm font-medium hover:bg-muted" onClick={() => setModalCargaAbierto(true)} type="button">
            Agregar imagenes
          </button>
        ) : null}
        <ObjetoImagenesUploadModal
          objetoId={objeto.id}
          onClose={() => setModalCargaAbierto(false)}
          onUploaded={(fotoId) => setFotoPrincipalId(fotoId ?? null)}
          open={modalCargaAbierto}
        />
      </div>
    );
  }

  return (
    <>
      <div className="overflow-hidden rounded-lg border border-primary/15 bg-white shadow-sm">
        <button
          aria-label="Abrir imagen principal en visor ampliado"
          className="block h-[360px] w-full overflow-hidden border-b border-primary/10 bg-muted/20 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring sm:h-[460px]"
          onClick={() => {
            setVisorFotoId(fotoPrincipal.id);
            setVisorAbierto(true);
          }}
          type="button"
        >
          {fotoUrls[fotoPrincipal.id] ? (
            <img
              alt={fotoPrincipal.descripcion || fotoPrincipal.nombreArchivo || objeto.denominacionObjeto}
              className="h-full w-full object-contain"
              src={fotoUrls[fotoPrincipal.id]}
            />
          ) : (
            <span className="flex h-full items-center justify-center text-sm text-muted-foreground">Cargando imagen...</span>
          )}
        </button>

        <div className="space-y-3 p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-sm font-semibold text-primary">Imagenes del objeto</h2>
            {puedeEscribir || esAdmin ? (
              <div className="flex flex-wrap items-center gap-2">
                {esAdmin ? (
                  <button
                    className="inline-flex items-center gap-1.5 rounded-md border border-primary/20 px-3 py-1.5 text-sm font-medium text-primary hover:bg-primary/5 disabled:cursor-not-allowed disabled:opacity-60"
                    disabled={descargandoOriginal}
                    onClick={() => void descargarOriginalSeleccionado()}
                    type="button"
                  >
                    <Download aria-hidden="true" className="h-4 w-4" />
                    {descargandoOriginal ? "Descargando..." : "Descargar original"}
                  </button>
                ) : null}
                {puedeEscribir ? (
                  <>
                    <button className="rounded-md border border-primary/20 px-3 py-1.5 text-sm font-medium text-primary hover:bg-primary/5" onClick={() => setModalCargaAbierto(true)} type="button">
                      Agregar mas imagenes
                    </button>
                    <button
                      className="inline-flex items-center gap-1.5 rounded-md border border-destructive/30 px-3 py-1.5 text-sm font-medium text-destructive hover:bg-destructive/10 disabled:cursor-not-allowed disabled:opacity-60"
                      disabled={eliminarFotoMutation.isPending}
                      onClick={eliminarFotoSeleccionada}
                      type="button"
                    >
                      <Trash2 aria-hidden="true" className="h-4 w-4" />
                      {eliminarFotoMutation.isPending ? "Eliminando..." : "Eliminar imagen"}
                    </button>
                  </>
                ) : null}
              </div>
            ) : null}
          </div>

          <div className="flex min-w-0 justify-center">
            <div className="flex max-w-full items-center justify-center gap-2">
              <button
                aria-label="Seleccionar imagen anterior"
                className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-primary/15 text-primary transition-colors hover:bg-primary/5 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-default disabled:opacity-35"
                disabled={!hayVariasFotos}
                onClick={seleccionarFotoAnterior}
                type="button"
              >
                <ChevronLeft aria-hidden="true" className="h-5 w-5" />
              </button>

              <div className="flex w-fit max-w-[calc(100%_-_5.5rem)] gap-2 overflow-x-auto px-1 pb-1">
                {fotosOrdenadas.map((foto) => {
                  const activa = foto.id === fotoPrincipal.id;
                  return (
                    <button
                      aria-label={`Usar ${foto.nombreArchivo} como imagen principal`}
                      aria-pressed={activa}
                      className={`h-20 w-24 shrink-0 overflow-hidden rounded-md border bg-muted/20 transition focus:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:h-24 sm:w-28 ${activa ? "border-primary ring-2 ring-primary/20" : "border-primary/15 hover:border-primary/40"}`}
                      key={foto.id}
                      onClick={() => setFotoPrincipalId(foto.id)}
                      ref={(element) => { miniaturaRefs.current[foto.id] = element; }}
                      type="button"
                    >
                      {fotoUrls[foto.id] ? (
                        <img alt={foto.descripcion || foto.nombreArchivo} className="h-full w-full object-cover" src={fotoUrls[foto.id]} />
                      ) : (
                        <span className="flex h-full items-center justify-center text-xs text-muted-foreground">Cargando...</span>
                      )}
                    </button>
                  );
                })}
              </div>

              <button
                aria-label="Seleccionar imagen siguiente"
                className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-primary/15 text-primary transition-colors hover:bg-primary/5 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-default disabled:opacity-35"
                disabled={!hayVariasFotos}
                onClick={seleccionarFotoSiguiente}
                type="button"
              >
                <ChevronRight aria-hidden="true" className="h-5 w-5" />
              </button>
            </div>
          </div>

          <p className="text-center text-xs font-medium text-muted-foreground">
            Imagen {fotoPrincipalIndex + 1} de {fotosOrdenadas.length}
          </p>
        </div>
      </div>

      {eliminarFotoMutation.isError ? (
        <ErrorState
          message={getApiErrorMessage(eliminarFotoMutation.error)}
          requestId={eliminarFotoMutation.error instanceof ApiClientError ? eliminarFotoMutation.error.requestId : undefined}
        />
      ) : null}

      {errorDescargaOriginal ? (
        <ErrorState
          message={getApiErrorMessage(errorDescargaOriginal)}
          requestId={errorDescargaOriginal instanceof ApiClientError ? errorDescargaOriginal.requestId : undefined}
        />
      ) : null}

      <ObjetoImagenesUploadModal
        objetoId={objeto.id}
        onClose={() => setModalCargaAbierto(false)}
        onUploaded={(fotoId) => {
          if (fotoId) {
            setFotoPrincipalId(fotoId);
          }
        }}
        open={modalCargaAbierto}
      />

      {visorAbierto && visorFotoActual ? (
        <div
          aria-label="Visor de imagen del objeto"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
          onClick={() => setVisorAbierto(false)}
          role="dialog"
        >
          <button
            aria-label="Cerrar visor"
            className="absolute right-4 top-4 rounded-full border border-white/30 bg-black/40 p-2 text-white hover:bg-black/70 focus:outline-none focus:ring-2 focus:ring-white"
            onClick={() => setVisorAbierto(false)}
            type="button"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>

          {hayVariasFotos ? (
            <button
              aria-label="Imagen anterior"
              className="absolute left-3 top-1/2 hidden -translate-y-1/2 rounded-full border border-white/30 bg-black/40 p-3 text-white hover:bg-black/70 focus:outline-none focus:ring-2 focus:ring-white sm:block"
              onClick={(event) => {
                event.stopPropagation();
                irAImagenAnterior();
              }}
              type="button"
            >
              <ChevronLeft className="h-7 w-7" aria-hidden="true" />
            </button>
          ) : null}

          <div className="flex max-h-[92vh] w-full max-w-6xl flex-col gap-4" onClick={(event) => event.stopPropagation()}>
            <div className="flex min-h-0 flex-1 items-center justify-center">
              {fotoUrls[visorFotoActual.id] ? (
                <img
                  alt={visorFotoActual.descripcion || visorFotoActual.nombreArchivo || objeto.denominacionObjeto}
                  className="max-h-[78vh] max-w-full object-contain"
                  src={fotoUrls[visorFotoActual.id]}
                />
              ) : (
                <span className="text-sm text-white">Cargando imagen...</span>
              )}
            </div>

            {hayVariasFotos ? (
              <div className="flex justify-center gap-2 overflow-x-auto px-12 pb-1">
                {fotosOrdenadas.map((foto) => {
                  const activa = foto.id === visorFotoActual.id;
                  return (
                    <button
                      aria-label={`Ver ${foto.nombreArchivo}`}
                      className={`h-16 w-20 shrink-0 overflow-hidden rounded-md border bg-black/30 focus:outline-none focus:ring-2 focus:ring-white ${activa ? "border-white" : "border-white/20"}`}
                      key={foto.id}
                      onClick={() => setVisorFotoId(foto.id)}
                      type="button"
                    >
                      {fotoUrls[foto.id] ? (
                        <img alt={foto.descripcion || foto.nombreArchivo} className="h-full w-full object-cover" src={fotoUrls[foto.id]} />
                      ) : (
                        <span className="flex h-full items-center justify-center text-[10px] text-white/70">...</span>
                      )}
                    </button>
                  );
                })}
              </div>
            ) : null}
          </div>

          {hayVariasFotos ? (
            <button
              aria-label="Imagen siguiente"
              className="absolute right-3 top-1/2 hidden -translate-y-1/2 rounded-full border border-white/30 bg-black/40 p-3 text-white hover:bg-black/70 focus:outline-none focus:ring-2 focus:ring-white sm:block"
              onClick={(event) => {
                event.stopPropagation();
                irAImagenSiguiente();
              }}
              type="button"
            >
              <ChevronRight className="h-7 w-7" aria-hidden="true" />
            </button>
          ) : null}
        </div>
      ) : null}
    </>
  );
}

export default function DetalleObjetoPage() {
  const params = useParams<{ id: string }>();
  const id = getParamId(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const { roles } = useAuth();
  const esAdmin = hasRole(roles, "ADMIN");
  const puedeVerRecibos = esAdmin || hasRole(roles, "MUSEOLOGO");
  const { data, error, isError, isLoading } = useObjetoQuery(id);
  const detallesConservacionQuery = useDetallesConservacionQuery();
  const detallesConservacionLabels = useMemo(() => new Map((detallesConservacionQuery.data ?? []).map((detalle) => [detalle.codigo, detalle.nombre])), [detallesConservacionQuery.data]);
  const { data: recibos = [], isError: recibosError } = useRecibosObjetoQuery(id, puedeVerRecibos);
  const { data: recibosEscaneados = [], isError: recibosEscaneadosError } = useRecibosEscaneadosObjetoQuery(id, puedeVerRecibos);
  const [descargandoFicha, setDescargandoFicha] = useState(false);
  const [errorDescargaFicha, setErrorDescargaFicha] = useState<unknown>(null);

  const descargarFicha = async () => {
    if (!data || descargandoFicha) return;
    setDescargandoFicha(true);
    setErrorDescargaFicha(null);
    try {
      const blob = await descargarFichaObjetoPdf(data.id);
      const inventarioSeguro = (data.numeroInventario || String(data.id))
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/[^A-Za-z0-9._-]+/g, "-")
        .replace(/^-+|-+$/g, "");
      descargarBlob(blob, `ficha-objeto-${inventarioSeguro || data.id}.pdf`);
    } catch (downloadError) {
      setErrorDescargaFicha(downloadError);
    } finally {
      setDescargandoFicha(false);
    }
  };

  const datosDetalle: DatoDetalle[] = data ? [
    { label: "Numero de inventario", value: data.numeroInventario },
    {
      label: "Estado de conservacion",
      value: data.estadoConservacion ? (
        <span className={`inline-flex rounded-md border px-2 py-0.5 text-xs font-semibold ${getEstadoConservacionBadgeClass(data.estadoConservacion)}`}>
          {enumLabel(data.estadoConservacion)}
        </span>
      ) : null
    },
    ...(data.ubicacionVisible !== false
      ? [{ label: "Ubicación actual", value: data.ubicacionNombre || "Sin ubicación asignada" }]
      : []),
    {
      label: "Coleccion",
      value: data.coleccionId ? (
        <Link className="text-primary underline-offset-4 hover:underline" href={`/objetos/colecciones/${data.coleccionId}`}>
          {data.coleccionNombre}
        </Link>
      ) : null
    },
    { label: "Categorias", value: data.categorias?.map((categoria) => categoria.nombre).join(", ") },
    { label: "Depositante", value: data.depositanteNombre },
    { label: "Caracter de recepcion", value: enumLabel(data.caracterRecepcion) },
    { label: "Fecha de ingreso", value: data.fechaIngreso ? formatFecha(data.fechaIngreso) : null },
    { label: "Fecha de vencimiento", value: data.fechaVencimiento ? formatFecha(data.fechaVencimiento) : null },
    { label: "Materiales", value: data.materiales },
    { label: "Alto", value: data.alto },
    { label: "Ancho", value: data.ancho },
    { label: "Diámetro", value: data.diametro },
    { label: "Espesor", value: data.espesor },
    { label: "Peso", value: data.peso },
    { label: "Medidas", value: data.medidas, wide: true, alignWideValue: true, splitSeparator: true },
    { label: "Partes", value: data.cantidadPartes ? Array.from({ length: data.cantidadPartes }, (_, index) => data.numeroInventario + "_PT." + (index + 1)).join("\n") : null, wide: true, longText: true },
    { label: "Régimen de propiedad", value: enumLabel(data.regimenPropiedad) },
    { label: "Intervenciones inadecuadas", value: enumLabel(data.intervencionesInadecuadas) },
    { label: "Estado de integridad", value: enumLabel(data.estadoIntegridad), hideBottomSeparator: Boolean(data.descripcion) },
    { label: "Descripción", value: data.descripcion, wide: true, longText: true, alignWideValue: true, topSeparator: true }
  ].filter((dato) => hasDisplayValue(dato.value)) : [];

  const conservacionPreventiva: DatoDetalle[] = data ? [
    { label: "Humedad", value: enumLabel(data.humedadConservacion) },
    { label: "Temperatura", value: data.temperaturaConservacion },
    { label: "Luz", value: data.luzConservacion },
    { label: "Extintores", value: boolLabel(data.conservacionExtintores) },
    { label: "Montaje", value: boolLabel(data.conservacionMontaje) },
    { label: "Sistema eléctrico", value: boolLabel(data.conservacionSistemaElectrico) },
    { label: "Alarmas", value: boolLabel(data.conservacionAlarmas) },
    { label: "Cámaras", value: boolLabel(data.conservacionCamaras) }
  ].filter((dato) => hasDisplayValue(dato.value)) : [];

  const detallesConservacion = data?.detallesEstadoConservacion?.map((codigo) => detallesConservacionLabels.get(codigo) ?? enumLabel(codigo)).filter(Boolean).join(", ");

  return (
    <AppShell>
      <div className="space-y-6">
        <div className="flex flex-col gap-4 rounded-lg border border-primary/15 bg-white p-5 shadow-sm sm:flex-row sm:items-start sm:justify-between">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-normal text-primary">{data?.denominacionObjeto || "Detalle de objeto"}</h1>
            {data ? (
              <span className="rounded-md border border-sky-300 bg-sky-200 px-3 py-1 text-xs font-semibold text-sky-950 shadow-sm">
                {data.numeroInventario}
              </span>
            ) : null}
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <div className="flex gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos">
                Volver
              </Link>
              {puedeEscribir && data ? (
                <Link
                  className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                  href={`/objetos/${data.id}/editar`}
                >
                  Editar
                </Link>
              ) : null}
              {puedeEscribir && data ? (
                <Link
                  className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                  href={`/objetos/${data.id}/movimientos`}
                >
                  Ver movimientos
                </Link>
              ) : null}
              {esAdmin && data ? (
                <Link
                  className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                  href={`/objetos/${data.id}/historial`}
                >
                  Historial
                </Link>
              ) : null}
              {data ? (
                <button
                  className="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={descargandoFicha}
                  onClick={descargarFicha}
                  type="button"
                >
                  <Download className="h-4 w-4" aria-hidden="true" />
                  {descargandoFicha ? "Generando PDF..." : "Descargar ficha PDF"}
                </button>
              ) : null}
              {data ? (
                <Link
                  className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                  href={`/objetos/${data.id}/relaciones`}
                >
                  Ver relaciones
                </Link>
              ) : null}
            </div>
          </div>
        </div>
        {isLoading ? <LoadingState label="Cargando objeto..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {errorDescargaFicha ? (
          <ErrorState
            message={getApiErrorMessage(errorDescargaFicha)}
            requestId={errorDescargaFicha instanceof ApiClientError ? errorDescargaFicha.requestId : undefined}
          />
        ) : null}
        {data ? (
          <section className="grid gap-4 lg:grid-cols-2">
            <div className="min-w-0 lg:col-start-1 lg:row-start-1 lg:[&>div:first-child]:h-full">
              <GaleriaObjeto esAdmin={esAdmin} objeto={data} puedeEscribir={puedeEscribir} />
            </div>

            <div className="h-full min-w-0 overflow-hidden rounded-lg border border-primary/15 bg-white shadow-sm lg:col-start-2 lg:row-start-1">
              <SectionHeader icon={ClipboardList} title="Datos del objeto" />
              <dl className="grid px-5 pb-2 text-sm sm:grid-cols-2 sm:gap-x-6">
                {datosDetalle.map((dato) => (
                  <ObjetoDato key={dato.label} {...dato} />
                ))}
              </dl>
            </div>

            {data.descripcionTecnica ? (
              <section className="h-full min-w-0 overflow-hidden rounded-lg border border-primary/15 bg-white shadow-sm lg:col-start-1 lg:row-start-2">
                <SectionHeader icon={FileText} title="Descripcion tecnica" />
                <p className="max-h-64 overflow-y-auto whitespace-pre-wrap break-words px-5 py-4 text-sm font-medium leading-relaxed text-foreground">{data.descripcionTecnica}</p>
              </section>
            ) : null}

            {[data.inscripciones, data.condicionLegalBien, detallesConservacion].some(Boolean) ? (
              <section className="h-full min-w-0 overflow-hidden rounded-lg border border-primary/15 bg-white shadow-sm lg:col-start-2 lg:row-start-2">
                <SectionHeader icon={Scale} title="Información descriptiva y legal" />
                <dl className="grid px-5 pb-2 text-sm sm:grid-cols-2 sm:gap-x-6">
                  {data.inscripciones ? <ObjetoDato label="Inscripciones" value={data.inscripciones} wide /> : null}
                  {data.condicionLegalBien ? <ObjetoDato label="Condición legal del bien" value={data.condicionLegalBien} wide /> : null}
                  {detallesConservacion ? <ObjetoDato label="Detalles del estado de conservación" value={detallesConservacion} wide /> : null}
                </dl>
              </section>
            ) : null}

            {conservacionPreventiva.length > 0 ? (
              <section className="h-full min-w-0 overflow-hidden rounded-lg border border-primary/15 bg-white shadow-sm lg:col-start-1 lg:row-start-3">
                <SectionHeader icon={Leaf} title="Conservación Preventiva" />
                <dl className="grid px-5 pb-2 text-sm sm:grid-cols-2 sm:gap-x-6">
                  {conservacionPreventiva.map((dato) => <ObjetoDato key={dato.label} {...dato} />)}
                </dl>
              </section>
            ) : null}

            {puedeVerRecibos ? (
              <section className="h-full min-w-0 overflow-hidden rounded-lg border border-primary/15 bg-white shadow-sm lg:col-start-2 lg:row-start-3">
                <SectionHeader icon={Receipt} title="Recibos" />
                <div className="grid gap-3 p-5">
                  {recibosError || recibosEscaneadosError ? <p className="text-sm text-destructive">No se pudieron cargar todos los recibos.</p> : null}
                  {!recibosError && !recibosEscaneadosError && recibos.length === 0 && recibosEscaneados.length === 0 ? <p className="py-2 text-sm text-muted-foreground">Sin recibos emitidos.</p> : null}
                  {recibos.length > 0 ? <h3 className="text-sm font-semibold">Recibos de ingreso emitidos</h3> : null}
                  {recibos.map((recibo) => (
                    <div className="rounded-md border border-primary/10 p-3 text-sm" key={recibo.id}>
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <div>
                          <p className="font-medium">{recibo.numeroRecibo}</p>
                          <p className="text-muted-foreground">{recibo.tieneCopiaFirmada ? `Copia firmada: ${recibo.copiaFirmadaNombreArchivo}` : "Sin copia firmada adjunta"}</p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <button className="rounded-md border px-3 py-1.5 hover:bg-muted" onClick={async () => descargarBlob(await descargarReciboPdf(recibo.id), `recibo-${recibo.id}.pdf`)} type="button">PDF</button>
                          {recibo.tieneCopiaFirmada ? (
                            <button className="rounded-md border px-3 py-1.5 hover:bg-muted" onClick={async () => descargarBlob(await descargarCopiaFirmadaRecibo(recibo.id), recibo.copiaFirmadaNombreArchivo || `recibo-firmado-${recibo.id}`)} type="button">Copia firmada</button>
                          ) : null}
                        </div>
                      </div>
                    </div>
                  ))}
                  {recibosEscaneados.length > 0 ? <h3 className="pt-2 text-sm font-semibold">Recibos escaneados adjuntos</h3> : null}
                  {recibosEscaneados.map((recibo) => (
                    <div className="rounded-md border border-primary/10 p-3 text-sm" key={recibo.id}>
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <div>
                          <p className="font-medium">{recibo.nombreArchivoOriginal}</p>
                          <p className="text-muted-foreground">Recibo escaneado #{recibo.id}</p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <button className="rounded-md border px-3 py-1.5 hover:bg-muted" onClick={() => void visualizarReciboEscaneado(id, recibo.id)} type="button">Ver</button>
                          <button className="rounded-md border px-3 py-1.5 hover:bg-muted" onClick={async () => descargarBlob(await descargarReciboEscaneadoObjetoPorId(id, recibo.id), recibo.nombreArchivoOriginal)} type="button">Descargar</button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            ) : null}
          </section>
        ) : null}
      </div>
    </AppShell>
  );
}

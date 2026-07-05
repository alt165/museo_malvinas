"use client";

/* eslint-disable @next/next/no-img-element */

import { Camera, ChevronLeft, ChevronRight, X } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useDetallesConservacionQuery } from "@/features/tablas-auxiliares/queries";
import { hasRole, useAuth } from "@/lib/auth";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import {
  descargarCopiaFirmadaRecibo,
  descargarFotoObjeto,
  descargarReciboPdf
} from "@/features/objetos/api";
import {
  useObjetoQuery,
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
};

function ObjetoDato({ label, value, wide }: DatoDetalle) {
  return (
    <div className={wide ? "sm:col-span-2" : undefined}>
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="mt-1 whitespace-pre-wrap font-medium">{value}</dd>
    </div>
  );
}



type GaleriaObjetoProps = {
  objeto: ObjetoMuseoResponseDTO;
  puedeEscribir: boolean;
};

function GaleriaObjeto({ objeto, puedeEscribir }: GaleriaObjetoProps) {
  const fotosOrdenadas = useMemo(() => ordenarFotos(objeto.fotos), [objeto.fotos]);
  const [fotoPrincipalId, setFotoPrincipalId] = useState<number | null>(null);
  const [visorAbierto, setVisorAbierto] = useState(false);
  const [modalCargaAbierto, setModalCargaAbierto] = useState(false);
  const [visorFotoId, setVisorFotoId] = useState<number | null>(null);
  const [fotoUrls, setFotoUrls] = useState<Record<number, string>>({});
  const fotoUrlsRef = useRef<Record<number, string>>({});

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
  const fotosCarrusel = fotoPrincipal ? fotosOrdenadas.filter((foto) => foto.id !== fotoPrincipal.id) : [];
  const visorFotoActual = fotosOrdenadas.find((foto) => foto.id === visorFotoId) ?? fotoPrincipal;
  const visorIndex = visorFotoActual ? fotosOrdenadas.findIndex((foto) => foto.id === visorFotoActual.id) : -1;
  const hayVariasFotos = fotosOrdenadas.length > 1;

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
      <div className="flex min-h-[360px] flex-col items-center justify-center gap-4 rounded-lg border bg-muted/30 p-8 text-center">
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
    <div className="space-y-4">
      <button
        aria-label="Abrir imagen principal en visor ampliado"
        className="block h-[360px] w-full overflow-hidden rounded-lg border bg-muted/30 focus:outline-none focus:ring-2 focus:ring-ring sm:h-[460px]"
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

      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-sm font-semibold text-muted-foreground">Imagenes del objeto</h2>
        {puedeEscribir ? (
          <button className="rounded-md border px-3 py-1.5 text-sm font-medium hover:bg-muted" onClick={() => setModalCargaAbierto(true)} type="button">
            Agregar mas imagenes
          </button>
        ) : null}
      </div>

      {hayVariasFotos ? (
        <div className="space-y-2">
          <div className="flex gap-3 overflow-x-auto pb-1">
            {fotosCarrusel.map((foto) => (
              <button
                aria-label={`Usar ${foto.nombreArchivo} como imagen principal`}
                className="h-24 w-32 shrink-0 overflow-hidden rounded-md border bg-muted/30 focus:outline-none focus:ring-2 focus:ring-ring"
                key={foto.id}
                onClick={() => setFotoPrincipalId(foto.id)}
                type="button"
              >
                {fotoUrls[foto.id] ? (
                  <img alt={foto.descripcion || foto.nombreArchivo} className="h-full w-full object-cover" src={fotoUrls[foto.id]} />
                ) : (
                  <span className="flex h-full items-center justify-center text-xs text-muted-foreground">Cargando...</span>
                )}
              </button>
            ))}
          </div>
        </div>
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
    </div>
  );
}

export default function DetalleObjetoPage() {
  const params = useParams<{ id: string }>();
  const id = getParamId(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const { roles } = useAuth();
  const esAdmin = hasRole(roles, "ADMIN");
  const puedeVerRecibos = esAdmin || hasRole(roles, "OPERATOR");
  const { data, error, isError, isLoading } = useObjetoQuery(id);
  const detallesConservacionQuery = useDetallesConservacionQuery();
  const detallesConservacionLabels = useMemo(() => new Map((detallesConservacionQuery.data ?? []).map((detalle) => [detalle.codigo, detalle.nombre])), [detallesConservacionQuery.data]);
  const { data: recibos = [] } = useRecibosObjetoQuery(id, puedeVerRecibos);

  const datosDetalle: DatoDetalle[] = data ? [
    { label: "Numero de inventario", value: data.numeroInventario },
    { label: "Estado de conservacion", value: enumLabel(data.estadoConservacion) },
    { label: "Ubicacion actual", value: data.ubicacionNombre },
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
    { label: "Régimen de propiedad", value: enumLabel(data.regimenPropiedad) },
    { label: "Intervenciones inadecuadas", value: enumLabel(data.intervencionesInadecuadas) },
    { label: "Estado de integridad", value: enumLabel(data.estadoIntegridad) },
    { label: "Descripcion", value: data.descripcion, wide: true }
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
        <PageHeader
          actions={
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
                <Link
                  className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                  href={`/objetos/${data.id}/relaciones`}
                >
                  Ver relaciones
                </Link>
              ) : null}
            </div>
          }
          description={data ? `Inventario ${data.numeroInventario}` : "Detalle completo del objeto patrimonial."}
          title={data?.denominacionObjeto || "Detalle de objeto"}
        />
        {isLoading ? <LoadingState label="Cargando objeto..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {data ? (
          <>
            <section className="grid gap-6 lg:grid-cols-[minmax(0,1.15fr)_minmax(360px,0.85fr)]">
              <GaleriaObjeto objeto={data} puedeEscribir={puedeEscribir} />

              <div className="rounded-lg border p-5">
                <h2 className="text-base font-semibold">Datos del objeto</h2>
                <dl className="mt-5 grid gap-5 text-sm sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
                  {datosDetalle.map((dato) => (
                    <ObjetoDato key={dato.label} {...dato} />
                  ))}
                </dl>
              </div>
            </section>

            {data.descripcionTecnica ? (
              <section className="rounded-lg border p-5">
                <h2 className="text-base font-semibold">Descripcion tecnica</h2>
                <p className="mt-3 whitespace-pre-wrap text-sm font-medium">{data.descripcionTecnica}</p>
              </section>
            ) : null}

            {[data.inscripciones, data.condicionLegalBien, detallesConservacion].some(Boolean) ? (
              <section className="rounded-lg border p-5">
                <h2 className="text-base font-semibold">Información descriptiva y legal</h2>
                <dl className="mt-5 grid gap-5 text-sm sm:grid-cols-2">
                  {data.inscripciones ? <ObjetoDato label="Inscripciones" value={data.inscripciones} wide /> : null}
                  {data.condicionLegalBien ? <ObjetoDato label="Condición legal del bien" value={data.condicionLegalBien} wide /> : null}
                  {detallesConservacion ? <ObjetoDato label="Detalles del estado de conservación" value={detallesConservacion} wide /> : null}
                </dl>
              </section>
            ) : null}

            {conservacionPreventiva.length > 0 ? (
              <section className="rounded-lg border p-5">
                <h2 className="text-base font-semibold">Conservación Preventiva</h2>
                <dl className="mt-5 grid gap-5 text-sm sm:grid-cols-2 lg:grid-cols-4">
                  {conservacionPreventiva.map((dato) => <ObjetoDato key={dato.label} {...dato} />)}
                </dl>
              </section>
            ) : null}
          </>
        ) : null}
        {data && puedeVerRecibos ? (
          <div className="rounded-lg border p-5">
            <h2 className="text-base font-semibold">Recibos</h2>
            <div className="mt-4 grid gap-3">
              {recibos.length === 0 ? <p className="text-sm text-muted-foreground">Sin recibos emitidos.</p> : null}
              {recibos.map((recibo) => (
                <div className="rounded-md border p-3 text-sm" key={recibo.id}>
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
            </div>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

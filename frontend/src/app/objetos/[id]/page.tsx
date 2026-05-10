"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { canWrite, useAuth } from "@/lib/auth";
import { ApiClientError } from "@/lib/errors/api-error";
import {
  descargarCopiaFirmadaRecibo,
  descargarFotoObjeto,
  descargarReciboPdf,
  subirCopiaFirmadaRecibo,
  subirFotoObjeto
} from "@/features/objetos/api";
import {
  objetosQueryKeys,
  useBajaLogicaObjetoMutation,
  useEliminarFotoObjetoMutation,
  useFotosObjetoQuery,
  useObjetoQuery,
  useRecibosObjetoQuery
} from "@/features/objetos/queries";
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

export default function DetalleObjetoPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const queryClient = useQueryClient();
  const [fotoDescripcion, setFotoDescripcion] = useState("");
  const { data, error, isError, isLoading } = useObjetoQuery(id);
  const { data: fotos = [] } = useFotosObjetoQuery(id);
  const { data: recibos = [] } = useRecibosObjetoQuery(id);
  const bajaMutation = useBajaLogicaObjetoMutation();
  const eliminarFotoMutation = useEliminarFotoObjetoMutation(id);
  const subirFotoMutation = useMutation({
    mutationFn: (archivo: File) => subirFotoObjeto(id, archivo, fotoDescripcion.trim() || undefined),
    onSuccess: () => {
      setFotoDescripcion("");
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.fotos(id) });
    }
  });
  const subirCopiaFirmadaMutation = useMutation({
    mutationFn: ({ reciboId, archivo }: { reciboId: number; archivo: File }) => subirCopiaFirmadaRecibo(reciboId, archivo),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: objetosQueryKeys.recibos(id) });
    }
  });

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
                <>
                  <Link
                    className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                    href={`/objetos/${data.id}/editar`}
                  >
                    Editar
                  </Link>
                  <button
                    className="rounded-md border border-destructive/40 px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 disabled:opacity-60"
                    disabled={bajaMutation.isPending}
                    onClick={() => {
                      if (window.confirm("Confirmar baja logica del objeto")) {
                        bajaMutation.mutate(data.id, {
                          onSuccess: () => router.push("/objetos")
                        });
                      }
                    }}
                    type="button"
                  >
                    {bajaMutation.isPending ? "Dando de baja..." : "Dar de baja"}
                  </button>
                </>
              ) : null}
            </div>
          }
          description="Detalle completo del objeto patrimonial."
          title="Detalle de objeto"
        />
        {isLoading ? <LoadingState label="Cargando objeto..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {bajaMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(bajaMutation.error)}
            requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined}
          />
        ) : null}
        {data ? (
          <div className="rounded-lg border p-5">
            <dl className="grid gap-5 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-muted-foreground">Numero de inventario</dt>
                <dd className="mt-1 font-medium">{data.numeroInventario}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Denominacion</dt>
                <dd className="mt-1 font-medium">{data.denominacionObjeto}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-muted-foreground">Descripcion</dt>
                <dd className="mt-1 whitespace-pre-wrap font-medium">{data.descripcion || "Sin descripcion"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Estado de conservacion</dt>
                <dd className="mt-1 font-medium">{data.estadoConservacion || "No especificado"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Categorias</dt>
                <dd className="mt-1 font-medium">{data.categorias?.map((categoria) => categoria.nombre).join(", ") || "Sin categorias"}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-muted-foreground">Descripcion tecnica</dt>
                <dd className="mt-1 whitespace-pre-wrap font-medium">{data.descripcionTecnica || "Sin descripcion tecnica"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Materiales</dt>
                <dd className="mt-1 whitespace-pre-wrap font-medium">{data.materiales || "No especificado"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Dimensiones</dt>
                <dd className="mt-1 whitespace-pre-wrap font-medium">{data.dimensiones || "No especificado"}</dd>
              </div>
            </dl>
          </div>
        ) : null}
        {data ? (
          <div className="rounded-lg border p-5">
            <h2 className="text-base font-semibold">Fotos</h2>
            {puedeEscribir ? (
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
            <div className="mt-4 grid gap-3">
              {fotos.length === 0 ? <p className="text-sm text-muted-foreground">Sin fotos adjuntas.</p> : null}
              {fotos.map((foto) => (
                <div className="flex flex-wrap items-center justify-between gap-3 rounded-md border p-3 text-sm" key={foto.id}>
                  <div>
                    <p className="font-medium">{foto.nombreArchivo}</p>
                    <p className="text-muted-foreground">{foto.descripcion || foto.contentType}</p>
                  </div>
                  <div className="flex gap-2">
                    <button className="rounded-md border px-3 py-1.5 hover:bg-muted" onClick={async () => descargarBlob(await descargarFotoObjeto(data.id, foto.id), foto.nombreArchivo)} type="button">Descargar</button>
                    {puedeEscribir ? (
                      <button className="rounded-md border px-3 py-1.5 text-destructive hover:bg-destructive/10" onClick={() => eliminarFotoMutation.mutate(foto.id)} type="button">Eliminar</button>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : null}
        {data ? (
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
                  {puedeEscribir ? (
                    <div className="mt-3">
                      <input
                        accept="application/pdf,image/jpeg,image/png,image/webp"
                        className="h-10 rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => {
                          const file = event.target.files?.[0];
                          if (file) {
                            subirCopiaFirmadaMutation.mutate({ reciboId: recibo.id, archivo: file });
                          }
                          event.currentTarget.value = "";
                        }}
                        type="file"
                      />
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

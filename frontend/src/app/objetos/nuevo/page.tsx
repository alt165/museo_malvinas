"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ObjetoMuseoForm, type ObjetoMuseoFormFiles } from "@/features/objetos/components/objeto-museo-form";
import { listarRecibosObjeto, subirFotosObjeto, subirReciboEscaneadoObjeto } from "@/features/objetos/api";
import { objetosQueryKeys, useCrearObjetoMutation } from "@/features/objetos/queries";
import { descargarReciboIngresoPdf } from "@/features/objetos/recibos";
import type { ObjetoMuseoRequestDTO, ObjetoMuseoResponseDTO, ReciboIngresoObjetoResponseDTO } from "@/features/objetos/types";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevoObjetoPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const mutation = useCrearObjetoMutation();
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [createdObjectId, setCreatedObjectId] = useState<number | null>(null);
  const [resultado, setResultado] = useState<{ objeto: ObjetoMuseoResponseDTO; recibo: ReciboIngresoObjetoResponseDTO } | null>(null);

  async function handleSubmit(payload: ObjetoMuseoRequestDTO, archivos: ObjetoMuseoFormFiles) {
    setUploadError(null);
    setDownloadError(null);
    setResultado(null);
    setCreatedObjectId(null);
    mutation.mutate(payload, {
      onSuccess: async (objeto) => {
        setCreatedObjectId(objeto.id);
        try {
          if (archivos.fotos.length > 0) {
            await subirFotosObjeto(objeto.id, archivos.fotos);
          }
          if (archivos.reciboEscaneado) {
            await subirReciboEscaneadoObjeto(objeto.id, archivos.reciboEscaneado);
          }
          const recibos = await listarRecibosObjeto(objeto.id);
          const recibo = [...recibos].sort((a, b) => b.id - a.id)[0];
          if (!recibo) {
            throw new Error("Recibo no encontrado para el objeto creado");
          }
          await queryClient.invalidateQueries({ queryKey: objetosQueryKeys.detail(objeto.id) });
          await queryClient.invalidateQueries({ queryKey: objetosQueryKeys.fotos(objeto.id) });
          await queryClient.invalidateQueries({ queryKey: objetosQueryKeys.reciboEscaneado(objeto.id) });
          await queryClient.invalidateQueries({ queryKey: objetosQueryKeys.recibos(objeto.id) });
          setResultado({ objeto, recibo });
        } catch {
          setUploadError("El objeto fue creado, pero fallo la subida de uno o mas archivos. Podes reintentar la carga desde el detalle del objeto.");
        }
      }
    });
  }

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          description="Alta de un objeto patrimonial."
          title="Nuevo objeto"
        />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        {resultado ? (
          <div className="rounded-lg border p-5 text-sm">
            <p className="font-medium">Objeto creado: {resultado.objeto.numeroInventario}</p>
            <p className="mt-1 text-muted-foreground">Recibo emitido: {resultado.recibo.numeroRecibo}</p>
            <div className="mt-4 flex flex-wrap gap-2">
              <button className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" onClick={() => router.push(`/objetos/${resultado.objeto.id}`)} type="button">
                Ver objeto
              </button>
              <button
                className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                onClick={async () => {
                  setDownloadError(null);
                  try {
                    await descargarReciboIngresoPdf(resultado.recibo);
                  } catch {
                    setDownloadError("No se pudo descargar el recibo. Intentalo nuevamente.");
                  }
                }}
                type="button"
              >
                Descargar recibo
              </button>
            </div>
            {downloadError ? <p className="mt-3 text-sm text-destructive">{downloadError}</p> : null}
          </div>
        ) : null}
        {uploadError ? (
          <div className="rounded-lg border border-destructive/30 bg-surface p-5 shadow-sm">
            <h2 className="text-base font-semibold text-destructive">Carga parcial</h2>
            <p className="mt-2 text-sm text-muted-foreground">{uploadError}</p>
            {createdObjectId ? (
              <button className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" onClick={() => router.push(`/objetos/${createdObjectId}`)} type="button">
                Ir al objeto creado
              </button>
            ) : null}
          </div>
        ) : null}
        <ObjetoMuseoForm
          allowFileUploads
          isSubmitting={mutation.isPending}
          onSubmit={handleSubmit}
          submitError={mutation.error}
          submitLabel="Crear objeto"
        />
      </div>
    </AppShell>
  );
}

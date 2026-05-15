"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ObjetoMuseoForm, type ObjetoMuseoFormFiles } from "@/features/objetos/components/objeto-museo-form";
import { subirFotosObjeto, subirReciboEscaneadoObjeto } from "@/features/objetos/api";
import { objetosQueryKeys, useCrearObjetoMutation } from "@/features/objetos/queries";
import type { ObjetoMuseoRequestDTO } from "@/features/objetos/types";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevoObjetoPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const mutation = useCrearObjetoMutation();
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [createdObjectId, setCreatedObjectId] = useState<number | null>(null);

  async function handleSubmit(payload: ObjetoMuseoRequestDTO, archivos: ObjetoMuseoFormFiles) {
    setUploadError(null);
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
          await queryClient.invalidateQueries({ queryKey: objetosQueryKeys.detail(objeto.id) });
          await queryClient.invalidateQueries({ queryKey: objetosQueryKeys.fotos(objeto.id) });
          await queryClient.invalidateQueries({ queryKey: objetosQueryKeys.reciboEscaneado(objeto.id) });
          router.push(`/objetos/${objeto.id}`);
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

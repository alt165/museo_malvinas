"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useDepositantesQuery } from "@/features/depositantes/queries";
import { descargarReciboPdf } from "@/features/objetos/api";
import { cargaRapidaObjetoSchema, type CargaRapidaObjetoFormValues } from "@/features/objetos/schemas";
import type { CargaRapidaObjetoResponseDTO } from "@/features/objetos/types";
import { getApiErrorMessage, getValidationErrors } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";
import { useCargaRapidaObjetoMutation } from "@/features/objetos/queries";
import { useEffect, useState } from "react";

function abrirBlob(blob: Blob, nombre: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = nombre;
  link.click();
  URL.revokeObjectURL(url);
}

export default function CargaRapidaObjetoPage() {
  const [resultado, setResultado] = useState<CargaRapidaObjetoResponseDTO | null>(null);
  const { data: depositantes = [], isLoading: isLoadingDepositantes } = useDepositantesQuery();
  const mutation = useCargaRapidaObjetoMutation();
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError
  } = useForm<CargaRapidaObjetoFormValues>({
    resolver: zodResolver(cargaRapidaObjetoSchema),
    defaultValues: {
      depositanteId: 0,
      denominacionObjeto: "",
      numeroInventario: "",
      descripcionBreve: ""
    }
  });

  useEffect(() => {
    const validationErrors = getValidationErrors(mutation.error);
    Object.entries(validationErrors).forEach(([field, message]) => {
      if (field === "depositanteId" || field === "denominacionObjeto" || field === "numeroInventario" || field === "descripcionBreve") {
        setError(field, { message });
      }
    });
  }, [mutation.error, setError]);

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          actions={
            <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos">
              Volver
            </Link>
          }
          description="Ingreso minimo de un objeto y emision de recibo para el depositante."
          title="Carga rapida de objeto"
        />
        {isLoadingDepositantes ? <LoadingState label="Cargando depositantes..." /> : null}
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
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/objetos/${resultado.objeto.id}`}>
                Ver objeto
              </Link>
              <button
                className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                onClick={async () => abrirBlob(await descargarReciboPdf(resultado.recibo.id), `recibo-${resultado.recibo.id}.pdf`)}
                type="button"
              >
                Descargar recibo
              </button>
            </div>
          </div>
        ) : null}
        <form
          className="max-w-3xl space-y-5 rounded-lg border p-5"
          onSubmit={handleSubmit((values) =>
            mutation.mutate(
              {
                depositanteId: values.depositanteId,
                denominacionObjeto: values.denominacionObjeto.trim(),
                numeroInventario: values.numeroInventario.trim(),
                descripcionBreve: values.descripcionBreve.trim()
              },
              { onSuccess: setResultado }
            )
          )}
        >
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor="depositanteId">Depositante</label>
            <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="depositanteId" {...register("depositanteId", { valueAsNumber: true })}>
              <option value={0}>Seleccionar depositante</option>
              {depositantes.map((depositante) => (
                <option key={depositante.id} value={depositante.id}>{depositante.nombre}</option>
              ))}
            </select>
            {errors.depositanteId ? <p className="text-sm text-destructive">{errors.depositanteId.message}</p> : null}
          </div>
          <div className="grid gap-5 sm:grid-cols-2">
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="denominacionObjeto">Denominacion</label>
              <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="denominacionObjeto" {...register("denominacionObjeto")} />
              {errors.denominacionObjeto ? <p className="text-sm text-destructive">{errors.denominacionObjeto.message}</p> : null}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="numeroInventario">Numero de inventario</label>
              <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="numeroInventario" {...register("numeroInventario")} />
              {errors.numeroInventario ? <p className="text-sm text-destructive">{errors.numeroInventario.message}</p> : null}
            </div>
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor="descripcionBreve">Descripcion breve</label>
            <textarea className="min-h-28 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" id="descripcionBreve" {...register("descripcionBreve")} />
            {errors.descripcionBreve ? <p className="text-sm text-destructive">{errors.descripcionBreve.message}</p> : null}
          </div>
          <button className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-60" disabled={mutation.isPending} type="submit">
            {mutation.isPending ? "Generando..." : "Crear y generar recibo"}
          </button>
        </form>
      </div>
    </AppShell>
  );
}

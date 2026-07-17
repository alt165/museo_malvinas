"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { CheckCircle, Undo2 } from "lucide-react";
import { useForm } from "react-hook-form";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { RowActionButton, RowActions } from "@/components/common/row-actions";
import { EmptyState } from "@/components/common/empty-state";
import { useObjetosQuery } from "@/features/objetos/queries";
import { ApiClientError } from "@/lib/errors/api-error";
import {
  useAgregarObjetoAExhibicionMutation,
  useObjetosExhibicionQuery,
  useRevertirDevolucionMutation,
  useVerificarDevolucionMutation
} from "../queries";
import { agregarObjetoExhibicionSchema, type AgregarObjetoExhibicionFormValues } from "../schemas";
import type { EstadoExhibicion } from "../types";
import { formatDate, formatDateTime, getApiErrorMessage } from "../utils";

type ObjetosExhibicionPanelProps = {
  exhibicionId: number;
  estado: EstadoExhibicion;
  canWrite: boolean;
};

export function ObjetosExhibicionPanel({ canWrite, estado, exhibicionId }: ObjetosExhibicionPanelProps) {
  const objetosExhibicionQuery = useObjetosExhibicionQuery(exhibicionId);
  const objetosQuery = useObjetosQuery();
  const agregarMutation = useAgregarObjetoAExhibicionMutation(exhibicionId);
  const verificarMutation = useVerificarDevolucionMutation(exhibicionId);
  const revertirMutation = useRevertirDevolucionMutation(exhibicionId);
  const puedeAgregar = canWrite && estado !== "FINALIZADA";
  const {
    formState: { errors },
    handleSubmit,
    register,
    reset
  } = useForm<AgregarObjetoExhibicionFormValues>({
    resolver: zodResolver(agregarObjetoExhibicionSchema),
    defaultValues: {
      objetoMuseoId: 0,
      fechaInclusion: new Date().toISOString().slice(0, 10)
    }
  });

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-1">
        <h2 className="text-lg font-semibold">Objetos asociados</h2>
        <p className="text-sm text-muted-foreground">Objetos incluidos y estado de devolución.</p>
      </div>
      {puedeAgregar ? (
        <form
          className="grid gap-3 rounded-lg border p-4 md:grid-cols-[1fr_180px_auto]"
          onSubmit={handleSubmit((values) =>
            agregarMutation.mutate(
              {
                exhibicionId,
                objetoMuseoId: Number(values.objetoMuseoId),
                fechaInclusion: values.fechaInclusion,
                fechaRetiro: null,
                estado: "EN_EXHIBICION",
                devolucionVerificada: false,
                verificadoPorUsuarioId: null,
                fechaVerificacion: null,
                observacionesDevolucion: null
              },
              {
                onSuccess: () => reset({ objetoMuseoId: 0, fechaInclusion: new Date().toISOString().slice(0, 10) })
              }
            )
          )}
        >
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor="objetoMuseoId">
              Objeto
            </label>
            <select className="h-10 w-full rounded-md border bg-background px-3 text-sm" id="objetoMuseoId" {...register("objetoMuseoId", { valueAsNumber: true })}>
              <option value={0}>Seleccionar objeto</option>
              {(objetosQuery.data ?? []).map((objeto) => (
                <option key={objeto.id} value={objeto.id}>
                  {objeto.numeroInventario} - {objeto.denominacionObjeto}
                </option>
              ))}
            </select>
            {errors.objetoMuseoId ? <p className="text-sm text-destructive">{errors.objetoMuseoId.message}</p> : null}
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor="fechaInclusion">
              Inclusión
            </label>
            <input className="h-10 w-full rounded-md border bg-background px-3 text-sm" id="fechaInclusion" type="date" {...register("fechaInclusion")} />
            {errors.fechaInclusion ? <p className="text-sm text-destructive">{errors.fechaInclusion.message}</p> : null}
          </div>
          <div className="flex items-end">
            <button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground disabled:opacity-60" disabled={agregarMutation.isPending} type="submit">
              {agregarMutation.isPending ? "Agregando..." : "Agregar"}
            </button>
          </div>
        </form>
      ) : null}
      {agregarMutation.isError ? (
        <ErrorState
          message={getApiErrorMessage(agregarMutation.error)}
          requestId={agregarMutation.error instanceof ApiClientError ? agregarMutation.error.requestId : undefined}
        />
      ) : null}
      {verificarMutation.isError ? (
        <ErrorState
          message={getApiErrorMessage(verificarMutation.error)}
          requestId={verificarMutation.error instanceof ApiClientError ? verificarMutation.error.requestId : undefined}
        />
      ) : null}
      {revertirMutation.isError ? (
        <ErrorState
          message={getApiErrorMessage(revertirMutation.error)}
          requestId={revertirMutation.error instanceof ApiClientError ? revertirMutation.error.requestId : undefined}
        />
      ) : null}
      {objetosExhibicionQuery.isLoading ? <LoadingState label="Cargando objetos asociados..." /> : null}
      {objetosExhibicionQuery.isError ? (
        <ErrorState
          message={getApiErrorMessage(objetosExhibicionQuery.error)}
          requestId={objetosExhibicionQuery.error instanceof ApiClientError ? objetosExhibicionQuery.error.requestId : undefined}
        />
      ) : null}
      {objetosExhibicionQuery.data?.length === 0 ? (
        <EmptyState description="Todavía no hay objetos asociados a esta exhibición." title="Sin objetos asociados" />
      ) : null}
      {objetosExhibicionQuery.data && objetosExhibicionQuery.data.length > 0 ? (
        <div className="overflow-hidden rounded-lg border">
          <table className="w-full border-collapse text-sm">
            <thead className="bg-muted/60">
              <tr>
                <th className="px-4 py-3 text-left font-medium">Objeto</th>
                <th className="px-4 py-3 text-left font-medium">Estado</th>
                <th className="px-4 py-3 text-left font-medium">Inclusión</th>
                <th className="px-4 py-3 text-left font-medium">Devolución</th>
                <th className="px-4 py-3 text-left font-medium">Verificación</th>
                <th className="px-4 py-3 text-left font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {objetosExhibicionQuery.data.map((objeto) => (
                <tr className="border-t" key={objeto.id}>
                  <td className="px-4 py-3 font-medium">{objeto.objetoNombre}</td>
                  <td className="px-4 py-3">{objeto.estado}</td>
                  <td className="px-4 py-3">{formatDate(objeto.fechaInclusion)}</td>
                  <td className="px-4 py-3">{objeto.devolucionVerificada ? "Verificada" : "Pendiente"}</td>
                  <td className="px-4 py-3">{formatDateTime(objeto.fechaVerificacion)}</td>
                  <td className="px-4 py-3">
                    {canWrite && !objeto.devolucionVerificada ? (
                      <RowActions className="justify-start">
                        <RowActionButton
                          disabled={verificarMutation.isPending}
                          icon={CheckCircle}
                          label="Verificar devolución"
                          onClick={() => {
                            if (!window.confirm(`Confirmar verificación de devolución de "${objeto.objetoNombre}"`)) {
                              return;
                            }

                            const observaciones = window.prompt("Observaciones de devolución", objeto.observacionesDevolucion ?? "") ?? undefined;
                            verificarMutation.mutate({ id: objeto.id, observaciones });
                          }}
                        />
                      </RowActions>
                    ) : canWrite && objeto.devolucionVerificada ? (
                      <RowActions className="justify-start">
                        <RowActionButton
                          disabled={revertirMutation.isPending}
                          icon={Undo2}
                          label="Revertir devolución"
                          onClick={() => {
                            if (window.confirm(`Revertir devolución verificada de "${objeto.objetoNombre}"`)) {
                              revertirMutation.mutate(objeto.id);
                            }
                          }}
                        />
                      </RowActions>
                    ) : (
                      <span className="text-muted-foreground">Sin acciones</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

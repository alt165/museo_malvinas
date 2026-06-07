"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";
import {
  useAgregarObjetosColeccionMutation,
  useObjetosColeccionQuery,
  useObjetosSinColeccionQuery,
  useQuitarObjetoColeccionMutation
} from "../queries";
import { getApiErrorMessage } from "../utils";

type ObjetosColeccionPanelProps = {
  coleccionId: number;
};

export function ObjetosColeccionPanel({ coleccionId }: ObjetosColeccionPanelProps) {
  const { canEdit: puedeEscribir } = useEditingMode();
  const objetosQuery = useObjetosColeccionQuery(coleccionId);
  const disponiblesQuery = useObjetosSinColeccionQuery();
  const agregarMutation = useAgregarObjetosColeccionMutation(coleccionId);
  const quitarMutation = useQuitarObjetoColeccionMutation(coleccionId);
  const [seleccionados, setSeleccionados] = useState<number[]>([]);
  const disponibles = useMemo(() => disponiblesQuery.data ?? [], [disponiblesQuery.data]);

  function toggleObjeto(objetoId: number) {
    setSeleccionados((current) => current.includes(objetoId) ? current.filter((id) => id !== objetoId) : [...current, objetoId]);
  }

  function handleAgregar() {
    agregarMutation.mutate({ objetoIds: seleccionados }, { onSuccess: () => setSeleccionados([]) });
  }

  function handleQuitar(objetoId: number) {
    if (window.confirm("Quitar este objeto de la coleccion?")) {
      quitarMutation.mutate(objetoId);
    }
  }

  return (
    <div className="grid gap-6 xl:grid-cols-[1.2fr_1fr]">
      <section className="rounded-lg border p-5">
        <h2 className="text-base font-semibold">Objetos asociados</h2>
        {objetosQuery.isLoading ? <LoadingState label="Cargando objetos..." /> : null}
        {objetosQuery.isError ? <ErrorState message={getApiErrorMessage(objetosQuery.error)} requestId={objetosQuery.error instanceof ApiClientError ? objetosQuery.error.requestId : undefined} /> : null}
        {objetosQuery.data?.length === 0 ? <EmptyState description="La coleccion no tiene objetos asociados." title="Sin objetos" /> : null}
        {objetosQuery.data && objetosQuery.data.length > 0 ? (
          <div className="mt-4 overflow-hidden rounded-md border">
            <table className="w-full border-collapse text-sm">
              <thead className="bg-muted/60">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Inventario</th>
                  <th className="px-4 py-3 text-left font-medium">Denominacion</th>
                  {puedeEscribir ? <th className="px-4 py-3 text-right font-medium">Acciones</th> : null}
                </tr>
              </thead>
              <tbody>
                {objetosQuery.data.map((objeto) => (
                  <tr className="border-t" key={objeto.id}>
                    <td className="px-4 py-3 align-top font-medium">{objeto.numeroInventario}</td>
                    <td className="px-4 py-3 align-top">
                      <Link className="text-primary underline-offset-4 hover:underline" href={`/objetos/${objeto.id}`}>{objeto.denominacionObjeto}</Link>
                    </td>
                    {puedeEscribir ? (
                      <td className="px-4 py-3 align-top text-right">
                        <button className="rounded-md border px-3 py-1.5 text-xs text-destructive hover:bg-muted disabled:opacity-60" disabled={quitarMutation.isPending} onClick={() => handleQuitar(objeto.id)} type="button">
                          Quitar
                        </button>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
      {puedeEscribir ? (
        <section className="rounded-lg border p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-base font-semibold">Agregar objetos disponibles</h2>
            <span className="text-xs text-muted-foreground">{seleccionados.length} seleccionado(s)</span>
          </div>
          {disponiblesQuery.isLoading ? <LoadingState label="Cargando disponibles..." /> : null}
          {disponiblesQuery.isError ? <ErrorState message={getApiErrorMessage(disponiblesQuery.error)} requestId={disponiblesQuery.error instanceof ApiClientError ? disponiblesQuery.error.requestId : undefined} /> : null}
          {agregarMutation.isError ? <ErrorState message={getApiErrorMessage(agregarMutation.error)} requestId={agregarMutation.error instanceof ApiClientError ? agregarMutation.error.requestId : undefined} /> : null}
          {!disponiblesQuery.isLoading && disponibles.length === 0 ? <p className="mt-4 text-sm text-muted-foreground">No hay objetos sin coleccion.</p> : null}
          {disponibles.length > 0 ? (
            <div className="mt-4 max-h-96 overflow-y-auto rounded-md border">
              {disponibles.map((objeto) => (
                <label className="flex items-start gap-3 border-b px-3 py-2 text-sm last:border-b-0 hover:bg-muted/50" key={objeto.id}>
                  <input checked={seleccionados.includes(objeto.id)} className="mt-1 h-4 w-4 accent-primary" onChange={() => toggleObjeto(objeto.id)} type="checkbox" />
                  <span>
                    <span className="block font-medium">{objeto.numeroInventario}</span>
                    <span className="block text-muted-foreground">{objeto.denominacionObjeto}</span>
                  </span>
                </label>
              ))}
            </div>
          ) : null}
          <button
            className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={seleccionados.length === 0 || agregarMutation.isPending}
            onClick={handleAgregar}
            type="button"
          >
            {agregarMutation.isPending ? "Agregando..." : "Agregar a coleccion"}
          </button>
        </section>
      ) : null}
    </div>
  );
}

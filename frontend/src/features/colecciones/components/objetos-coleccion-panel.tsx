"use client";

import Link from "next/link";
import { PackageOpen } from "lucide-react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { ApiClientError } from "@/lib/errors/api-error";
import { useObjetosColeccionQuery } from "../queries";
import { getApiErrorMessage } from "../utils";

type ObjetosColeccionPanelProps = {
  coleccionId: number;
};

export function ObjetosColeccionPanel({ coleccionId }: ObjetosColeccionPanelProps) {
  const objetosQuery = useObjetosColeccionQuery(coleccionId);

  return (
    <section className="rounded-lg border bg-white p-5 shadow-sm">
      <div className="flex items-center gap-2 border-b pb-3">
        <PackageOpen className="h-5 w-5 text-primary" />
        <h2 className="text-base font-bold text-primary">Objetos asociados</h2>
      </div>
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
              </tr>
            </thead>
            <tbody>
              {objetosQuery.data.map((objeto) => (
                <tr className="border-t" key={objeto.id}>
                  <td className="px-4 py-3 align-top font-medium">{objeto.numeroInventario}</td>
                  <td className="px-4 py-3 align-top">
                    <Link className="text-primary underline-offset-4 hover:underline" href={`/objetos/${objeto.id}`}>{objeto.denominacionObjeto}</Link>
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

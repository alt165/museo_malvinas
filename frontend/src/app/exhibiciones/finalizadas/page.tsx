
"use client";

import Link from "next/link";
import { useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useExhibicionesFinalizadasQuery } from "@/features/exhibiciones/queries";
import { formatDate, getApiErrorMessage } from "@/features/exhibiciones/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function ExhibicionesFinalizadasPage() {
  const [texto, setTexto] = useState("");
  const [textoAplicado, setTextoAplicado] = useState("");
  const { data, error, isError, isFetching, isLoading } = useExhibicionesFinalizadasQuery({ texto: textoAplicado, page: 0, size: 50 }, true);
  const exhibiciones = data?.content ?? [];

  function buscar() {
    setTextoAplicado(texto.trim());
  }

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          actions={
            <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/exhibiciones">
              Volver
            </Link>
          }
          description="Seleccione una exhibición finalizada para crear una nueva repetición."
          title="Exhibiciones finalizadas"
        />

        <section className="rounded-lg border p-4">
          <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
            <input
              className="h-10 rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
              onChange={(event) => setTexto(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  buscar();
                }
              }}
              placeholder="Buscar exhibición finalizada"
              value={texto}
            />
            <button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90" onClick={buscar} type="button">
              Buscar
            </button>
          </div>
        </section>

        {isLoading || isFetching ? <LoadingState label="Cargando exhibiciones finalizadas..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {!isLoading && !isError && exhibiciones.length === 0 ? <EmptyState description="No hay exhibiciones finalizadas para repetir." title="Sin exhibiciones finalizadas" /> : null}
        {!isLoading && !isError && exhibiciones.length > 0 ? (
          <div className="overflow-hidden rounded-lg border">
            <table className="w-full border-collapse text-sm">
              <thead className="bg-muted/60">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Nombre</th>
                  <th className="px-4 py-3 text-left font-medium">Tipo</th>
                  <th className="px-4 py-3 text-left font-medium">Periodo</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {exhibiciones.map((exhibicion) => (
                  <tr className="border-t" key={exhibicion.id}>
                    <td className="px-4 py-3 align-top font-medium">{exhibicion.nombre}</td>
                    <td className="px-4 py-3 align-top">{exhibicion.tipo}</td>
                    <td className="px-4 py-3 align-top">{formatDate(exhibicion.fechaInicio)} - {formatDate(exhibicion.fechaFin)}</td>
                    <td className="px-4 py-3 align-top">
                      <div className="flex justify-end gap-2">
                        <Link className="inline-flex h-8 items-center rounded-md border px-2 text-xs hover:bg-muted" href={`/exhibiciones/${exhibicion.id}`}>
                          Ver
                        </Link>
                        <Link className="inline-flex h-8 items-center rounded-md border px-2 text-xs hover:bg-muted" href={`/exhibiciones/repetir/${exhibicion.id}`}>
                          Repetir
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

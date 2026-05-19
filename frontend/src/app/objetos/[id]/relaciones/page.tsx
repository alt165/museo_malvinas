"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useObjetoQuery } from "@/features/objetos/queries";
import { getApiErrorMessage as getObjetoApiErrorMessage } from "@/features/objetos/utils";
import { ObjetoRelacionesGraph } from "@/features/relaciones-objetos/components/ObjetoRelacionesGraph";
import { useRelacionesPorObjetoQuery } from "@/features/relaciones-objetos/queries";
import { getApiErrorMessage } from "@/features/relaciones-objetos/utils";
import { canWrite, useAuth } from "@/lib/auth";
import { ApiClientError } from "@/lib/errors/api-error";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function RelacionesObjetoPage() {
  const params = useParams<{ id: string }>();
  const id = getParamId(params.id);
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const [vista, setVista] = useState<"tabla" | "grafo">(() => {
    if (typeof window === "undefined") {
      return "grafo";
    }

    return new URLSearchParams(window.location.search).get("view") === "table" ? "tabla" : "grafo";
  });
  const [profundidad, setProfundidad] = useState(1);
  const objetoQuery = useObjetoQuery(id);
  const relacionesQuery = useRelacionesPorObjetoQuery(id);
  const relaciones = relacionesQuery.data ?? [];

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            <div className="flex flex-wrap gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/objetos/${id}`}>
                Volver
              </Link>
              {puedeEscribir ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/relaciones-objetos/nueva?origenId=${id}`}>
                  Nueva relacion
                </Link>
              ) : null}
            </div>
          }
          description={objetoQuery.data ? `${objetoQuery.data.numeroInventario} - ${objetoQuery.data.denominacionObjeto}` : "Relaciones del objeto."}
          title="Relaciones del objeto"
        />
        {objetoQuery.isLoading || relacionesQuery.isLoading ? <LoadingState label="Cargando relaciones..." /> : null}
        {objetoQuery.isError ? (
          <ErrorState
            message={getObjetoApiErrorMessage(objetoQuery.error)}
            requestId={objetoQuery.error instanceof ApiClientError ? objetoQuery.error.requestId : undefined}
          />
        ) : null}
        {relacionesQuery.isError ? (
          <ErrorState
            message={getApiErrorMessage(relacionesQuery.error)}
            requestId={relacionesQuery.error instanceof ApiClientError ? relacionesQuery.error.requestId : undefined}
          />
        ) : null}

        <div className="inline-flex rounded-md border bg-white p-1">
          <button
            className={
              vista === "tabla"
                ? "rounded bg-primary px-3 py-1.5 text-sm font-medium text-white"
                : "rounded px-3 py-1.5 text-sm font-medium hover:bg-muted"
            }
            onClick={() => setVista("tabla")}
            type="button"
          >
            Vista tabla
          </button>
          <button
            className={
              vista === "grafo"
                ? "rounded bg-primary px-3 py-1.5 text-sm font-medium text-white"
                : "rounded px-3 py-1.5 text-sm font-medium hover:bg-muted"
            }
            onClick={() => setVista("grafo")}
            type="button"
          >
            Vista grafo
          </button>
        </div>

        {vista === "tabla" && !relacionesQuery.isLoading && !relacionesQuery.isError && relaciones.length === 0 ? (
          <EmptyState description="El objeto no tiene relaciones registradas." title="Sin relaciones" />
        ) : null}
        {vista === "tabla" && !relacionesQuery.isLoading && !relacionesQuery.isError && relaciones.length > 0 ? (
          <div className="overflow-hidden rounded-lg border">
            <table className="w-full border-collapse text-sm">
              <thead className="bg-primary text-primary-foreground">
                <tr>
                  <th className="px-4 py-3 text-left font-semibold text-white">Direccion</th>
                  <th className="px-4 py-3 text-left font-semibold text-white">Objeto relacionado</th>
                  <th className="px-4 py-3 text-left font-semibold text-white">Tipo</th>
                  <th className="px-4 py-3 text-left font-semibold text-white">Descripcion</th>
                  <th className="px-4 py-3 text-left font-semibold text-white">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {relaciones.map((relacion) => {
                  const relacionado =
                    relacion.direccion === "SALIENTE"
                      ? {
                          id: relacion.objetoDestinoId,
                          inventario: relacion.objetoDestinoNumeroInventario,
                          nombre: relacion.objetoDestinoNombre
                        }
                      : {
                          id: relacion.objetoOrigenId,
                          inventario: relacion.objetoOrigenNumeroInventario,
                          nombre: relacion.objetoOrigenNombre
                        };

                  return (
                    <tr className="border-t" key={relacion.idRelacion}>
                      <td className="px-4 py-3 align-top font-medium">{relacion.direccion}</td>
                      <td className="px-4 py-3 align-top">
                        <Link className="text-primary underline-offset-4 hover:underline" href={`/objetos/${relacionado.id}`}>
                          {relacionado.inventario} - {relacionado.nombre}
                        </Link>
                      </td>
                      <td className="px-4 py-3 align-top">{relacion.tipoRelacion}</td>
                      <td className="px-4 py-3 align-top text-muted-foreground">{relacion.descripcion || "Sin descripcion"}</td>
                      <td className="px-4 py-3 align-top">
                        <Link className="rounded-md border px-3 py-1.5 text-xs hover:bg-muted" href={`/relaciones-objetos/${relacion.idRelacion}`}>
                          Ver
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : null}
        {vista === "grafo" ? (
          <ObjetoRelacionesGraph
            objeto={objetoQuery.data}
            objetoId={id}
            onBackToTable={() => setVista("tabla")}
            onProfundidadChange={setProfundidad}
            profundidad={profundidad}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

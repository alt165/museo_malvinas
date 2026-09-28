"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { Presentation } from "lucide-react";
import type { ReactNode } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { AppShell } from "@/components/layout/app-shell";
import { ObjetosExhibicionPanel } from "@/features/exhibiciones/components/objetos-exhibicion-panel";
import { useCancelarExhibicionMutation, useExhibicionQuery, useFinalizarExhibicionMutation } from "@/features/exhibiciones/queries";
import { formatDate, getApiErrorMessage } from "@/features/exhibiciones/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function DetalleExhibicionPage() {
  const params = useParams<{ id: string }>();
  const id = getParamId(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const { data, error, isError, isLoading } = useExhibicionQuery(id);
  const finalizarMutation = useFinalizarExhibicionMutation(id);
  const cancelarMutation = useCancelarExhibicionMutation(id);
  const hoy = new Date().toISOString().slice(0, 10);

  return (
    <AppShell>
      <div className="space-y-6">
        <div className="rounded-lg border bg-white p-2 shadow-sm">
          <div className="flex flex-col gap-4 p-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <h1 className="text-2xl font-semibold tracking-normal text-primary">{data?.nombre ?? "Exhibición"}</h1>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/exhibiciones">
                Volver
              </Link>
              {puedeEscribir && data ? (
                <>
                  <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/exhibiciones/${data.id}/editar`}>
                    Editar
                  </Link>
                  {data.estado === "PLANIFICADA" && data.fechaInicio > hoy ? (
                    <button
                      className="rounded-md border px-4 py-2 text-sm font-medium text-destructive hover:bg-muted disabled:opacity-60"
                      disabled={cancelarMutation.isPending}
                      onClick={() => {
                        if (window.confirm("¿Confirma que desea cancelar esta exhibición? Los objetos asociados quedarán disponibles.")) {
                          cancelarMutation.mutate();
                        }
                      }}
                      type="button"
                    >
                      {cancelarMutation.isPending ? "Cancelando..." : "Cancelar exhibición"}
                    </button>
                  ) : null}
                  {data.estado === "FINALIZADA" ? (
                    <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/exhibiciones/repetir/${data.id}`}>
                      Repetir exhibición
                    </Link>
                  ) : data.estado !== "CANCELADA" ? (
                    <button
                      className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-60"
                      disabled={finalizarMutation.isPending}
                      onClick={() => {
                        if (window.confirm("Confirmar finalización de la exhibición")) {
                          finalizarMutation.mutate();
                        }
                      }}
                      type="button"
                    >
                      {finalizarMutation.isPending ? "Finalizando..." : "Finalizar"}
                    </button>
                  ) : null}
                </>
              ) : null}
            </div>
          </div>
        </div>
        {isLoading ? <LoadingState label="Cargando exhibición..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {finalizarMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(finalizarMutation.error)}
            requestId={finalizarMutation.error instanceof ApiClientError ? finalizarMutation.error.requestId : undefined}
          />
        ) : null}
        {cancelarMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(cancelarMutation.error)}
            requestId={cancelarMutation.error instanceof ApiClientError ? cancelarMutation.error.requestId : undefined}
          />
        ) : null}
        {data ? (
          <>
            <section className="rounded-lg border bg-white p-5 shadow-sm">
              <SectionHeading icon={Presentation} title="Datos de la exhibición" />
              <div className="grid gap-5 pt-4 sm:grid-cols-2 sm:items-start">
                <InlineInfo label="Nombre" value={data.nombre} />
                <InlineInfo label="Estado" value={<span className={estadoBadgeClass(data.estado)}>{data.estado}</span>} />
                <InlineInfo label="Tipo" value={formatearTipo(data.tipo)} />
                <InlineInfo label="Período" value={`${formatDate(data.fechaInicio)} - ${formatDate(data.fechaFin)}`} />
              </div>
              <div className="mt-5 border-t pt-4">
                <Info label="Descripción" value={data.descripcion || "Sin descripción"} />
              </div>
            </section>
            <ObjetosExhibicionPanel canWrite={puedeEscribir} estado={data.estado} exhibicionId={data.id} />
          </>
        ) : null}
      </div>
    </AppShell>
  );
}

function estadoBadgeClass(estado: string) {
  switch (estado) {
    case "ACTIVA":
      return "inline-flex rounded-md border border-green-200 bg-green-100 px-2.5 py-1 text-sm font-semibold text-green-900";
    case "FINALIZADA":
      return "inline-flex rounded-md border border-red-200 bg-red-100 px-2.5 py-1 text-sm font-semibold text-red-900";
    default:
      return "inline-flex rounded-md border border-slate-200 bg-slate-100 px-2.5 py-1 text-sm font-semibold text-slate-800";
  }
}

function formatearTipo(tipo: string) {
  return tipo.charAt(0).toUpperCase() + tipo.slice(1).toLowerCase();
}

function InlineInfo({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="grid min-w-0 grid-cols-[5.5rem_minmax(0,1fr)] items-baseline gap-x-3 gap-y-1">
      <p className="text-sm tracking-wide text-muted-foreground">{label}</p>
      <div className="min-w-0 break-words text-base font-medium text-primary">{value}</div>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-sm tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-1 whitespace-pre-wrap break-words text-base font-medium text-primary">{value}</p>
    </div>
  );
}

function SectionHeading({ icon: Icon, title }: { icon: typeof Presentation; title: string }) {
  return (
    <div className="flex items-center gap-2 border-b pb-3">
      <Icon className="h-5 w-5 text-primary" />
      <h2 className="text-base font-bold text-primary">{title}</h2>
    </div>
  );
}

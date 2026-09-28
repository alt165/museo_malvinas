"use client";

import Link from "next/link";
import { ArrowLeft, Pencil, Trash2 } from "lucide-react";
import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { AppShell } from "@/components/layout/app-shell";
import { VeteranoDetailPanels } from "@/features/veteranos/components/veterano-detail-panels";
import { VeteranoMultimediaPanel } from "@/features/veteranos/components/veterano-multimedia-panel";
import { useBajaLogicaVeteranoMutation, useVeteranoQuery } from "@/features/veteranos/queries";
import { getApiErrorMessage } from "@/features/veteranos/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

function clasificacionVisualFuerza(fuerza?: string | null) {
  const fuerzaNormalizada = fuerza
    ?.trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replaceAll("_", " ")
    .replace(/\s+/g, " ")
    .toUpperCase();

  if (fuerzaNormalizada === "CIVIL") return "Civil";
  if (["EJERCITO", "ARMADA", "FUERZA AEREA", "PREFECTURA", "GENDARMERIA"].includes(fuerzaNormalizada ?? "")) return "Veterano";
  return undefined;
}

export default function DetalleVeteranoPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const { data, error, isError, isLoading } = useVeteranoQuery(id);
  const bajaMutation = useBajaLogicaVeteranoMutation();
  const clasificacionFuerza = clasificacionVisualFuerza(data?.fuerza);

  function handleDelete() {
    if (window.confirm("Dar de baja este veterano?")) {
      bajaMutation.mutate(id, { onSuccess: () => router.push("/veteranos") });
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <header className="flex flex-col gap-4 rounded-lg border border-primary/15 bg-white p-5 shadow-sm sm:flex-row sm:items-start sm:justify-between">
          <div className="flex min-w-0 flex-wrap items-center gap-3">
            <h1 className="break-words text-2xl font-semibold tracking-normal text-primary">{data?.nombreCompleto || "Detalle de persona"}</h1>
            {clasificacionFuerza ? (
              <span className="rounded-md border border-sky-300 bg-sky-200 px-3 py-1 text-xs font-semibold text-sky-950 shadow-sm">
                {clasificacionFuerza}
              </span>
            ) : null}
          </div>
          <div className="flex shrink-0 flex-wrap items-center gap-2">
            <Link className="inline-flex items-center gap-2 rounded-md border bg-background px-4 py-2 text-sm font-medium hover:bg-muted" href="/veteranos"><ArrowLeft className="h-4 w-4" aria-hidden="true" />Volver</Link>
            {puedeEscribir && data ? <Link className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm hover:bg-primary/90" href={`/veteranos/${data.id}/editar`}><Pencil className="h-4 w-4" aria-hidden="true" />Editar</Link> : null}
            {puedeEscribir && data ? <button className="inline-flex items-center gap-2 rounded-md border border-destructive/40 bg-background px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 disabled:opacity-60" disabled={bajaMutation.isPending} onClick={handleDelete} type="button"><Trash2 className="h-4 w-4" aria-hidden="true" />Baja</button> : null}
          </div>
        </header>
        {isLoading ? <LoadingState label="Cargando veterano..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined} /> : null}
        {data ? <><VeteranoMultimediaPanel canWrite={puedeEscribir} veterano={data} /><VeteranoDetailPanels canWrite={puedeEscribir} veterano={data} /></> : null}
      </div>
    </AppShell>
  );
}

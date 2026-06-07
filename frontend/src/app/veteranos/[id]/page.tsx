"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { VeteranoDetailPanels } from "@/features/veteranos/components/veterano-detail-panels";
import { useBajaLogicaVeteranoMutation, useVeteranoQuery } from "@/features/veteranos/queries";
import { formatDate, getApiErrorMessage } from "@/features/veteranos/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function DetalleVeteranoPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const { data, error, isError, isLoading } = useVeteranoQuery(id);
  const bajaMutation = useBajaLogicaVeteranoMutation();

  function handleDelete() {
    if (window.confirm("Dar de baja este veterano?")) {
      bajaMutation.mutate(id, { onSuccess: () => router.push("/veteranos") });
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader title="Detalle de veterano" description="Datos personales, actuaciones y objetos asociados." actions={<div className="flex gap-2"><Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/veteranos">Volver</Link>{puedeEscribir && data ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/veteranos/${data.id}/editar`}>Editar</Link> : null}{puedeEscribir && data ? <button className="rounded-md border px-4 py-2 text-sm font-medium text-destructive hover:bg-muted disabled:opacity-60" disabled={bajaMutation.isPending} onClick={handleDelete} type="button">Baja</button> : null}</div>} />
        {isLoading ? <LoadingState label="Cargando veterano..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined} /> : null}
        {data ? <><div className="rounded-lg border p-5"><dl className="grid gap-5 text-sm sm:grid-cols-2"><div><dt className="text-muted-foreground">Nombre completo</dt><dd className="font-medium">{data.nombreCompleto}</dd></div><div><dt className="text-muted-foreground">Fuerza</dt><dd className="font-medium">{data.fuerza}</dd></div><div><dt className="text-muted-foreground">Nacimiento</dt><dd className="font-medium">{formatDate(data.fechaNacimiento)}</dd></div><div><dt className="text-muted-foreground">Fallecimiento</dt><dd className="font-medium">{formatDate(data.fechaFallecimiento)}</dd></div><div className="sm:col-span-2"><dt className="text-muted-foreground">Historia</dt><dd className="whitespace-pre-wrap font-medium">{data.historia || "Sin historia"}</dd></div></dl></div><VeteranoDetailPanels canWrite={puedeEscribir} veteranoId={data.id} /></> : null}
      </div>
    </AppShell>
  );
}

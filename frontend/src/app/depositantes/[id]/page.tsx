"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useBajaLogicaDepositanteMutation, useDepositanteQuery } from "@/features/depositantes/queries";
import { getApiErrorMessage, resumenObservaciones, telefonoVisible } from "@/features/depositantes/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function DetalleDepositantePage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const { data, error, isError, isLoading } = useDepositanteQuery(id);
  const bajaMutation = useBajaLogicaDepositanteMutation();

  function handleDelete() {
    if (window.confirm("Dar de baja este depositante?")) {
      bajaMutation.mutate(id, { onSuccess: () => router.push("/depositantes") });
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={<div className="flex gap-2"><Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/depositantes">Volver</Link>{puedeEscribir && data ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/depositantes/${data.id}/editar`}>Editar</Link> : null}{puedeEscribir && data ? <button className="rounded-md border px-4 py-2 text-sm font-medium text-destructive hover:bg-muted disabled:opacity-60" disabled={bajaMutation.isPending} onClick={handleDelete} type="button">Baja</button> : null}</div>}
          description="Datos del depositante."
          title="Detalle de depositante"
        />
        {isLoading ? <LoadingState label="Cargando depositante..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined} /> : null}
        {data ? (
          <div className="rounded-lg border p-5">
            <dl className="grid gap-5 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-muted-foreground">Tipo</dt>
                <dd className="font-medium">{data.tipo}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Nombre / organizacion</dt>
                <dd className="font-medium">{data.nombre}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Email</dt>
                <dd className="font-medium">{data.contacto || "Sin email"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Telefono</dt>
                <dd className="font-medium">{telefonoVisible(data)}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-muted-foreground">Observaciones</dt>
                <dd className="whitespace-pre-wrap font-medium">{resumenObservaciones(data.observaciones)}</dd>
              </div>
            </dl>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

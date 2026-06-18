"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useBajaLogicaRelacionObjetoMutation, useRelacionObjetoQuery } from "@/features/relaciones-objetos/queries";
import { getApiErrorMessage } from "@/features/relaciones-objetos/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function DetalleRelacionObjetoPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const { data, error, isError, isLoading } = useRelacionObjetoQuery(id);
  const bajaMutation = useBajaLogicaRelacionObjetoMutation();

  function handleDelete() {
    if (window.confirm("Dar de baja esta relacion entre objetos?")) {
      bajaMutation.mutate(id, { onSuccess: () => router.push("/relaciones-objetos") });
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={<div className="flex gap-2"><Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/relaciones-objetos">Volver</Link>{data ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/objetos/${data.objetoOrigenId}`}>Objeto origen</Link> : null}{data ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/objetos/${data.objetoDestinoId}`}>Objeto destino</Link> : null}{puedeEscribir && data ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/relaciones-objetos/${data.id}/editar`}>Editar</Link> : null}{puedeEscribir && data ? <button className="rounded-md border px-4 py-2 text-sm font-medium text-destructive hover:bg-muted disabled:opacity-60" disabled={bajaMutation.isPending} onClick={handleDelete} type="button">Baja</button> : null}</div>}
          description="Datos de la relacion entre objetos."
          title="Detalle de relacion"
        />
        {isLoading ? <LoadingState label="Cargando relacion..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined} /> : null}
        {data ? (
          <div className="rounded-lg border p-5">
            <dl className="grid gap-5 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-muted-foreground">Objeto origen</dt>
                <dd className="font-medium">{data.objetoOrigenNombre}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Objeto destino</dt>
                <dd className="font-medium">{data.objetoDestinoNombre}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Tipo de relacion</dt>
                <dd className="font-medium">{data.tipoRelacion}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-muted-foreground">Descripcion</dt>
                <dd className="whitespace-pre-wrap font-medium">{data.descripcion || "Sin descripcion"}</dd>
              </div>
            </dl>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

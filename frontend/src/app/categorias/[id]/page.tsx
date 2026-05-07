"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useBajaLogicaCategoriaMutation, useCategoriaQuery } from "@/features/categorias/queries";
import { getApiErrorMessage } from "@/features/categorias/utils";
import { canWrite, useAuth } from "@/lib/auth";
import { ApiClientError } from "@/lib/errors/api-error";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function DetalleCategoriaPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const { data, error, isError, isLoading } = useCategoriaQuery(id);
  const bajaMutation = useBajaLogicaCategoriaMutation();

  function handleDelete() {
    if (window.confirm("Dar de baja esta categoria?")) {
      bajaMutation.mutate(id, { onSuccess: () => router.push("/categorias") });
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={<div className="flex gap-2"><Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/categorias">Volver</Link>{puedeEscribir && data ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/categorias/${data.id}/editar`}>Editar</Link> : null}{puedeEscribir && data ? <button className="rounded-md border px-4 py-2 text-sm font-medium text-destructive hover:bg-muted disabled:opacity-60" disabled={bajaMutation.isPending} onClick={handleDelete} type="button">Baja</button> : null}</div>}
          description="Datos de la categoria."
          title="Detalle de categoria"
        />
        {isLoading ? <LoadingState label="Cargando categoria..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined} /> : null}
        {data ? (
          <div className="rounded-lg border p-5">
            <dl className="grid gap-5 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-muted-foreground">Nombre</dt>
                <dd className="font-medium">{data.nombre}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Activo</dt>
                <dd className="font-medium">{data.activo === false ? "No" : "Si"}</dd>
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

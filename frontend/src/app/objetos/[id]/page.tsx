"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { canWrite, useAuth } from "@/lib/auth";
import { ApiClientError } from "@/lib/errors/api-error";
import { useBajaLogicaObjetoMutation, useObjetoQuery } from "@/features/objetos/queries";
import { getApiErrorMessage } from "@/features/objetos/utils";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function DetalleObjetoPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const { data, error, isError, isLoading } = useObjetoQuery(id);
  const bajaMutation = useBajaLogicaObjetoMutation();

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            <div className="flex gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos">
                Volver
              </Link>
              {puedeEscribir && data ? (
                <>
                  <Link
                    className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                    href={`/objetos/${data.id}/editar`}
                  >
                    Editar
                  </Link>
                  <button
                    className="rounded-md border border-destructive/40 px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 disabled:opacity-60"
                    disabled={bajaMutation.isPending}
                    onClick={() => {
                      if (window.confirm("Confirmar baja logica del objeto")) {
                        bajaMutation.mutate(data.id, {
                          onSuccess: () => router.push("/objetos")
                        });
                      }
                    }}
                    type="button"
                  >
                    {bajaMutation.isPending ? "Dando de baja..." : "Dar de baja"}
                  </button>
                </>
              ) : null}
            </div>
          }
          description="Detalle completo del objeto patrimonial."
          title="Detalle de objeto"
        />
        {isLoading ? <LoadingState label="Cargando objeto..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {bajaMutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(bajaMutation.error)}
            requestId={bajaMutation.error instanceof ApiClientError ? bajaMutation.error.requestId : undefined}
          />
        ) : null}
        {data ? (
          <div className="rounded-lg border p-5">
            <dl className="grid gap-5 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-muted-foreground">Numero de inventario</dt>
                <dd className="mt-1 font-medium">{data.numeroInventario}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Nombre</dt>
                <dd className="mt-1 font-medium">{data.nombre}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Tipo de objeto</dt>
                <dd className="mt-1 font-medium">{data.tipoObjeto || "No especificado"}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-muted-foreground">Descripcion</dt>
                <dd className="mt-1 whitespace-pre-wrap font-medium">{data.descripcion || "Sin descripcion"}</dd>
              </div>
            </dl>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

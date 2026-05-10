"use client";

import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { canWrite, useAuth } from "@/lib/auth";
import { useObjetosQuery } from "@/features/objetos/queries";
import { ObjetosTable } from "@/features/objetos/components/objetos-table";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";

export default function ObjetosPage() {
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const { data = [], error, isError, isLoading } = useObjetosQuery();

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            puedeEscribir ? (
              <div className="flex flex-wrap gap-2">
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos/carga-rapida">
                  Carga rapida
                </Link>
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos/nuevo">
                  Nuevo objeto
                </Link>
              </div>
            ) : null
          }
          description="Catalogo de objetos patrimoniales registrados en el museo."
          title="Objetos del museo"
        />
        {isLoading ? <LoadingState label="Cargando objetos..." /> : null}
        {isError ? (
          <ErrorState
            message={getApiErrorMessage(error)}
            requestId={error instanceof ApiClientError ? error.requestId : undefined}
          />
        ) : null}
        {!isLoading && !isError && data.length === 0 ? (
          <EmptyState
            action={
              puedeEscribir ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos/nuevo">
                  Nuevo objeto
                </Link>
              ) : null
            }
            description="Todavia no hay objetos activos registrados."
            title="Sin objetos"
          />
        ) : null}
        {!isLoading && !isError && data.length > 0 ? <ObjetosTable canEdit={puedeEscribir} objetos={data} /> : null}
      </div>
    </AppShell>
  );
}

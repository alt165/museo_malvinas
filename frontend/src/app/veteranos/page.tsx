"use client";

import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { VeteranosTable } from "@/features/veteranos/components/veteranos-table";
import { useRelacionesObjetoVeteranoQuery, useVeteranosQuery } from "@/features/veteranos/queries";
import { getApiErrorMessage } from "@/features/veteranos/utils";
import { canWrite, useAuth } from "@/lib/auth";
import { ApiClientError } from "@/lib/errors/api-error";

export default function VeteranosPage() {
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);
  const veteranosQuery = useVeteranosQuery();
  const objetosQuery = useRelacionesObjetoVeteranoQuery();
  const objetos = objetosQuery.data ?? [];

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={puedeEscribir ? <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/veteranos/nuevo">Nuevo veterano</Link> : null}
          description="Registro de veteranos vinculados al acervo del museo."
          title="Veteranos"
        />
        {veteranosQuery.isLoading ? <LoadingState label="Cargando veteranos..." /> : null}
        {veteranosQuery.isError ? <ErrorState message={getApiErrorMessage(veteranosQuery.error)} requestId={veteranosQuery.error instanceof ApiClientError ? veteranosQuery.error.requestId : undefined} /> : null}
        {veteranosQuery.data?.length === 0 ? <EmptyState title="Sin veteranos" description="Todavía no hay veteranos registrados." /> : null}
        {veteranosQuery.data && veteranosQuery.data.length > 0 ? <VeteranosTable canEdit={puedeEscribir} objetos={objetos} veteranos={veteranosQuery.data} /> : null}
      </div>
    </AppShell>
  );
}

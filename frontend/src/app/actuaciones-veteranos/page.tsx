"use client";

import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useActuacionesVeteranosQuery } from "@/features/veteranos/queries";
import { formatDate, getApiErrorMessage } from "@/features/veteranos/utils";
import { ApiClientError } from "@/lib/errors/api-error";

export default function ActuacionesVeteranosPage() {
  const { data = [], error, isError, isLoading } = useActuacionesVeteranosQuery();

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader description="Participaciones, unidades, roles y periodos de veteranos." title="Actuaciones de veteranos" />
        {isLoading ? <LoadingState label="Cargando actuaciones..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} requestId={error instanceof ApiClientError ? error.requestId : undefined} /> : null}
        {!isLoading && !isError && data.length === 0 ? <EmptyState title="Sin actuaciones" /> : null}
        {data.length > 0 ? <div className="overflow-hidden rounded-lg border"><table className="w-full border-collapse text-sm"><thead className="bg-muted/60"><tr><th className="px-4 py-3 text-left font-medium">Veterano</th><th className="px-4 py-3 text-left font-medium">Rango</th><th className="px-4 py-3 text-left font-medium">Unidad</th><th className="px-4 py-3 text-left font-medium">Rol</th><th className="px-4 py-3 text-left font-medium">Periodo</th></tr></thead><tbody>{data.map((actuacion) => <tr className="border-t" key={actuacion.id}><td className="px-4 py-3 font-medium">{actuacion.veteranoNombreCompleto}</td><td className="px-4 py-3">{actuacion.rango || "-"}</td><td className="px-4 py-3">{actuacion.unidad || "-"}</td><td className="px-4 py-3">{actuacion.rol || "-"}</td><td className="px-4 py-3">{formatDate(actuacion.fechaInicio)} - {formatDate(actuacion.fechaFin)}</td></tr>)}</tbody></table></div> : null}
      </div>
    </AppShell>
  );
}

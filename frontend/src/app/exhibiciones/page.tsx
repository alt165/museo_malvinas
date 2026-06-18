"use client";

import Link from "next/link";
import { useState } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ExhibicionesTable } from "@/features/exhibiciones/components/exhibiciones-table";
import { useCancelarExhibicionPorIdMutation, useExhibicionesQuery, useFinalizarExhibicionPorIdMutation } from "@/features/exhibiciones/queries";
import { getApiErrorMessage } from "@/features/exhibiciones/utils";
import { useEditingMode } from "@/lib/editing-mode";
import { ApiClientError } from "@/lib/errors/api-error";

export default function ExhibicionesPage() {
  const { canEdit: puedeEscribir } = useEditingMode();
  const { data = [], error, isError, isLoading } = useExhibicionesQuery();
  const [finalizandoId, setFinalizandoId] = useState<number>();
  const [cancelandoId, setCancelandoId] = useState<number>();
  const finalizarMutation = useFinalizarExhibicionPorIdMutation();
  const cancelarMutation = useCancelarExhibicionPorIdMutation();

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            puedeEscribir ? (
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/exhibiciones/nueva">
                Nueva exhibición
              </Link>
            ) : null
          }
          description="Muestras temporales y permanentes del museo."
          title="Exhibiciones"
        />
        {isLoading ? <LoadingState label="Cargando exhibiciones..." /> : null}
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
        {!isLoading && !isError && data.length === 0 ? (
          <EmptyState
            action={
              puedeEscribir ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/exhibiciones/nueva">
                  Nueva exhibición
                </Link>
              ) : null
            }
            description="Todavía no hay exhibiciones activas o planificadas."
            title="Sin exhibiciones"
          />
        ) : null}
        {!isLoading && !isError && data.length > 0 ? (
          <ExhibicionesTable
            canEdit={puedeEscribir}
            exhibiciones={data}
            cancelandoId={cancelarMutation.isPending ? cancelandoId : undefined}
            finalizandoId={finalizarMutation.isPending ? finalizandoId : undefined}
            onCancelar={(id) => {
              if (window.confirm("¿Confirma que desea cancelar esta exhibición? Los objetos asociados quedarán disponibles.")) {
                setCancelandoId(id);
                cancelarMutation.mutate(id);
              }
            }}
            onFinalizar={(id) => {
              if (window.confirm("Confirmar finalización de la exhibición")) {
                setFinalizandoId(id);
                finalizarMutation.mutate(id);
              }
            }}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

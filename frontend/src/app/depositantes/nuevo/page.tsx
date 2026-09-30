"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { DepositanteForm } from "@/features/depositantes/components/depositante-form";
import { useCrearDepositanteMutation } from "@/features/depositantes/queries";
import { getApiErrorMessage } from "@/features/depositantes/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";
import {
  readCargaRapidaDepositanteContext,
  saveCreatedDepositante,
  type CargaRapidaDepositanteContext
} from "@/features/objetos/carga-rapida-context";

export default function NuevoDepositantePage() {
  return (
    <Suspense>
      <NuevoDepositanteContent />
    </Suspense>
  );
}

function NuevoDepositanteContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const mutation = useCrearDepositanteMutation();
  const identificacion = searchParams.get("identificacion") ?? undefined;
  const flowId = searchParams.get("flujo");
  const [cargaRapidaContext, setCargaRapidaContext] = useState<CargaRapidaDepositanteContext | null>(null);

  useEffect(() => {
    let active = true;
    queueMicrotask(() => {
      if (active && searchParams.get("origen") === "carga-rapida") {
        setCargaRapidaContext(readCargaRapidaDepositanteContext(flowId));
      }
    });

    return () => {
      active = false;
    };
  }, [flowId, searchParams]);

  const cargaRapidaReturnUrl = cargaRapidaContext
    ? `/objetos/carga-rapida?retorno=cancelado&flujo=${encodeURIComponent(cargaRapidaContext.flowId)}`
    : undefined;

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Alta de depositante." title="Nuevo depositante" />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        <DepositanteForm
          cancelHref={cargaRapidaReturnUrl}
          initialIdentification={identificacion}
          isSubmitting={mutation.isPending}
          onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (depositante) => {
            if (cargaRapidaContext) {
              saveCreatedDepositante(cargaRapidaContext, depositante);
              router.replace(`/objetos/carga-rapida?retorno=creado&flujo=${encodeURIComponent(cargaRapidaContext.flowId)}`);
              return;
            }

            router.push(`/depositantes/${depositante.id}`);
          } })}
          submitError={mutation.error}
          submitLabel="Crear depositante"
        />
      </div>
    </AppShell>
  );
}

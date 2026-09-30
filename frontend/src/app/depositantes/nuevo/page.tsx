"use client";

import Link from "next/link";
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
      <NuevoDepositanteRoute />
    </Suspense>
  );
}

function NuevoDepositanteRoute() {
  const searchParams = useSearchParams();
  const routeKey = `${searchParams.get("origen") ?? "normal"}:${searchParams.get("flujo") ?? "sin-flujo"}`;

  return <NuevoDepositanteContent key={routeKey} />;
}

function NuevoDepositanteContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const mutation = useCrearDepositanteMutation();
  const identificacion = searchParams.get("identificacion") ?? undefined;
  const flowId = searchParams.get("flujo");
  const esOrigenCargaRapida = searchParams.get("origen") === "carga-rapida";
  const [resolucionContexto, setResolucionContexto] = useState<ResolucionContexto>(() =>
    esOrigenCargaRapida
      ? { estado: "resolviendo", flowId }
      : { estado: "normal", flowId: null }
  );

  useEffect(() => {
    let active = true;
    queueMicrotask(() => {
      if (!active) {
        return;
      }
      if (!esOrigenCargaRapida) {
        setResolucionContexto({ estado: "normal", flowId: null });
        return;
      }

      const context = readCargaRapidaDepositanteContext(flowId);
      setResolucionContexto(context
        ? { estado: "valido", flowId, context }
        : { estado: "invalido", flowId });
    });

    return () => {
      active = false;
    };
  }, [esOrigenCargaRapida, flowId]);

  const resolucionActual = !esOrigenCargaRapida
    ? ({ estado: "normal", flowId: null } as const)
    : resolucionContexto.flowId === flowId
      ? resolucionContexto
      : ({ estado: "resolviendo", flowId } as const);

  if (resolucionActual.estado === "resolviendo") {
    return (
      <AppShell requiredRoles={[...routePermissions.write]}>
        <div className="space-y-6">
          <PageHeader description="Alta de depositante." title="Nuevo depositante" />
          <div className="rounded-md border bg-muted/30 p-4 text-sm text-muted-foreground">
            Recuperando el contexto de Alta Rápida...
          </div>
        </div>
      </AppShell>
    );
  }

  if (resolucionActual.estado === "invalido") {
    return (
      <AppShell requiredRoles={[...routePermissions.write]}>
        <div className="space-y-6">
          <PageHeader description="Alta de depositante." title="Nuevo depositante" />
          <ErrorState message="El contexto de Alta Rápida ya no está disponible o no es válido." />
          <Link
            className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm font-medium hover:bg-muted"
            href="/objetos/carga-rapida"
          >
            Volver a Alta Rápida
          </Link>
        </div>
      </AppShell>
    );
  }

  const cargaRapidaContext = resolucionActual.estado === "valido" ? resolucionActual.context : null;

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
          key={cargaRapidaContext?.flowId ?? "alta-normal"}
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

type ResolucionContexto =
  | { estado: "normal"; flowId: null }
  | { estado: "resolviendo"; flowId: string | null }
  | { estado: "valido"; flowId: string | null; context: CargaRapidaDepositanteContext }
  | { estado: "invalido"; flowId: string | null };

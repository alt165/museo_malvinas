"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ExhibicionForm } from "@/features/exhibiciones/components/exhibicion-form";
import { useCrearExhibicionMutation } from "@/features/exhibiciones/queries";
import { getApiErrorMessage } from "@/features/exhibiciones/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevaExhibicionPage() {
  return (
    <Suspense fallback={null}>
      <NuevaExhibicionContent />
    </Suspense>
  );
}

function NuevaExhibicionContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const repetirId = Number(searchParams.get("repetir"));
  const mutation = useCrearExhibicionMutation();

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Alta de exhibición." title="Nueva exhibición" />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        <ExhibicionForm
          isSubmitting={mutation.isPending}
          onSubmit={(payload) =>
            mutation.mutate(payload, {
              onSuccess: (exhibicion) => router.push(`/exhibiciones/${exhibicion.id}`)
            })
          }
          repetirExhibicionId={Number.isFinite(repetirId) ? repetirId : undefined}
          submitError={mutation.error}
          submitLabel={Number.isFinite(repetirId) ? "Crear exhibición repetida" : "Crear exhibición"}
        />
      </div>
    </AppShell>
  );
}

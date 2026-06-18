
"use client";

import { useParams, useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ExhibicionForm } from "@/features/exhibiciones/components/exhibicion-form";
import { useCrearExhibicionMutation } from "@/features/exhibiciones/queries";
import { getApiErrorMessage } from "@/features/exhibiciones/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function RepetirExhibicionPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const mutation = useCrearExhibicionMutation();

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader description="Crear una nueva exhibición a partir de una exhibición finalizada." title="Repetir exhibición" />
        {mutation.isError ? <ErrorState message={getApiErrorMessage(mutation.error)} requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined} /> : null}
        <ExhibicionForm
          isSubmitting={mutation.isPending}
          onSubmit={(payload) =>
            mutation.mutate(payload, {
              onSuccess: (exhibicion) => router.push(`/exhibiciones/${exhibicion.id}`)
            })
          }
          repetirExhibicionId={id}
          submitError={mutation.error}
          submitLabel="Crear exhibición repetida"
        />
      </div>
    </AppShell>
  );
}

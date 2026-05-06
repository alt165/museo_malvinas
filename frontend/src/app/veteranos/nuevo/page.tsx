"use client";

import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { VeteranoForm } from "@/features/veteranos/components/veterano-form";
import { useCrearVeteranoMutation } from "@/features/veteranos/queries";
import { getApiErrorMessage } from "@/features/veteranos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevoVeteranoPage() {
  const router = useRouter();
  const mutation = useCrearVeteranoMutation();
  return <AppShell requiredRoles={[...routePermissions.write]}><div className="space-y-6"><PageHeader title="Nuevo veterano" description="Alta de veterano." />{mutation.isError ? <ErrorState message={getApiErrorMessage(mutation.error)} requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined} /> : null}<VeteranoForm isSubmitting={mutation.isPending} submitLabel="Crear veterano" onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (v) => router.push(`/veteranos/${v.id}`) })} /></div></AppShell>;
}

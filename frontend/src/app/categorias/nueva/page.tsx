"use client";

import { useRouter } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { CategoriaForm } from "@/features/categorias/components/categoria-form";
import { useCrearCategoriaMutation } from "@/features/categorias/queries";
import { getApiErrorMessage } from "@/features/categorias/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

export default function NuevaCategoriaPage() {
  const router = useRouter();
  const mutation = useCrearCategoriaMutation();

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader description="Alta de categoria de objetos." title="Nueva categoria" />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        <CategoriaForm
          isSubmitting={mutation.isPending}
          onSubmit={(payload) => mutation.mutate(payload, { onSuccess: (categoria) => router.push(`/categorias/${categoria.id}`) })}
          submitError={mutation.error}
          submitLabel="Crear categoria"
        />
      </div>
    </AppShell>
  );
}

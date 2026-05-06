import { AppShell } from "@/components/layout/app-shell";
import { EmptyState } from "@/components/common/empty-state";
import { PageHeader } from "@/components/common/page-header";

export default function DashboardPage() {
  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          description="Vista inicial del sistema administrativo del museo."
          title="Dashboard"
        />
        <EmptyState
          description="La navegacion principal y el control de sesion ya estan disponibles. Los indicadores se agregaran cuando se implementen los modulos funcionales."
          title="Panel preparado"
        />
      </div>
    </AppShell>
  );
}

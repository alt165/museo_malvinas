import { AppShell } from "@/components/layout/app-shell";

export default function DashboardPage() {
  return (
    <AppShell>
      <div className="space-y-2">
        <h1 className="text-2xl font-semibold">Dashboard</h1>
        <p className="text-sm text-muted-foreground">
          Sesion autenticada con Keycloak. Los modulos funcionales se implementaran en etapas siguientes.
        </p>
      </div>
    </AppShell>
  );
}

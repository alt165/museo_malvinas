import { AppShell } from "@/components/layout/app-shell";
import type { UserRole } from "@/models/session";
import { EmptyState } from "./empty-state";
import { PageHeader } from "./page-header";
import { PermissionLink } from "./permission-link";

type PlaceholderPageProps = {
  title: string;
  description: string;
  emptyTitle?: string;
  emptyDescription?: string;
  actionHref?: string;
  actionLabel?: string;
  actionRoles?: UserRole[];
  requiredRoles?: UserRole[];
};

export function PlaceholderPage({
  actionHref,
  actionLabel,
  actionRoles,
  description,
  emptyDescription = "La estructura de navegacion esta lista. La funcionalidad se implementara en una etapa posterior.",
  emptyTitle = "Modulo pendiente de implementacion",
  requiredRoles,
  title
}: PlaceholderPageProps) {
  const action =
    actionHref && actionLabel ? (
      <PermissionLink href={actionHref} label={actionLabel} roles={actionRoles} />
    ) : null;

  return (
    <AppShell requiredRoles={requiredRoles}>
      <div className="space-y-6">
        <PageHeader actions={action} description={description} title={title} />
        <EmptyState description={emptyDescription} title={emptyTitle} />
      </div>
    </AppShell>
  );
}

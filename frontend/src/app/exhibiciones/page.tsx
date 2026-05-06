import { PlaceholderPage } from "@/components/common/placeholder-page";
import { routePermissions } from "@/lib/routes";

export default function ExhibicionesPage() {
  return (
    <PlaceholderPage
      actionHref="/exhibiciones/nueva"
      actionLabel="Nueva exhibicion"
      actionRoles={[...routePermissions.write]}
      description="Muestras temporales y permanentes del museo."
      title="Exhibiciones"
    />
  );
}

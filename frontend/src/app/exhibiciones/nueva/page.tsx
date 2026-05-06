import { PlaceholderPage } from "@/components/common/placeholder-page";
import { routePermissions } from "@/lib/routes";

export default function NuevaExhibicionPage() {
  return (
    <PlaceholderPage
      description="Alta de exhibicion."
      emptyDescription="El formulario de exhibicion se implementara con el modulo funcional correspondiente."
      requiredRoles={[...routePermissions.write]}
      title="Nueva exhibicion"
    />
  );
}

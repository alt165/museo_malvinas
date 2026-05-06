import { PlaceholderPage } from "@/components/common/placeholder-page";
import { routePermissions } from "@/lib/routes";

export default function EditarExhibicionPage() {
  return (
    <PlaceholderPage
      description="Edicion de exhibicion."
      emptyDescription="El formulario de edicion se implementara junto con validaciones y carga de datos reales."
      requiredRoles={[...routePermissions.write]}
      title="Editar exhibicion"
    />
  );
}

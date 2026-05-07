import {
  Archive,
  Boxes,
  FolderTree,
  Handshake,
  History,
  Home,
  IdCard,
  Landmark,
  MoveRight,
  UserCog,
  UserRound
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { UserRole } from "@/models/session";

export type NavigationItem = {
  href: string;
  label: string;
  description: string;
  icon: LucideIcon;
  roles: UserRole[];
  section: "principal" | "catalogos" | "personas" | "exhibiciones";
};

export type OperationAction = {
  href: string;
  label: string;
  roles: UserRole[];
};

const readRoles: UserRole[] = ["ADMIN", "OPERATOR", "VIEWER"];
const writeRoles: UserRole[] = ["ADMIN", "OPERATOR"];
const adminRoles: UserRole[] = ["ADMIN"];

export const routes = {
  dashboard: "/",
  dashboardHome: "/dashboard",
  objetos: "/objetos",
  objetoNuevo: "/objetos/nuevo",
  inventario: "/inventario",
  inventarioNuevo: "/inventario/nuevo",
  movimientosInventario: "/movimientos-inventario",
  categorias: "/categorias",
  categoriaNueva: "/categorias/nueva",
  depositantes: "/depositantes",
  depositanteNuevo: "/depositantes/nuevo",
  veteranos: "/veteranos",
  veteranoNuevo: "/veteranos/nuevo",
  actuacionesVeteranos: "/actuaciones-veteranos",
  exhibiciones: "/exhibiciones",
  exhibicionNueva: "/exhibiciones/nueva",
  usuarios: "/usuarios",
  usuarioNuevo: "/usuarios/nuevo",
  perfil: "/perfil"
} as const;

export const navigationItems: NavigationItem[] = [
  {
    href: routes.dashboardHome,
    label: "Dashboard",
    description: "Vista general operativa",
    icon: Home,
    roles: readRoles,
    section: "principal"
  },
  {
    href: routes.objetos,
    label: "Objetos del museo",
    description: "Catalogo patrimonial",
    icon: Archive,
    roles: readRoles,
    section: "principal"
  },
  {
    href: routes.inventario,
    label: "Inventario",
    description: "Ubicaciones y conservacion",
    icon: Boxes,
    roles: readRoles,
    section: "principal"
  },
  {
    href: routes.movimientosInventario,
    label: "Movimientos de inventario",
    description: "Registro de movimientos",
    icon: MoveRight,
    roles: readRoles,
    section: "principal"
  },
  {
    href: routes.categorias,
    label: "Categorias",
    description: "Clasificacion de objetos",
    icon: FolderTree,
    roles: readRoles,
    section: "catalogos"
  },
  {
    href: routes.depositantes,
    label: "Depositantes",
    description: "Personas e instituciones",
    icon: Handshake,
    roles: readRoles,
    section: "personas"
  },
  {
    href: routes.veteranos,
    label: "Veteranos",
    description: "Registro de veteranos",
    icon: IdCard,
    roles: readRoles,
    section: "personas"
  },
  {
    href: routes.actuacionesVeteranos,
    label: "Actuaciones de veteranos",
    description: "Participaciones y unidades",
    icon: History,
    roles: readRoles,
    section: "personas"
  },
  {
    href: routes.exhibiciones,
    label: "Exhibiciones",
    description: "Muestras y devoluciones",
    icon: Landmark,
    roles: readRoles,
    section: "exhibiciones"
  },
  {
    href: routes.perfil,
    label: "Perfil",
    description: "Sesion actual",
    icon: UserRound,
    roles: readRoles,
    section: "principal"
  },
  {
    href: routes.usuarios,
    label: "Usuarios",
    description: "Administracion Keycloak",
    icon: UserCog,
    roles: adminRoles,
    section: "principal"
  }
];

export const operationActions: OperationAction[] = [
  {
    href: routes.objetoNuevo,
    label: "Nuevo objeto",
    roles: writeRoles
  },
  {
    href: routes.categoriaNueva,
    label: "Nueva categoria",
    roles: writeRoles
  },
  {
    href: routes.depositanteNuevo,
    label: "Nuevo depositante",
    roles: writeRoles
  },
  {
    href: routes.exhibicionNueva,
    label: "Nueva exhibicion",
    roles: writeRoles
  },
  {
    href: routes.inventarioNuevo,
    label: "Nuevo inventario",
    roles: writeRoles
  },
  {
    href: routes.veteranoNuevo,
    label: "Nuevo veterano",
    roles: writeRoles
  }
];

export const routePermissions = {
  admin: adminRoles,
  read: readRoles,
  write: writeRoles
} as const;

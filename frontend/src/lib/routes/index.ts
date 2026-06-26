import {
  Archive,
  FileClock,
  Gavel,
  FolderTree,
  Handshake,
  History,
  IdCard,
  Landmark,
  Link2,
  MapPin,
  UserCog,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { UserRole } from "@/models/session";

export type NavigationItem = {
  href: string;
  label: string;
  icon: LucideIcon;
  roles: UserRole[];
  disabled?: boolean;
  badge?: string;
  requiresEditing?: boolean;
};

export type NavigationGroup = {
  key: string;
  label: string;
  icon: LucideIcon;
  roles: UserRole[];
  items: NavigationItem[];
};

const readRoles: UserRole[] = ["ADMIN", "OPERATOR", "VIEWER"];
const writeRoles: UserRole[] = ["ADMIN", "OPERATOR"];
const adminRoles: UserRole[] = ["ADMIN"];

export const routes = {
  dashboard: "/",
  dashboardHome: "/dashboard",
  objetos: "/objetos",
  objetosEliminados: "/objetos/eliminados",
  objetosCargaRapida: "/objetos/carga-rapida",
  objetosPendientes: "/objetos/pendientes",
  comodatosPrestamos: "/comodatos-prestamos",
  objetosEmbargos: "/objetos/embargos",
  objetosColecciones: "/objetos/colecciones",
  objetosColeccionNueva: "/objetos/colecciones/nueva",
  objetoNuevo: "/objetos/nuevo",
  relacionesObjetos: "/relaciones-objetos",
  relacionObjetoNueva: "/relaciones-objetos/nueva",
  inventario: "/inventario",
  inventarioNuevo: "/inventario/nuevo",
  movimientosInventario: "/movimientos-inventario",
  ubicaciones: "/ubicaciones",
  ubicacionNueva: "/ubicaciones/nueva",
  categorias: "/categorias",
  categoriaNueva: "/categorias/nueva",
  depositantes: "/depositantes",
  depositanteNuevo: "/depositantes/nuevo",
  veteranos: "/veteranos",
  veteranoNuevo: "/veteranos/nuevo",
  actuacionesVeteranos: "/actuaciones-veteranos",
  actuacionVeteranoNueva: "/actuaciones-veteranos/nueva",
  exhibiciones: "/exhibiciones",
  exhibicionesFinalizadas: "/exhibiciones/finalizadas",
  exhibicionNueva: "/exhibiciones/nueva",
  usuarios: "/usuarios",
  usuarioNuevo: "/usuarios/nuevo",
  perfil: "/perfil"
} as const;

export const navigationGroups: NavigationGroup[] = [
  {
    key: "colecciones",
    label: "Colecciones",
    icon: FolderTree,
    roles: readRoles,
    items: [
      { href: routes.objetosColecciones, label: "Consulta", icon: FolderTree, roles: readRoles },
      { href: routes.objetosColeccionNueva, label: "Alta", icon: FolderTree, roles: writeRoles, requiresEditing: true }
    ]
  },
  {
    key: "veteranos",
    label: "Veteranos",
    icon: IdCard,
    roles: readRoles,
    items: [
      { href: routes.veteranos, label: "Consulta", icon: IdCard, roles: readRoles },
      { href: routes.veteranoNuevo, label: "Alta de veterano", icon: IdCard, roles: writeRoles, requiresEditing: true },
      { href: routes.actuacionesVeteranos, label: "Actuaciones de veteranos", icon: History, roles: writeRoles, requiresEditing: true }
    ]
  },
  {
    key: "usuarios",
    label: "Usuarios",
    icon: UserCog,
    roles: adminRoles,
    items: [
      { href: routes.usuarios, label: "Consulta", icon: UserCog, roles: adminRoles },
      { href: routes.usuarioNuevo, label: "Alta", icon: UserCog, roles: adminRoles, requiresEditing: true }
    ]
  },
  {
    key: "objetos",
    label: "Objetos",
    icon: Archive,
    roles: readRoles,
    items: [
      { href: routes.objetos, label: "Consulta", icon: Archive, roles: readRoles },
      { href: routes.objetosEliminados, label: "Eliminados", icon: Archive, roles: adminRoles, requiresEditing: true },
      { href: routes.objetosCargaRapida, label: "Alta rapida", icon: Archive, roles: writeRoles, requiresEditing: true },
      { href: routes.objetosPendientes, label: "Pendientes de completar", icon: Archive, roles: writeRoles, requiresEditing: true },
      { href: routes.objetoNuevo, label: "Alta completa", icon: Archive, roles: writeRoles, requiresEditing: true },
      { href: routes.comodatosPrestamos, label: "Comodatos y préstamos", icon: FileClock, roles: adminRoles },
      { href: routes.objetosEmbargos, label: "Embargos", icon: Gavel, roles: adminRoles },
      { href: routes.relacionesObjetos, label: "Relaciones entre objetos", icon: Link2, roles: readRoles }
    ]
  },
  {
    key: "categorias",
    label: "Categorias",
    icon: FolderTree,
    roles: readRoles,
    items: [
      { href: routes.categorias, label: "Consulta", icon: FolderTree, roles: readRoles },
      { href: routes.categoriaNueva, label: "Alta", icon: FolderTree, roles: writeRoles, requiresEditing: true }
    ]
  },
  {
    key: "ubicaciones",
    label: "Ubicaciones",
    icon: MapPin,
    roles: writeRoles,
    items: [
      { href: routes.ubicaciones, label: "Consulta", icon: MapPin, roles: writeRoles },
      { href: routes.ubicacionNueva, label: "Alta", icon: MapPin, roles: adminRoles, requiresEditing: true }
    ]
  },
  {
    key: "depositantes",
    label: "Depositantes",
    icon: Handshake,
    roles: readRoles,
    items: [
      { href: routes.depositantes, label: "Consulta", icon: Handshake, roles: readRoles },
      { href: routes.depositanteNuevo, label: "Alta", icon: Handshake, roles: writeRoles, requiresEditing: true }
    ]
  },
  {
    key: "exhibiciones",
    label: "Exhibiciones",
    icon: Landmark,
    roles: readRoles,
    items: [
      { href: routes.exhibiciones, label: "Consulta", icon: Landmark, roles: readRoles },
      { href: routes.exhibicionNueva, label: "Alta", icon: Landmark, roles: writeRoles, requiresEditing: true },
      { href: routes.exhibicionesFinalizadas, label: "Repetir", icon: Landmark, roles: writeRoles, requiresEditing: true }
    ]
  }
];

export const navigationItems: NavigationItem[] = navigationGroups.flatMap((group) => group.items);

export const operationActions: NavigationItem[] = navigationItems.filter((item) => item.roles === writeRoles);

export const routePermissions = {
  admin: adminRoles,
  read: readRoles,
  write: writeRoles
} as const;

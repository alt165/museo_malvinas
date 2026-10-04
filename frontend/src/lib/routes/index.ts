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
  rangosMilitares: "/rangos-militares",
  unidadesMilitares: "/unidades-militares",
  detallesConservacion: "/detalles-conservacion",
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
    key: "veteranos",
    label: "Personas",
    icon: IdCard,
    roles: readRoles,
    items: [
      { href: routes.veteranos, label: "Consulta", icon: IdCard, roles: readRoles },
      { href: routes.veteranoNuevo, label: "Alta de persona", icon: IdCard, roles: writeRoles },
      { href: routes.actuacionesVeteranos, label: "Actuaciones de personas", icon: History, roles: writeRoles }
    ]
  },
  {
    key: "objetos",
    label: "Objetos",
    icon: Archive,
    roles: readRoles,
    items: [
      { href: routes.objetos, label: "Consulta", icon: Archive, roles: readRoles },
      { href: routes.objetosEliminados, label: "Eliminados", icon: Archive, roles: adminRoles },
      { href: routes.objetosCargaRapida, label: "Alta rapida", icon: Archive, roles: writeRoles },
      { href: routes.objetosPendientes, label: "Pendientes de completar", icon: Archive, roles: writeRoles },
      { href: routes.objetoNuevo, label: "Alta completa", icon: Archive, roles: writeRoles },
      { href: routes.comodatosPrestamos, label: "Comodatos y préstamos", icon: FileClock, roles: adminRoles },
      { href: routes.objetosEmbargos, label: "Embargos", icon: Gavel, roles: adminRoles },
      { href: routes.relacionesObjetos, label: "Relaciones entre objetos", icon: Link2, roles: readRoles }
    ]
  },
  {
    key: "colecciones",
    label: "Colecciones",
    icon: FolderTree,
    roles: readRoles,
    items: [
      { href: routes.objetosColecciones, label: "Consulta", icon: FolderTree, roles: readRoles },
      { href: routes.objetosColeccionNueva, label: "Alta", icon: FolderTree, roles: writeRoles }
    ]
  },
  {
    key: "exhibiciones",
    label: "Exhibiciones",
    icon: Landmark,
    roles: readRoles,
    items: [
      { href: routes.exhibiciones, label: "Consulta", icon: Landmark, roles: readRoles },
      { href: routes.exhibicionNueva, label: "Alta", icon: Landmark, roles: writeRoles },
      { href: routes.exhibicionesFinalizadas, label: "Repetir", icon: Landmark, roles: writeRoles }
    ]
  },
  {
    key: "depositantes",
    label: "Depositantes",
    icon: Handshake,
    roles: writeRoles,
    items: [
      { href: routes.depositantes, label: "Consulta", icon: Handshake, roles: writeRoles },
      { href: routes.depositanteNuevo, label: "Alta", icon: Handshake, roles: writeRoles }
    ]
  },
  {
    key: "usuarios",
    label: "Usuarios",
    icon: UserCog,
    roles: adminRoles,
    items: [
      { href: routes.usuarios, label: "Consulta", icon: UserCog, roles: adminRoles },
      { href: routes.usuarioNuevo, label: "Alta", icon: UserCog, roles: adminRoles }
    ]
  },
  {
    key: "tablas-auxiliares",
    label: "Tablas Auxiliares",
    icon: FolderTree,
    roles: adminRoles,
    items: [
      { href: routes.categorias, label: "Categorias", icon: FolderTree, roles: adminRoles },
      { href: routes.ubicaciones, label: "Ubicaciones", icon: MapPin, roles: adminRoles },
      { href: routes.rangosMilitares, label: "Rangos militares", icon: IdCard, roles: adminRoles },
      { href: routes.unidadesMilitares, label: "Unidades militares", icon: Landmark, roles: adminRoles },
      { href: routes.detallesConservacion, label: "Detalles de conservacion", icon: Archive, roles: adminRoles }
    ]
  }
];

export const navigationItems: NavigationItem[] = navigationGroups.flatMap((group) => group.items);

const editModeRoutePatterns: RegExp[] = [
  /^\/actuaciones-veteranos(?:\/.*)?$/,
  /^\/categorias\/(?:nueva|[^/]+\/editar)$/,
  /^\/depositantes\/(?:nuevo|[^/]+\/editar)$/,
  /^\/exhibiciones\/(?:nueva|finalizadas|repetir\/[^/]+|[^/]+\/editar)$/,
  /^\/inventario\/(?:nuevo|[^/]+\/editar)$/,
  /^\/objetos\/(?:nuevo|carga-rapida|pendientes|eliminados|[^/]+\/editar)$/,
  /^\/objetos\/colecciones\/(?:nueva|[^/]+\/editar)$/,
  /^\/relaciones-objetos\/(?:nueva|[^/]+\/editar)$/,
  /^\/ubicaciones\/(?:nueva|[^/]+\/editar)$/,
  /^\/usuarios\/(?:nuevo|[^/]+\/editar)$/,
  /^\/veteranos\/(?:nuevo|[^/]+\/editar)$/
];

export function requiresEditMode(pathname: string) {
  const normalizedPath = pathname.split(/[?#]/, 1)[0].replace(/\/$/, "") || "/";
  return editModeRoutePatterns.some((pattern) => pattern.test(normalizedPath));
}

export const operationActions: NavigationItem[] = navigationItems.filter((item) => item.roles === writeRoles);

export const routePermissions = {
  admin: adminRoles,
  read: readRoles,
  write: writeRoles
} as const;

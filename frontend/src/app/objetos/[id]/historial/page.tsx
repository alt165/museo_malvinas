"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useHistorialObjetoQuery, useObjetoQuery } from "@/features/objetos/queries";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { routePermissions } from "@/lib/routes";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

function formatFecha(value?: string | null) {
  if (!value) {
    return "Sin fecha";
  }
  return new Intl.DateTimeFormat("es-AR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

function etiquetaAuditoria(value?: string | null) {
  const labels: Record<string, string> = {
    COLECCION: "Colección",
    ELIMINACION_COLECCION: "Eliminación de colección",
    INCORPORACION_COLECCION: "Incorporación a colección",
    DESVINCULACION_COLECCION: "Desvinculación de colección",
    DESVINCULACION_POR_ELIMINACION_COLECCION: "Desvinculación por eliminación de colección",
    CREACION: "Creación",
    MODIFICACION: "Modificación",
    ELIMINACION: "Eliminación"
  };

  return value ? labels[value] ?? value : "Sin origen";
}

function formatValor(value?: string | null) {
  if (!value) {
    return "Sin datos";
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function ValoresAuditoria({ label, value }: { label: string; value?: string | null }) {
  if (!value) {
    return (
      <div>
        <div className="mb-1 text-xs font-semibold text-muted-foreground">{label}</div>
        <div className="rounded-md bg-muted px-3 py-2 text-xs text-muted-foreground">Sin datos</div>
      </div>
    );
  }

  return (
    <details className="group rounded-md border bg-muted/40">
      <summary className="cursor-pointer list-none px-3 py-2 text-xs font-semibold text-muted-foreground hover:text-foreground">
        <span className="group-open:hidden">Ver {label.toLowerCase()}</span>
        <span className="hidden group-open:inline">Ocultar {label.toLowerCase()}</span>
      </summary>
      <pre className="max-h-80 overflow-auto border-t bg-muted p-3 text-xs leading-relaxed">{formatValor(value)}</pre>
    </details>
  );
}

export default function HistorialObjetoPage() {
  const params = useParams<{ id: string }>();
  const id = getParamId(params.id);
  const { data: objeto } = useObjetoQuery(id);
  const { data = [], error, isError, isLoading } = useHistorialObjetoQuery(id);

  return (
    <AppShell requiredRoles={routePermissions.admin}>
      <div className="space-y-6">
        <PageHeader
          actions={
            <div className="flex gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/objetos/${id}`}>
                Volver
              </Link>
            </div>
          }
          description={objeto ? `${objeto.numeroInventario} - ${objeto.denominacionObjeto}` : "Eventos registrados para el objeto."}
          title="Historial"
        />

        {isLoading ? <LoadingState label="Cargando historial..." /> : null}
        {isError ? (
          <ErrorState message={getApiErrorMessage(error)} />
        ) : null}

        {!isLoading && !isError && data.length === 0 ? (
          <div className="rounded-lg border p-5 text-sm text-muted-foreground">No hay eventos de auditoria registrados para este objeto.</div>
        ) : null}

        {data.length > 0 ? (
          <div className="overflow-hidden rounded-lg border">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y text-sm">
                <thead className="bg-muted/40 text-left text-xs uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 font-semibold">Fecha y hora</th>
                    <th className="px-4 py-3 font-semibold">Accion</th>
                    <th className="px-4 py-3 font-semibold">Descripcion</th>
                    <th className="px-4 py-3 font-semibold">Usuario</th>
                    <th className="px-4 py-3 font-semibold">Origen</th>
                    <th className="px-4 py-3 font-semibold">Valores</th>
                  </tr>
                </thead>
                <tbody className="divide-y bg-card">
                  {data.map((evento) => (
                    <tr key={evento.id}>
                      <td className="whitespace-nowrap px-4 py-3 align-top">{formatFecha(evento.fechaHora)}</td>
                      <td className="px-4 py-3 align-top">
                        <div className="font-medium">{etiquetaAuditoria(evento.accion || evento.tipoOperacion)}</div>
                        <div className="text-xs text-muted-foreground">{etiquetaAuditoria(evento.tipoOperacion)}</div>
                      </td>
                      <td className="min-w-64 px-4 py-3 align-top">{evento.descripcion || "Sin descripcion"}</td>
                      <td className="px-4 py-3 align-top">
                        <div>{evento.usuario || "Usuario no identificado"}</div>
                        {evento.rol ? <div className="text-xs text-muted-foreground">{evento.rol}</div> : null}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 align-top">{etiquetaAuditoria(evento.origen)}</td>
                      <td className="min-w-80 px-4 py-3 align-top">
                        <div className="grid gap-3 md:grid-cols-2">
                          <ValoresAuditoria label="Anteriores" value={evento.valoresAnteriores} />
                          <ValoresAuditoria label="Nuevos" value={evento.valoresNuevos} />
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { Download, Trash2 } from "lucide-react";
import { useState } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { exportarColeccionPdf } from "@/features/colecciones/api";
import { ObjetosColeccionPanel } from "@/features/colecciones/components/objetos-coleccion-panel";
import { useBajaLogicaColeccionMutation, useColeccionQuery } from "@/features/colecciones/queries";
import { getApiErrorMessage, resumenDescripcion } from "@/features/colecciones/utils";
import { descargarBlob } from "@/lib/download";
import { useEditingMode } from "@/lib/editing-mode";
import { routes } from "@/lib/routes";

export default function ColeccionDetallePage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const router = useRouter();
  const { canAdminEdit: puedeEliminar, canEdit: puedeEscribir } = useEditingMode();
  const coleccionQuery = useColeccionQuery(id);
  const bajaMutation = useBajaLogicaColeccionMutation();
  const [descargandoPdf, setDescargandoPdf] = useState(false);
  const [descargaPdfError, setDescargaPdfError] = useState<string | null>(null);
  const [descargaPdfOk, setDescargaPdfOk] = useState(false);

  function handleEliminar() {
    if (!puedeEliminar || !coleccionQuery.data) {
      return;
    }

    if (window.confirm("Al eliminar esta colección, los objetos asociados quedarán sin colección. ¿Desea continuar?")) {
      bajaMutation.mutate(id, { onSuccess: () => router.push(routes.objetosColecciones) });
    }
  }

  async function handleDescargarPdf() {
    if (!coleccionQuery.data) {
      return;
    }

    setDescargandoPdf(true);
    setDescargaPdfError(null);
    setDescargaPdfOk(false);

    try {
      const blob = await exportarColeccionPdf(id);
      descargarBlob(blob, nombreArchivoColeccionPdf(coleccionQuery.data.nombre, id));
      setDescargaPdfOk(true);
    } catch (error) {
      setDescargaPdfError(getApiErrorMessage(error));
    } finally {
      setDescargandoPdf(false);
    }
  }

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          actions={
            <div className="flex flex-wrap gap-2">
              <button
                className="inline-flex h-10 items-center gap-2 rounded-md border px-4 text-sm font-medium hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                disabled={descargandoPdf || coleccionQuery.isLoading || !coleccionQuery.data}
                onClick={handleDescargarPdf}
                type="button"
              >
                <Download className="h-4 w-4" />
                {descargandoPdf ? "Generando PDF..." : "Descargar PDF"}
              </button>
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={routes.objetosColecciones}>
                Volver
              </Link>
              {puedeEscribir ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/objetos/colecciones/${id}/editar`}>
                  Editar
                </Link>
              ) : null}
              {puedeEliminar ? (
                <button
                  className="inline-flex h-10 items-center gap-2 rounded-md border px-4 text-sm font-medium text-destructive hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={bajaMutation.isPending || !coleccionQuery.data}
                  onClick={handleEliminar}
                  type="button"
                >
                  <Trash2 className="h-4 w-4" />
                  {bajaMutation.isPending ? "Eliminando..." : "Eliminar"}
                </button>
              ) : null}
            </div>
          }
          description="Detalle y objetos asociados a la coleccion."
          title={coleccionQuery.data?.nombre ?? "Coleccion"}
        />
        {coleccionQuery.isLoading ? <LoadingState label="Cargando coleccion..." /> : null}
        {coleccionQuery.isError ? (
          <ErrorState message={getApiErrorMessage(coleccionQuery.error)} />
        ) : null}
        {descargaPdfError ? <ErrorState message={descargaPdfError} title="No se pudo descargar el PDF" /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} title="No se pudo eliminar la colección" /> : null}
        {descargaPdfOk ? (
          <div className="rounded-lg border border-green-200 bg-green-50 p-3 text-sm font-medium text-green-900">
            PDF generado correctamente.
          </div>
        ) : null}
        {coleccionQuery.data ? (
          <>
            <section className="grid gap-4 rounded-lg border bg-surface p-5 shadow-sm sm:grid-cols-3">
              <Info label="Nombre" value={coleccionQuery.data.nombre} />
              <Info label="Objetos asociados" value={String(coleccionQuery.data.cantidadObjetos ?? 0)} />
              <Info className="sm:col-span-3" label="Descripcion" value={resumenDescripcion(coleccionQuery.data.descripcion)} />
            </section>
            <ObjetosColeccionPanel coleccionId={id} />
          </>
        ) : null}
      </div>
    </AppShell>
  );
}

function nombreArchivoColeccionPdf(nombre: string, id: number) {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  const nombreSeguro = nombre
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "") || String(id);
  return `coleccion_${nombreSeguro}_${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}_${pad(now.getHours())}-${pad(now.getMinutes())}.pdf`;
}

function Info({ className, label, value }: { className?: string; label: string; value: string }) {
  return (
    <div className={className}>
      <p className="text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm font-medium text-primary">{value}</p>
    </div>
  );
}

"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { Download } from "lucide-react";
import { useState } from "react";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { exportarObjetosDepositantePdf } from "@/features/depositantes/api";
import { useBajaLogicaDepositanteMutation, useDepositanteQuery, useObjetosDepositanteQuery } from "@/features/depositantes/queries";
import { getApiErrorMessage, resumenObservaciones, telefonoVisible } from "@/features/depositantes/utils";
import type { DepositanteResponseDTO } from "@/features/depositantes/types";
import type { ObjetoMuseoResponseDTO } from "@/features/objetos/types";
import { descargarBlob } from "@/lib/download";
import { useEditingMode } from "@/lib/editing-mode";

function getParamId(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? value[0] : value;
  const id = Number(raw);
  return Number.isFinite(id) ? id : NaN;
}

export default function DetalleDepositantePage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = getParamId(params.id);
  const { canEdit: puedeEscribir } = useEditingMode();
  const { data, error, isError, isLoading } = useDepositanteQuery(id);
  const objetosQuery = useObjetosDepositanteQuery(id);
  const bajaMutation = useBajaLogicaDepositanteMutation();
  const [descargandoPdf, setDescargandoPdf] = useState(false);
  const [descargaPdfError, setDescargaPdfError] = useState<string | null>(null);
  const [descargaPdfOk, setDescargaPdfOk] = useState(false);

  function handleDelete() {
    if (window.confirm("Dar de baja este depositante?")) {
      bajaMutation.mutate(id, { onSuccess: () => router.push("/depositantes") });
    }
  }

  async function handleDescargarPdf() {
    if (!data) {
      return;
    }

    setDescargandoPdf(true);
    setDescargaPdfError(null);
    setDescargaPdfOk(false);

    try {
      const blob = await exportarObjetosDepositantePdf(id);
      descargarBlob(blob, nombreArchivoDepositanteObjetosPdf(data));
      setDescargaPdfOk(true);
    } catch (downloadError) {
      setDescargaPdfError(getApiErrorMessage(downloadError));
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
                disabled={descargandoPdf || isLoading || !data}
                onClick={handleDescargarPdf}
                type="button"
              >
                <Download className="h-4 w-4" />
                {descargandoPdf ? "Generando PDF..." : "Descargar PDF"}
              </button>
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/depositantes">
                Volver
              </Link>
              {puedeEscribir && data ? (
                <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/depositantes/${data.id}/editar`}>
                  Editar
                </Link>
              ) : null}
              {puedeEscribir && data ? (
                <button
                  className="rounded-md border px-4 py-2 text-sm font-medium text-destructive hover:bg-muted disabled:opacity-60"
                  disabled={bajaMutation.isPending}
                  onClick={handleDelete}
                  type="button"
                >
                  Baja
                </button>
              ) : null}
            </div>
          }
          description="Datos del depositante."
          title="Detalle de depositante"
        />
        {isLoading ? <LoadingState label="Cargando depositante..." /> : null}
        {isError ? <ErrorState message={getApiErrorMessage(error)} /> : null}
        {bajaMutation.isError ? <ErrorState message={getApiErrorMessage(bajaMutation.error)} /> : null}
        {descargaPdfError ? <ErrorState message={descargaPdfError} title="No se pudo descargar el PDF" /> : null}
        {descargaPdfOk ? (
          <div className="rounded-lg border border-green-200 bg-green-50 p-3 text-sm font-medium text-green-900">
            PDF generado correctamente.
          </div>
        ) : null}
        {data ? (
          <>
            <div className="rounded-lg border p-5">
              <dl className="grid gap-5 text-sm sm:grid-cols-2">
                <div>
                  <dt className="text-muted-foreground">Tipo</dt>
                  <dd className="font-medium">{data.tipo}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Nombre / organizacion</dt>
                  <dd className="font-medium">{data.nombre}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Email</dt>
                  <dd className="font-medium">{data.contacto || "Sin email"}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">Telefono</dt>
                  <dd className="font-medium">{telefonoVisible(data)}</dd>
                </div>
                <div className="sm:col-span-2">
                  <dt className="text-muted-foreground">Observaciones</dt>
                  <dd className="whitespace-pre-wrap font-medium">{resumenObservaciones(data.observaciones)}</dd>
                </div>
              </dl>
            </div>
            <ObjetosEntregadosSection objetosQuery={objetosQuery} />
          </>
        ) : null}
      </div>
    </AppShell>
  );
}

function ObjetosEntregadosSection({ objetosQuery }: { objetosQuery: ReturnType<typeof useObjetosDepositanteQuery> }) {
  const objetos = objetosQuery.data ?? [];

  return (
    <section className="space-y-4 rounded-lg border p-5">
      <div>
        <h2 className="text-lg font-semibold text-primary">Objetos entregados</h2>
      </div>
      {objetosQuery.isLoading ? <LoadingState label="Cargando objetos entregados..." /> : null}
      {objetosQuery.isError ? <ErrorState message={getApiErrorMessage(objetosQuery.error)} title="No se pudieron cargar los objetos" /> : null}
      {!objetosQuery.isLoading && !objetosQuery.isError && objetos.length === 0 ? (
        <p className="text-sm text-muted-foreground">Este depositante no tiene objetos registrados.</p>
      ) : null}
      {!objetosQuery.isLoading && !objetosQuery.isError && objetos.length > 0 ? <ObjetosEntregadosTable objetos={objetos} /> : null}
    </section>
  );
}

function ObjetosEntregadosTable({ objetos }: { objetos: ObjetoMuseoResponseDTO[] }) {
  return (
    <div className="overflow-x-auto rounded-md border">
      <table className="w-full min-w-[980px] text-left text-sm">
        <thead className="bg-muted text-xs uppercase text-muted-foreground">
          <tr>
            <th className="px-3 py-2 font-medium">Inventario</th>
            <th className="px-3 py-2 font-medium">Nombre / denominación</th>
            <th className="px-3 py-2 font-medium">Categoría</th>
            <th className="px-3 py-2 font-medium">Ubicación</th>
            <th className="px-3 py-2 font-medium">Estado</th>
            <th className="px-3 py-2 font-medium">Recepción</th>
            <th className="px-3 py-2 font-medium">Ingreso</th>
            <th className="px-3 py-2 font-medium">Vencimiento</th>
          </tr>
        </thead>
        <tbody className="divide-y">
          {objetos.map((objeto) => (
            <tr key={objeto.id} className="align-top">
              <td className="px-3 py-3 font-medium">{objeto.numeroInventario}</td>
              <td className="px-3 py-3">{objeto.denominacionObjeto}</td>
              <td className="px-3 py-3">{categoriasObjeto(objeto)}</td>
              <td className="px-3 py-3">{objeto.ubicacionNombre || "Sin ubicación"}</td>
              <td className="px-3 py-3">{enumVisible(objeto.estadoConservacion)}</td>
              <td className="px-3 py-3">{enumVisible(objeto.caracterRecepcion)}</td>
              <td className="px-3 py-3">{fechaVisible(objeto.fechaIngreso)}</td>
              <td className="px-3 py-3">{fechaVisible(objeto.fechaVencimiento)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function categoriasObjeto(objeto: ObjetoMuseoResponseDTO) {
  return objeto.categorias && objeto.categorias.length > 0 ? objeto.categorias.map((categoria) => categoria.nombre).join(", ") : "Sin categoría";
}

function enumVisible(value?: string | null) {
  return value ? value.replaceAll("_", " ") : "-";
}

function fechaVisible(value?: string | null) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("es-AR", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(`${value}T00:00:00`));
}

function nombreArchivoDepositanteObjetosPdf(depositante: DepositanteResponseDTO) {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  const nombreSeguro = depositante.nombre
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "") || String(depositante.id);
  return `depositante_${nombreSeguro}_objetos_${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}_${pad(now.getHours())}-${pad(now.getMinutes())}.pdf`;
}

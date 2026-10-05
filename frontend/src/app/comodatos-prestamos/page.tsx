"use client";

import { useEffect, useMemo, useState } from "react";
import { Download, X } from "lucide-react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { exportarComodatosPrestamosPdf } from "@/features/objetos/api";
import {
  useActualizarConfigAlertasComodatosPrestamosMutation,
  useActualizarFechaVencimientoComodatoPrestamoMutation,
  useBuscarComodatosPrestamosQuery,
  useConfigAlertasComodatosPrestamosQuery
} from "@/features/objetos/queries";
import type { CaracterRecepcionObjeto, ComodatoPrestamoResponseDTO, EstadoVencimientoComodatoPrestamo } from "@/features/objetos/types";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { descargarBlob } from "@/lib/download";
import { currentTimestampInArgentinaForFilename } from "@/lib/date-time";
import { useEditingMode } from "@/lib/editing-mode";
import { routePermissions } from "@/lib/routes";

const caracterLabels: Record<Extract<CaracterRecepcionObjeto, "PRESTAMO" | "COMODATO">, string> = {
  PRESTAMO: "Préstamo",
  COMODATO: "Comodato"
};

const estadoLabels: Record<EstadoVencimientoComodatoPrestamo, string> = {
  VIGENTE: "Vigente",
  PROXIMO_A_VENCER: "Próximo a vencer",
  VENCIDO: "Vencido"
};

const estadoClasses: Record<EstadoVencimientoComodatoPrestamo, string> = {
  VIGENTE: "border-emerald-200 bg-emerald-50 text-emerald-800",
  PROXIMO_A_VENCER: "border-amber-200 bg-amber-50 text-amber-800",
  VENCIDO: "border-red-200 bg-red-50 text-red-800"
};

function formatDate(value?: string | null) {
  if (!value) {
    return "Sin fecha de vencimiento";
  }

  return new Intl.DateTimeFormat("es-AR", { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

function diasRestantesLabel(dias?: number | null) {
  if (dias == null) {
    return "Sin fecha";
  }

  if (dias < 0) {
    const diasVencido = Math.abs(dias);
    return diasVencido === 1 ? "Vencido hace 1 día" : `Vencido hace ${diasVencido} días`;
  }

  if (dias === 0) {
    return "Vence hoy";
  }

  return dias === 1 ? "Vence en 1 día" : `Vence en ${dias} días`;
}

function estadoBadge(objeto: ComodatoPrestamoResponseDTO) {
  if (!objeto.fechaVencimiento) {
    return <span className="inline-flex rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-700">Sin fecha</span>;
  }

  return (
    <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${estadoClasses[objeto.estadoVencimiento]}`}>
      {estadoLabels[objeto.estadoVencimiento]}
    </span>
  );
}

export default function ComodatosPrestamosPage() {
  const { canAdminEdit } = useEditingMode();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [texto, setTexto] = useState("");
  const [textoAplicado, setTextoAplicado] = useState("");
  const [fechasEditadas, setFechasEditadas] = useState<Record<number, string>>({});
  const [diasAnticipacionInput, setDiasAnticipacionInput] = useState("");
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [descargandoPdf, setDescargandoPdf] = useState(false);
  const [descargaPdfError, setDescargaPdfError] = useState<string | null>(null);
  const [descargaPdfOk, setDescargaPdfOk] = useState(false);
  useEffect(() => {
    const timeout = window.setTimeout(() => setTextoAplicado(texto.trim()), 300);
    return () => window.clearTimeout(timeout);
  }, [texto]);
  const buscando = texto.trim() !== textoAplicado;
  const params = useMemo(() => ({ texto: textoAplicado, page, size }), [textoAplicado, page, size]);
  const comodatosQuery = useBuscarComodatosPrestamosQuery(params);
  const configQuery = useConfigAlertasComodatosPrestamosQuery();
  const actualizarFecha = useActualizarFechaVencimientoComodatoPrestamoMutation();
  const actualizarConfig = useActualizarConfigAlertasComodatosPrestamosMutation();
  const objetos = useMemo(() => comodatosQuery.data?.content ?? [], [comodatosQuery.data?.content]);
  const diasConfigurados = configQuery.data?.diasAnticipacion;
  const diasFormulario = diasAnticipacionInput || (diasConfigurados == null ? "" : String(diasConfigurados));

  function handleFechaChange(objetoId: number, value: string) {
    setMensaje(null);
    setFechasEditadas((current) => ({ ...current, [objetoId]: value }));
  }

  function handleGuardarFecha(objeto: ComodatoPrestamoResponseDTO) {
    const fechaVencimiento = fechasEditadas[objeto.id] ?? objeto.fechaVencimiento ?? "";
    setMensaje(null);
    if (!fechaVencimiento) {
      setMensaje("Ingrese una fecha de vencimiento antes de guardar.");
      return;
    }

    actualizarFecha.mutate(
      { objetoId: objeto.id, fechaVencimiento },
      {
        onSuccess: () => {
          setFechasEditadas((current) => {
            const next = { ...current };
            delete next[objeto.id];
            return next;
          });
          setMensaje("Fecha de vencimiento actualizada.");
        }
      }
    );
  }

  function handleGuardarConfig() {
    const diasAnticipacion = Number(diasFormulario);
    setMensaje(null);
    if (!Number.isInteger(diasAnticipacion) || diasAnticipacion < 1 || diasAnticipacion > 365) {
      setMensaje("Los días de anticipación deben estar entre 1 y 365.");
      return;
    }

    actualizarConfig.mutate(diasAnticipacion, {
      onSuccess: () => {
        setDiasAnticipacionInput("");
        setMensaje("Configuración de alertas actualizada.");
      }
    });
  }

  async function handleDescargarPdf() {
    setDescargandoPdf(true);
    setDescargaPdfError(null);
    setDescargaPdfOk(false);

    try {
      const blob = await exportarComodatosPrestamosPdf();
      descargarBlob(blob, nombreArchivoComodatosPdf());
      setDescargaPdfOk(true);
    } catch (downloadError) {
      setDescargaPdfError(getApiErrorMessage(downloadError));
    } finally {
      setDescargandoPdf(false);
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader
          actions={
            <button
              className="inline-flex h-10 items-center gap-2 rounded-md border px-4 text-sm font-medium hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
              disabled={descargandoPdf || comodatosQuery.isLoading}
              onClick={handleDescargarPdf}
              type="button"
            >
              <Download className="h-4 w-4" />
              {descargandoPdf ? "Generando PDF..." : "Descargar PDF"}
            </button>
          }
          description="Administración de objetos recibidos como préstamo o comodato, ordenados por vencimiento más próximo."
          title="Comodatos y préstamos"
        />

        {!canAdminEdit ? (
          <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            Active ‘Permitir edición’ para modificar fechas o la configuración de alertas.
          </div>
        ) : null}

        {mensaje ? <div className="rounded-md border border-primary/20 bg-primary/5 px-4 py-3 text-sm font-medium text-primary">{mensaje}</div> : null}
        {descargaPdfError ? <ErrorState message={descargaPdfError} title="No se pudo descargar el PDF" /> : null}
        {descargaPdfOk ? (
          <div className="rounded-lg border border-green-200 bg-green-50 p-3 text-sm font-medium text-green-900">
            PDF generado correctamente.
          </div>
        ) : null}
        {actualizarFecha.isError ? <ErrorState message={getApiErrorMessage(actualizarFecha.error)} title="No se pudo actualizar la fecha" /> : null}
        {actualizarConfig.isError ? <ErrorState message={getApiErrorMessage(actualizarConfig.error)} title="No se pudo actualizar la configuración" /> : null}

        <section className="rounded-lg border bg-surface p-5 shadow-sm">
          <div className="mb-4 flex flex-col gap-1">
            <h2 className="text-base font-semibold text-primary">Días de anticipación para alertas</h2>
            <p className="text-sm text-muted-foreground">El valor se usa en la alerta global de vencimientos para Administrador.</p>
          </div>
          {configQuery.isLoading ? <LoadingState label="Cargando configuración..." /> : null}
          {configQuery.isError ? <ErrorState message={getApiErrorMessage(configQuery.error)} title="No se pudo cargar la configuración" /> : null}
          {configQuery.isSuccess ? (
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
              <label className="w-full max-w-xs space-y-1 text-sm font-medium text-primary">
                <span>Días de anticipación</span>
                <input
                  className="h-10 w-full rounded-md border bg-white px-3 text-sm outline-none focus:border-primary disabled:bg-muted"
                  disabled={!canAdminEdit || actualizarConfig.isPending}
                  max={365}
                  min={1}
                  onChange={(event) => setDiasAnticipacionInput(event.target.value)}
                  type="number"
                  value={diasFormulario}
                />
              </label>
              <button
                className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-white hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
                disabled={!canAdminEdit || actualizarConfig.isPending}
                onClick={handleGuardarConfig}
                title={!canAdminEdit ? "Active ‘Permitir edición’ para modificar datos." : undefined}
                type="button"
              >
                {actualizarConfig.isPending ? "Guardando..." : "Guardar"}
              </button>
            </div>
          ) : null}
        </section>

        <section className="rounded-lg border bg-surface p-5 shadow-sm">
          <div className="mb-4 flex flex-col gap-1">
            <h2 className="text-base font-semibold text-primary">Objetos recibidos por préstamo o comodato</h2>
            <p className="text-sm text-muted-foreground">Los objetos sin fecha de vencimiento se muestran al final.</p>
          </div>

          <div className="mb-4 flex flex-col gap-2 sm:flex-row">
            <input
              className="h-10 min-w-0 flex-1 rounded-md border bg-white px-3 text-sm outline-none focus:border-primary"
              aria-label="Buscar por inventario, objeto o depositante"
              onChange={(event) => {
                setPage(0);
                setTexto(event.target.value);
              }}
              placeholder="Buscar por inventario, objeto o depositante"
              value={texto}
            />
            {texto ? (
              <button
                className="inline-flex h-10 items-center justify-center gap-2 rounded-md border px-3 text-sm font-medium hover:bg-muted"
                onClick={() => {
                  setPage(0);
                  setTexto("");
                }}
                onMouseDown={(event) => event.preventDefault()}
                type="button"
              >
                <X className="h-4 w-4" />
                Limpiar
              </button>
            ) : null}
          </div>

          {comodatosQuery.isLoading || comodatosQuery.isFetching || buscando ? <LoadingState label="Cargando comodatos y préstamos..." /> : null}
          {comodatosQuery.isError ? <ErrorState message={getApiErrorMessage(comodatosQuery.error)} /> : null}
          {!comodatosQuery.isLoading && !comodatosQuery.isError && !buscando && comodatosQuery.isSuccess && objetos.length === 0 ? (
            <EmptyState description="No hay objetos activos recibidos como préstamo o comodato." title="Sin resultados" />
          ) : null}
          {!comodatosQuery.isLoading && !comodatosQuery.isError && !buscando && comodatosQuery.isSuccess && objetos.length > 0 ? (
            <div className="overflow-x-auto rounded-md border bg-white">
              <table className="w-full min-w-[1040px] border-collapse text-left text-sm">
                <thead className="bg-muted/60 text-primary">
                  <tr>
                    <th className="px-3 py-2 font-medium">Inventario</th>
                    <th className="px-3 py-2 font-medium">Objeto</th>
                    <th className="px-3 py-2 font-medium">Depositante</th>
                    <th className="px-3 py-2 font-medium">Carácter</th>
                    <th className="px-3 py-2 font-medium">Fecha de vencimiento</th>
                    <th className="px-3 py-2 font-medium">Días restantes</th>
                    <th className="px-3 py-2 font-medium">Estado</th>
                    <th className="px-3 py-2 font-medium">Actualizar</th>
                  </tr>
                </thead>
                <tbody>
                  {objetos.map((objeto) => {
                    const fechaFormulario = fechasEditadas[objeto.id] ?? objeto.fechaVencimiento ?? "";
                    return (
                      <tr className="border-t" key={objeto.id}>
                        <td className="px-3 py-3 font-medium text-primary">{objeto.numeroInventario}</td>
                        <td className="px-3 py-3">{objeto.denominacionObjeto}</td>
                        <td className="px-3 py-3">{objeto.depositanteNombre}</td>
                        <td className="px-3 py-3">{caracterLabels[objeto.caracterRecepcion]}</td>
                        <td className="px-3 py-3">
                          <div className="space-y-1">
                            <span className="block text-xs text-muted-foreground">Actual: {formatDate(objeto.fechaVencimiento)}</span>
                            <input
                              className="h-9 w-40 rounded-md border bg-white px-2 text-sm outline-none focus:border-primary disabled:bg-muted"
                              disabled={!canAdminEdit || actualizarFecha.isPending}
                              min={objeto.fechaIngreso}
                              onChange={(event) => handleFechaChange(objeto.id, event.target.value)}
                              type="date"
                              value={fechaFormulario}
                            />
                          </div>
                        </td>
                        <td className="px-3 py-3">{diasRestantesLabel(objeto.diasRestantes)}</td>
                        <td className="px-3 py-3">{estadoBadge(objeto)}</td>
                        <td className="px-3 py-3">
                          <button
                            className="h-9 rounded-md bg-primary px-3 text-sm font-medium text-white hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
                            disabled={!canAdminEdit || actualizarFecha.isPending || !fechaFormulario}
                            onClick={() => handleGuardarFecha(objeto)}
                            title={!canAdminEdit ? "Active ‘Permitir edición’ para modificar datos." : undefined}
                            type="button"
                          >
                            {actualizarFecha.isPending ? "Guardando..." : "Guardar"}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : null}
          {!comodatosQuery.isLoading && !comodatosQuery.isError && !buscando && comodatosQuery.data ? (
            <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-background px-4 py-3 text-sm">
              <div className="text-muted-foreground">
                {comodatosQuery.isFetching ? "Actualizando..." : `${comodatosQuery.data.totalElements} elementos encontrados`}
              </div>
              <div className="flex items-center gap-2">
                <label className="flex items-center gap-2">
                  <span>Cantidad por página</span>
                  <select
                    className="h-9 rounded-md border bg-white px-2"
                    onChange={(event) => {
                      setPage(0);
                      setSize(Number(event.target.value));
                    }}
                    value={size}
                  >
                    <option value={10}>10</option>
                    <option value={20}>20</option>
                    <option value={50}>50</option>
                  </select>
                </label>
                <button
                  className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={comodatosQuery.data.first}
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                  type="button"
                >Anterior</button>
                <span>Página {comodatosQuery.data.number + 1} de {Math.max(comodatosQuery.data.totalPages, 1)}</span>
                <button
                  className="h-9 rounded-md border px-3 hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={comodatosQuery.data.last}
                  onClick={() => setPage((current) => current + 1)}
                  type="button"
                >Siguiente</button>
              </div>
            </div>
          ) : null}
        </section>
      </div>
    </AppShell>
  );
}

function nombreArchivoComodatosPdf() {
  return `comodatos_prestamos_${currentTimestampInArgentinaForFilename()}.pdf`;
}

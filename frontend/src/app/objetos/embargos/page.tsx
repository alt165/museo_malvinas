"use client";

import { Download, Gavel, Search } from "lucide-react";
import Link from "next/link";
import { useMemo, useState, type FormEvent } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { exportarEmbargosObjetosPdf } from "@/features/objetos/api";
import {
  useBuscarObjetosQuery,
  useCrearEmbargoObjetoMutation,
  useEmbargosObjetosQuery,
  useLevantarEmbargoObjetoMutation
} from "@/features/objetos/queries";
import type { EmbargoObjetoResponseDTO, ObjetoMuseoResponseDTO } from "@/features/objetos/types";
import { getApiErrorMessage } from "@/features/objetos/utils";
import { descargarBlob } from "@/lib/download";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";

function fechaLocalIso() {
  const date = new Date();
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}

function formatearFecha(value?: string | null) {
  if (!value) {
    return "Sin fecha";
  }
  const [year, month, day] = value.split("-");
  return year && month && day ? `${day}/${month}/${year}` : value;
}

function nombreArchivoEmbargosPdf() {
  return `embargos_objetos_${fechaLocalIso().replaceAll("-", "")}.pdf`;
}

export default function EmbargosObjetosPage() {
  const [incluirHistoricos, setIncluirHistoricos] = useState(false);
  const [busqueda, setBusqueda] = useState({ nombre: "", numeroInventario: "" });
  const [busquedaAplicada, setBusquedaAplicada] = useState({ nombre: "", numeroInventario: "" });
  const [objetoSeleccionado, setObjetoSeleccionado] = useState<ObjetoMuseoResponseDTO | null>(null);
  const [fechaInicio, setFechaInicio] = useState(fechaLocalIso());
  const [fechaFinalizacion, setFechaFinalizacion] = useState("");
  const [observaciones, setObservaciones] = useState("");
  const [descargaError, setDescargaError] = useState<string | null>(null);
  const [descargando, setDescargando] = useState(false);

  const buscarParams = useMemo(() => ({
    nombre: busquedaAplicada.nombre,
    numeroInventario: busquedaAplicada.numeroInventario,
    page: 0,
    size: 10,
    sort: "numeroInventario,asc"
  }), [busquedaAplicada]);
  const buscarHabilitado = Boolean(busquedaAplicada.nombre || busquedaAplicada.numeroInventario);
  const objetosQuery = useBuscarObjetosQuery(buscarParams, buscarHabilitado);
  const embargosQuery = useEmbargosObjetosQuery(incluirHistoricos);
  const crearEmbargo = useCrearEmbargoObjetoMutation();
  const levantarEmbargo = useLevantarEmbargoObjetoMutation();

  const objetos = objetosQuery.data?.content ?? [];
  const embargos = embargosQuery.data ?? [];

  function handleBuscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setObjetoSeleccionado(null);
    setBusquedaAplicada({
      nombre: busqueda.nombre.trim(),
      numeroInventario: busqueda.numeroInventario.trim()
    });
  }

  function handleCrearEmbargo(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!objetoSeleccionado) {
      return;
    }
    crearEmbargo.mutate({
      objetoMuseoId: objetoSeleccionado.id,
      fechaInicio: fechaInicio || null,
      fechaFinalizacion: fechaFinalizacion || null,
      observaciones: observaciones.trim() || null
    }, {
      onSuccess: () => {
        setObjetoSeleccionado(null);
        setFechaInicio(fechaLocalIso());
        setFechaFinalizacion("");
        setObservaciones("");
      }
    });
  }

  function handleLevantar(embargo: EmbargoObjetoResponseDTO) {
    if (window.confirm(`Levantar embargo del objeto ${embargo.numeroInventario}?`)) {
      levantarEmbargo.mutate(embargo.id);
    }
  }

  async function handleDescargarPdf() {
    setDescargando(true);
    setDescargaError(null);
    try {
      const blob = await exportarEmbargosObjetosPdf();
      descargarBlob(blob, nombreArchivoEmbargosPdf());
    } catch (error) {
      setDescargaError(getApiErrorMessage(error));
    } finally {
      setDescargando(false);
    }
  }

  return (
    <AppShell requiredRoles={[...routePermissions.admin]}>
      <div className="space-y-6">
        <PageHeader
          actions={
            <div className="flex flex-wrap gap-2">
              <button
                className="inline-flex h-10 items-center gap-2 rounded-md border px-4 text-sm font-medium hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                disabled={descargando || embargosQuery.isLoading}
                onClick={handleDescargarPdf}
                type="button"
              >
                <Download className="h-4 w-4" />
                {descargando ? "Generando PDF..." : "Descargar PDF"}
              </button>
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos">
                Volver a objetos
              </Link>
            </div>
          }
          description="Registro administrativo de embargos sobre objetos patrimoniales."
          title="Embargos"
        />

        {descargaError ? <ErrorState message={descargaError} /> : null}
        {crearEmbargo.isError ? (
          <ErrorState
            message={getApiErrorMessage(crearEmbargo.error)}
            requestId={crearEmbargo.error instanceof ApiClientError ? crearEmbargo.error.requestId : undefined}
          />
        ) : null}
        {levantarEmbargo.isError ? (
          <ErrorState
            message={getApiErrorMessage(levantarEmbargo.error)}
            requestId={levantarEmbargo.error instanceof ApiClientError ? levantarEmbargo.error.requestId : undefined}
          />
        ) : null}

        <section className="rounded-lg border bg-surface p-4 shadow-sm">
          <h2 className="text-base font-semibold">Registrar embargo</h2>
          <form className="mt-4 grid gap-4 lg:grid-cols-[1fr_1fr_auto] lg:items-end" onSubmit={handleBuscar}>
            <label className="space-y-1 text-sm font-medium text-primary">
              <span>Denominación</span>
              <input
                className="h-10 w-full rounded-md border bg-white px-3 text-sm outline-none focus:border-primary"
                onChange={(event) => setBusqueda((current) => ({ ...current, nombre: event.target.value }))}
                value={busqueda.nombre}
              />
            </label>
            <label className="space-y-1 text-sm font-medium text-primary">
              <span>Número de inventario</span>
              <input
                className="h-10 w-full rounded-md border bg-white px-3 text-sm outline-none focus:border-primary"
                onChange={(event) => setBusqueda((current) => ({ ...current, numeroInventario: event.target.value }))}
                value={busqueda.numeroInventario}
              />
            </label>
            <button className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-medium text-white hover:opacity-90" type="submit">
              <Search className="h-4 w-4" />
              Buscar
            </button>
          </form>

          {objetosQuery.isLoading || objetosQuery.isFetching ? <LoadingState label="Buscando objetos..." /> : null}
          {objetosQuery.isError ? <ErrorState message={getApiErrorMessage(objetosQuery.error)} /> : null}
          {buscarHabilitado && objetosQuery.isSuccess && objetos.length === 0 ? <EmptyState title="Sin objetos" description="No hay objetos para esos filtros." /> : null}
          {objetos.length > 0 ? (
            <div className="mt-4 overflow-hidden rounded-lg border">
              <table className="w-full border-collapse text-sm">
                <thead className="bg-primary text-primary-foreground">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold text-white">Número de inventario</th>
                    <th className="px-4 py-3 text-left font-semibold text-white">Denominación</th>
                    <th className="px-4 py-3 text-left font-semibold text-white">Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {objetos.map((objeto) => (
                    <tr className="border-t" key={objeto.id}>
                      <td className="px-4 py-3 font-medium">{objeto.numeroInventario}</td>
                      <td className="px-4 py-3">{objeto.denominacionObjeto}</td>
                      <td className="px-4 py-3">
                        <button className="rounded-md border px-3 py-1.5 text-xs hover:bg-muted" onClick={() => setObjetoSeleccionado(objeto)} type="button">
                          Seleccionar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}

          <form className="mt-5 grid gap-4 rounded-md border bg-background p-4 lg:grid-cols-2" onSubmit={handleCrearEmbargo}>
            <div className="lg:col-span-2">
              <p className="text-sm text-muted-foreground">Objeto seleccionado</p>
              <p className="mt-1 text-sm font-semibold">
                {objetoSeleccionado ? `${objetoSeleccionado.numeroInventario} - ${objetoSeleccionado.denominacionObjeto}` : "Sin objeto seleccionado"}
              </p>
            </div>
            <label className="space-y-1 text-sm font-medium">
              <span>Fecha de inicio</span>
              <input className="h-10 w-full rounded-md border px-3 text-sm" onChange={(event) => setFechaInicio(event.target.value)} type="date" value={fechaInicio} />
            </label>
            <label className="space-y-1 text-sm font-medium">
              <span>Fecha de finalización</span>
              <input className="h-10 w-full rounded-md border px-3 text-sm" onChange={(event) => setFechaFinalizacion(event.target.value)} type="date" value={fechaFinalizacion} />
            </label>
            <label className="space-y-1 text-sm font-medium lg:col-span-2">
              <span>Observaciones</span>
              <textarea className="min-h-24 w-full rounded-md border px-3 py-2 text-sm" onChange={(event) => setObservaciones(event.target.value)} value={observaciones} />
            </label>
            <div className="lg:col-span-2">
              <button
                className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={!objetoSeleccionado || crearEmbargo.isPending}
                type="submit"
              >
                <Gavel className="h-4 w-4" />
                {crearEmbargo.isPending ? "Registrando..." : "Registrar embargo"}
              </button>
            </div>
          </form>
        </section>

        <section className="space-y-4 rounded-lg border bg-surface p-4 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-semibold">Objetos embargados</h2>
              <p className="mt-1 text-sm text-muted-foreground">Se muestran principalmente embargos vigentes.</p>
            </div>
            <label className="flex items-center gap-2 text-sm">
              <input checked={incluirHistoricos} className="h-4 w-4 accent-primary" onChange={(event) => setIncluirHistoricos(event.target.checked)} type="checkbox" />
              Ver históricos
            </label>
          </div>
          {embargosQuery.isLoading ? <LoadingState label="Cargando embargos..." /> : null}
          {embargosQuery.isError ? <ErrorState message={getApiErrorMessage(embargosQuery.error)} /> : null}
          {embargosQuery.isSuccess && embargos.length === 0 ? <EmptyState title="Sin embargos" description="No hay embargos vigentes registrados." /> : null}
          {embargos.length > 0 ? (
            <div className="overflow-hidden rounded-lg border">
              <table className="w-full border-collapse text-sm">
                <thead className="bg-primary text-primary-foreground">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold text-white">Número de inventario</th>
                    <th className="px-4 py-3 text-left font-semibold text-white">Denominación</th>
                    <th className="px-4 py-3 text-left font-semibold text-white">Inicio</th>
                    <th className="px-4 py-3 text-left font-semibold text-white">Finalización</th>
                    <th className="px-4 py-3 text-left font-semibold text-white">Estado</th>
                    <th className="px-4 py-3 text-left font-semibold text-white">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {embargos.map((embargo) => (
                    <tr className="border-t" key={embargo.id}>
                      <td className="px-4 py-3 font-medium">{embargo.numeroInventario}</td>
                      <td className="px-4 py-3">{embargo.denominacionObjeto}</td>
                      <td className="px-4 py-3">{formatearFecha(embargo.fechaInicio)}</td>
                      <td className="px-4 py-3">{formatearFecha(embargo.fechaFinalizacion)}</td>
                      <td className="px-4 py-3">{embargo.estado === "VIGENTE" ? "Vigente" : "Levantado"}</td>
                      <td className="px-4 py-3">
                        <div className="flex flex-wrap gap-2">
                          <Link className="rounded-md border px-3 py-1.5 text-xs hover:bg-muted" href={`/objetos/${embargo.objetoMuseoId}`}>
                            Ver objeto
                          </Link>
                          {embargo.estado === "VIGENTE" ? (
                            <button
                              className="rounded-md border border-red-200 px-3 py-1.5 text-xs text-red-700 hover:bg-red-50 disabled:opacity-60"
                              disabled={levantarEmbargo.isPending}
                              onClick={() => handleLevantar(embargo)}
                              type="button"
                            >
                              Levantar embargo
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      </div>
    </AppShell>
  );
}

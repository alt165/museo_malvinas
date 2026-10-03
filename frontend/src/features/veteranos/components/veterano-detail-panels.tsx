"use client";
import { FormLabel } from "@/components/common/form-label";

import { zodResolver } from "@hookform/resolvers/zod";
import { Link2, Medal, Network, Pencil, Search, Trash2, UserRound, type LucideIcon } from "lucide-react";
import { useRef, useState, type KeyboardEvent } from "react";
import { useForm } from "react-hook-form";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { RowActionButton, RowActionLink, RowActions } from "@/components/common/row-actions";
import { useObjetosQuery } from "@/features/objetos/queries";
import { ApiClientError } from "@/lib/errors/api-error";
import { ObjetoRelacionesGraph } from "@/features/relaciones-objetos/components/ObjetoRelacionesGraph";
import { RelacionesTable } from "@/features/relaciones-objetos/components/RelacionesTable";
import { useRelacionesPorPersonaQuery } from "@/features/relaciones-objetos/queries";
import {
  useActuacionesVeteranoQuery,
  useAsociarObjetoVeteranoMutation,
  useCrearActuacionVeteranoMutation,
  useEliminarRelacionObjetoVeteranoMutation,
  useObjetosVeteranoQuery,
  useRangosMilitaresQuery,
  useUnidadesMilitaresQuery
} from "../queries";
import { actuacionVeteranoSchema, objetoVeteranoSchema, type ActuacionVeteranoFormValues, type ObjetoVeteranoFormValues } from "../schemas";
import type { VeteranoResponseDTO } from "../types";
import { formatDate, getApiErrorMessage } from "../utils";

type DetailTab = "datos-personales" | "actuaciones-militares" | "objetos-vinculados" | "relaciones";

const detailTabs = [
  { id: "datos-personales", label: "Datos personales", icon: UserRound },
  { id: "actuaciones-militares", label: "Actuaciones militares", icon: Medal },
  { id: "objetos-vinculados", label: "Objetos vinculados", icon: Link2 },
  { id: "relaciones", label: "Relaciones", icon: Network }
] as const;

export function VeteranoDetailPanels({ canWrite, veterano }: { canWrite: boolean; veterano: VeteranoResponseDTO }) {
  const veteranoId = veterano.id;
  const actuacionesQuery = useActuacionesVeteranoQuery(veteranoId);
  const objetosQuery = useObjetosVeteranoQuery(veteranoId);
  const relacionesQuery = useRelacionesPorPersonaQuery(veteranoId);
  const objetosMuseoQuery = useObjetosQuery();
  const crearActuacion = useCrearActuacionVeteranoMutation(veteranoId);
  const asociarObjeto = useAsociarObjetoVeteranoMutation(veteranoId);
  const eliminarRelacion = useEliminarRelacionObjetoVeteranoMutation(veteranoId);
  const rangosQuery = useRangosMilitaresQuery(veterano.fuerza);
  const unidadesQuery = useUnidadesMilitaresQuery(veterano.fuerza);
  const actuacionForm = useForm<ActuacionVeteranoFormValues>({ resolver: zodResolver(actuacionVeteranoSchema), defaultValues: { rango: "", unidad: "", rangoId: null, unidadId: null, rol: "", fechaInicio: "", fechaFin: "", descripcion: "" } });
  const objetoForm = useForm<ObjetoVeteranoFormValues>({ resolver: zodResolver(objetoVeteranoSchema), defaultValues: { objetoMuseoId: 0, tipoRelacion: "", descripcion: "" } });
  const [activeTab, setActiveTab] = useState<DetailTab>("datos-personales");
  const [vistaRelaciones, setVistaRelaciones] = useState<"tabla" | "grafo">("tabla");
  const [profundidadRelaciones, setProfundidadRelaciones] = useState(1);
  const tabRefs = useRef<Array<HTMLButtonElement | null>>([]);

  function handleTabKeyDown(event: KeyboardEvent<HTMLButtonElement>, index: number) {
    let nextIndex: number | null = null;
    if (event.key === "ArrowRight") nextIndex = (index + 1) % detailTabs.length;
    if (event.key === "ArrowLeft") nextIndex = (index - 1 + detailTabs.length) % detailTabs.length;
    if (event.key === "Home") nextIndex = 0;
    if (event.key === "End") nextIndex = detailTabs.length - 1;
    if (nextIndex === null) return;
    event.preventDefault();
    const nextTab = detailTabs[nextIndex];
    setActiveTab(nextTab.id);
    tabRefs.current[nextIndex]?.focus();
  }

  return (
    <div className="overflow-hidden rounded-lg border border-primary/15 bg-white shadow-sm">
      <div aria-label="Secciones de la ficha del veterano" className="overflow-x-auto border-b border-primary/15 bg-white px-3 pt-2.5" role="tablist">
        <div className="flex w-max gap-px">
          {detailTabs.map((tab, index) => {
            const Icon = tab.icon;
            const selected = activeTab === tab.id;
            return (
              <button
                aria-controls={`panel-${tab.id}-${veteranoId}`}
                aria-selected={selected}
                className={`inline-flex shrink-0 items-center justify-center gap-2 whitespace-nowrap rounded-t-md border border-b-0 border-t-[3px] bg-white px-4 py-3 text-sm font-bold tracking-[0.01em] transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring ${selected ? "border-primary/30 border-t-primary text-primary" : "border-primary/15 border-t-transparent text-primary hover:border-primary/30"}`}
                id={`tab-${tab.id}-${veteranoId}`}
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                onKeyDown={(event) => handleTabKeyDown(event, index)}
                ref={(element) => { tabRefs.current[index] = element; }}
                role="tab"
                tabIndex={selected ? 0 : -1}
                type="button"
              >
                <Icon className="h-5 w-5 shrink-0" strokeWidth={2.25} aria-hidden="true" />
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      <section aria-labelledby={`tab-datos-personales-${veteranoId}`} className="bg-white" hidden={activeTab !== "datos-personales"} id={`panel-datos-personales-${veteranoId}`} role="tabpanel" tabIndex={0}>
        <PanelHeader icon={UserRound} title="Información personal" />
        <div className="p-4 sm:p-5">
          <div className="grid text-sm lg:w-full lg:max-w-[1200px] lg:grid-cols-[minmax(0,1.25fr)_minmax(0,1.15fr)_minmax(0,1.1fr)]">
            <dl className="px-4">
              <Dato label="Nombre" value={veterano.nombre} />
              <Dato label="Apellido" value={veterano.apellido} />
            </dl>
            <dl className="border-t border-primary/10 px-4 lg:border-l lg:border-t-0">
              <Dato label="Fuerza" value={fuerzaLabel(veterano.fuerza)} />
              <Dato label="Nacimiento" value={formatDate(veterano.fechaNacimiento)} />
            </dl>
            <dl className="border-t border-primary/10 px-4 lg:border-l lg:border-t-0">
              <Dato label="Fallecimiento" value={formatDate(veterano.fechaFallecimiento)} />
            </dl>
          </div>
          <dl className="mt-4 w-full border-t border-primary/10 px-4 pb-1 pt-4">
            <dt className="mb-3 text-base font-semibold text-primary">Observaciones / historia</dt>
            <dd className={`mt-2 whitespace-pre-wrap break-words font-medium leading-relaxed [overflow-wrap:anywhere] ${veterano.historia ? "text-foreground" : "text-muted-foreground"}`}>{veterano.historia || "Sin historia registrada"}</dd>
          </dl>
        </div>
      </section>

      <section aria-labelledby={`tab-actuaciones-militares-${veteranoId}`} className="bg-white" hidden={activeTab !== "actuaciones-militares"} id={`panel-actuaciones-militares-${veteranoId}`} role="tabpanel" tabIndex={0}>
        <PanelHeader icon={Medal} title="Actuaciones registradas" />
        <div className="space-y-4 p-5 sm:p-6">
        {canWrite ? (
          <form className="grid gap-3 rounded-lg border p-4 md:grid-cols-3" onSubmit={actuacionForm.handleSubmit((values) => crearActuacion.mutate({
            veteranoId,
            rango: null,
            unidad: null,
            rangoId: values.rangoId ?? null,
            unidadId: values.unidadId ?? null,
            rol: values.rol || null,
            fechaInicio: values.fechaInicio || null,
            fechaFin: values.fechaFin || null,
            descripcion: values.descripcion || null
          }, { onSuccess: () => actuacionForm.reset() }))}>
            <Input label="Rango" error={actuacionForm.formState.errors.rangoId?.message}><select className="h-10 w-full rounded-md border bg-background px-3 text-sm" disabled={rangosQuery.isLoading} {...actuacionForm.register("rangoId", { setValueAs: (value) => value ? Number(value) : null })}><option value="">Sin rango</option>{(rangosQuery.data ?? []).map((rango) => <option key={rango.id} value={rango.id}>{rango.nombre}</option>)}</select></Input>
            <Input label="Unidad" error={actuacionForm.formState.errors.unidadId?.message}><select className="h-10 w-full rounded-md border bg-background px-3 text-sm" disabled={unidadesQuery.isLoading} {...actuacionForm.register("unidadId", { setValueAs: (value) => value ? Number(value) : null })}><option value="">Sin unidad</option>{(unidadesQuery.data ?? []).map((unidad) => <option key={unidad.id} value={unidad.id}>{unidad.sigla ? `${unidad.sigla} - ${unidad.nombre}` : unidad.nombre}</option>)}</select></Input>
            <Input label="Rol" error={actuacionForm.formState.errors.rol?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...actuacionForm.register("rol")} /></Input>
            <Input label="Fecha inicio" error={actuacionForm.formState.errors.fechaInicio?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" type="date" {...actuacionForm.register("fechaInicio")} /></Input>
            <Input label="Fecha fin" error={actuacionForm.formState.errors.fechaFin?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" type="date" {...actuacionForm.register("fechaFin")} /></Input>
            <Input label="Descripción" error={actuacionForm.formState.errors.descripcion?.message}><textarea className="max-h-64 min-h-28 w-full resize-y overflow-y-auto rounded-md border bg-background px-3 py-2 text-sm" {...actuacionForm.register("descripcion")} /></Input>
            <button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground md:col-span-3" disabled={crearActuacion.isPending} type="submit">Agregar actuación</button>
          </form>
        ) : null}
        {crearActuacion.isError ? <ErrorState message={getApiErrorMessage(crearActuacion.error)} requestId={crearActuacion.error instanceof ApiClientError ? crearActuacion.error.requestId : undefined} /> : null}
        {actuacionesQuery.isLoading ? <LoadingState /> : null}
        {actuacionesQuery.data?.length === 0 ? <EmptyState title="Sin actuaciones" /> : null}
        {actuacionesQuery.data && actuacionesQuery.data.length > 0 ? <div className="rounded-lg border">{actuacionesQuery.data.map((a) => <div className="flex items-start justify-between gap-3 border-b p-4 text-sm last:border-b-0" key={a.id}><div className="min-w-0 flex-1"><p className="text-base font-semibold leading-snug text-primary">{a.rangoNombre || a.rango || "Sin rango"} · {a.unidadSigla ? `${a.unidadSigla} - ${a.unidadNombre || a.unidad || "Sin unidad"}` : a.unidadNombre || a.unidad || "Sin unidad"} · {a.rol || "Sin rol"}</p><p className="mt-1 text-[13px] leading-5 text-muted-foreground">{formatDate(a.fechaInicio)} - {formatDate(a.fechaFin)}</p><p className="mt-3 whitespace-pre-wrap break-words text-sm leading-6 text-foreground">{a.descripcion || "Sin descripción"}</p></div><RowActions className="shrink-0"><RowActionLink href={`/actuaciones-veteranos/${a.id}`} icon={Search} label="Ver" />{canWrite ? <RowActionLink href={`/actuaciones-veteranos/${a.id}/editar`} icon={Pencil} label="Editar" /> : null}</RowActions></div>)}</div> : null}
        </div>
      </section>
      <section aria-labelledby={`tab-objetos-vinculados-${veteranoId}`} className="bg-white" hidden={activeTab !== "objetos-vinculados"} id={`panel-objetos-vinculados-${veteranoId}`} role="tabpanel" tabIndex={0}>
        <PanelHeader icon={Link2} title="Objetos vinculados" />
        <div className="space-y-4 p-5 sm:p-6">
        {canWrite ? (
          <form className="grid gap-3 rounded-lg border p-4 md:grid-cols-[1fr_180px_1fr_auto]" onSubmit={objetoForm.handleSubmit((values) => asociarObjeto.mutate({
            veteranoId,
            objetoMuseoId: values.objetoMuseoId,
            tipoRelacion: values.tipoRelacion,
            descripcion: values.descripcion || null
          }, { onSuccess: () => objetoForm.reset({ objetoMuseoId: 0, tipoRelacion: "", descripcion: "" }) }))}>
            <Input required label="Objeto" error={objetoForm.formState.errors.objetoMuseoId?.message}><select className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...objetoForm.register("objetoMuseoId", { valueAsNumber: true })}><option value={0}>Seleccionar objeto</option>{(objetosMuseoQuery.data ?? []).map((objeto) => <option key={objeto.id} value={objeto.id}>{objeto.numeroInventario} - {objeto.denominacionObjeto}</option>)}</select></Input>
            <Input required label="Tipo relación" error={objetoForm.formState.errors.tipoRelacion?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...objetoForm.register("tipoRelacion")} /></Input>
            <Input label="Descripción" error={objetoForm.formState.errors.descripcion?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...objetoForm.register("descripcion")} /></Input>
            <div className="flex items-end"><button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground" disabled={asociarObjeto.isPending} type="submit">Asociar</button></div>
          </form>
        ) : null}
        {asociarObjeto.isError ? <ErrorState message={getApiErrorMessage(asociarObjeto.error)} requestId={asociarObjeto.error instanceof ApiClientError ? asociarObjeto.error.requestId : undefined} /> : null}
        {eliminarRelacion.isError ? <ErrorState message={getApiErrorMessage(eliminarRelacion.error)} requestId={eliminarRelacion.error instanceof ApiClientError ? eliminarRelacion.error.requestId : undefined} /> : null}
        {objetosQuery.isLoading ? <LoadingState /> : null}
        {objetosQuery.data?.length === 0 ? <EmptyState title="Sin objetos asociados" /> : null}
        {objetosQuery.data && objetosQuery.data.length > 0 ? <div className="rounded-lg border">{objetosQuery.data.map((objeto) => <div className="flex items-start justify-between gap-3 border-b p-4 text-sm last:border-b-0" key={objeto.id}><div><p className="font-medium">{objeto.objetoNombre}</p><p className="text-muted-foreground">{objeto.tipoRelacion}</p><p>{objeto.descripcion || "Sin descripción"}</p></div>{canWrite ? <RowActions><RowActionButton disabled={eliminarRelacion.isPending} icon={Trash2} label="Eliminar" onClick={() => { if (window.confirm("Eliminar relación objeto-veterano")) eliminarRelacion.mutate(objeto.id); }} variant="destructive" /></RowActions> : null}</div>)}</div> : null}
        </div>
      </section>
      <section aria-labelledby={`tab-relaciones-${veteranoId}`} className="bg-white" hidden={activeTab !== "relaciones"} id={`panel-relaciones-${veteranoId}`} role="tabpanel" tabIndex={0}>
        <PanelHeader icon={Network} title="Relaciones" />
        <div className="space-y-4 p-5 sm:p-6">
          <div className="inline-flex rounded-md border bg-white p-1">
            <button className={vistaRelaciones === "tabla" ? "rounded bg-primary px-3 py-1.5 text-sm font-medium text-white" : "rounded px-3 py-1.5 text-sm font-medium hover:bg-muted"} onClick={() => setVistaRelaciones("tabla")} type="button">Vista tabla</button>
            <button className={vistaRelaciones === "grafo" ? "rounded bg-primary px-3 py-1.5 text-sm font-medium text-white" : "rounded px-3 py-1.5 text-sm font-medium hover:bg-muted"} onClick={() => setVistaRelaciones("grafo")} type="button">Vista grafo</button>
          </div>
          {relacionesQuery.isLoading ? <LoadingState label="Cargando relaciones..." /> : null}
          {relacionesQuery.isError ? <ErrorState message={getApiErrorMessage(relacionesQuery.error)} requestId={relacionesQuery.error instanceof ApiClientError ? relacionesQuery.error.requestId : undefined} /> : null}
          {vistaRelaciones === "tabla" && relacionesQuery.data?.length === 0 ? <EmptyState description="La persona no tiene objetos relacionados." title="Sin relaciones" /> : null}
          {vistaRelaciones === "tabla" && relacionesQuery.data && relacionesQuery.data.length > 0 ? <RelacionesTable relaciones={relacionesQuery.data} /> : null}
          {vistaRelaciones === "grafo" ? <ObjetoRelacionesGraph entidadId={veteranoId} onBackToTable={() => setVistaRelaciones("tabla")} onProfundidadChange={setProfundidadRelaciones} profundidad={profundidadRelaciones} tipoCentral="PERSONA" tituloCentral={veterano.nombreCompleto} /> : null}
        </div>
      </section>
    </div>
  );
}

function PanelHeader({ icon: Icon, title }: { icon: LucideIcon; title: string }) {
  return (
    <div className="flex items-center gap-2.5 border-b border-primary/15 px-5 py-4 sm:px-6">
      <Icon aria-hidden="true" className="h-5 w-5 shrink-0 text-primary" strokeWidth={2.1} />
      <h2 className="text-base font-semibold text-primary">{title}</h2>
    </div>
  );
}

function Input({ children, error, label, required = false }: { children: React.ReactNode; error?: string; label: string; required?: boolean }) {
  return <FormLabel label={label} required={required}>{children}{error ? <p className="font-normal text-destructive">{error}</p> : null}</FormLabel>;
}

function Dato({ label, value }: { label: string; value?: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[96px_minmax(0,1fr)] items-baseline gap-2 py-2.5">
      <dt className="font-medium text-muted-foreground">{label}</dt>
      <dd className="break-words font-medium text-foreground">{value || "Sin registrar"}</dd>
    </div>
  );
}

const fuerzaLabels: Record<string, string> = {
  EJERCITO: "Ejército",
  ARMADA: "Armada",
  FUERZA_AEREA: "Fuerza aérea",
  PREFECTURA: "Prefectura",
  GENDARMERIA: "Gendarmería",
  CIVIL: "Civil"
};

function fuerzaLabel(fuerza?: string | null) {
  const key = fuerza?.trim().toUpperCase().replace(/\s+/g, "_");
  return (key && fuerzaLabels[key]) || fuerza || undefined;
}

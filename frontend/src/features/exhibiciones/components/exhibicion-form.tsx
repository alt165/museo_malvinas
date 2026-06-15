
"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { useForm, useWatch } from "react-hook-form";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { useExhibicionQuery, useObjetosDisponibilidadExhibicionQuery, useObjetosParaRepetirExhibicionQuery } from "../queries";
import type { ExhibicionRequestDTO, ExhibicionResponseDTO, ObjetoDisponibilidadExhibicionResponseDTO } from "../types";
import { estadosExhibicion, tiposExhibicion } from "../types";
import { exhibicionSchema, type ExhibicionFormValues } from "../schemas";
import { formatDate, getApiErrorMessage, getValidationErrors } from "../utils";
import { ApiClientError } from "@/lib/errors/api-error";

type ExhibicionFormProps = {
  initialValue?: ExhibicionResponseDTO;
  isSubmitting?: boolean;
  repetirExhibicionId?: number;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: ExhibicionRequestDTO) => void;
};

type ObjetoIncluido = {
  id: number;
  numeroInventario: string;
  denominacion: string;
};

export function ExhibicionForm({ initialValue, isSubmitting = false, repetirExhibicionId, onSubmit, submitError, submitLabel }: ExhibicionFormProps) {
  const initialObjetos = useMemo<ObjetoIncluido[]>(() => (initialValue?.objetos ?? []).map((objeto) => ({
    id: objeto.objetoMuseoId,
    numeroInventario: objeto.objetoNumeroInventario || `Objeto ${objeto.objetoMuseoId}`,
    denominacion: objeto.objetoNombre
  })), [initialValue?.objetos]);
  const [objetosIncluidos, setObjetosIncluidos] = useState<ObjetoIncluido[]>(initialObjetos);
  const [textoBusqueda, setTextoBusqueda] = useState("");
  const [textoAplicado, setTextoAplicado] = useState("");
  const [page, setPage] = useState(0);
  const repeticionPrecargadaIdRef = useRef<number | null>(null);
  const [mensajeRepeticion, setMensajeRepeticion] = useState<string | null>(null);
  const exhibicionARepetirId = repetirExhibicionId && Number.isFinite(repetirExhibicionId) ? repetirExhibicionId : undefined;

  const {
    formState: { errors },
    getValues,
    handleSubmit,
    register,
    reset,
    setError,
    setValue,
    control
  } = useForm<ExhibicionFormValues>({
    resolver: zodResolver(exhibicionSchema),
    defaultValues: {
      nombre: initialValue?.nombre ?? "",
      descripcion: initialValue?.descripcion ?? "",
      tipo: initialValue?.tipo ?? "TEMPORAL",
      fechaInicio: initialValue?.fechaInicio ?? new Date().toISOString().slice(0, 10),
      fechaFin: initialValue?.fechaFin ?? "",
      estado: initialValue?.estado ?? "PLANIFICADA"
    }
  });

  const fechaInicio = useWatch({ control, name: "fechaInicio" });
  const fechaFin = useWatch({ control, name: "fechaFin" });
  const today = new Date().toISOString().slice(0, 10);
  const tipoDerivado = fechaFin ? "TEMPORAL" : "PERMANENTE";
  const estadosDisponibles = useMemo(() => estadosExhibicion.filter((estado) => estado !== "CANCELADA"), []);
  const objetoIds = useMemo(() => objetosIncluidos.map((objeto) => objeto.id), [objetosIncluidos]);
  const hayBusqueda = textoAplicado.trim().length > 0;
  const disponibilidadQuery = useObjetosDisponibilidadExhibicionQuery({
    texto: textoAplicado,
    fechaInicio,
    fechaFin: fechaFin || null,
    exhibicionId: initialValue?.id,
    page,
    size: 10
  }, Boolean(fechaInicio) && hayBusqueda);
  const resultados = disponibilidadQuery.data?.content ?? [];
  const exhibicionARepetirQuery = useExhibicionQuery(exhibicionARepetirId ?? NaN);
  const objetosParaRepetirQuery = useObjetosParaRepetirExhibicionQuery({
    exhibicionId: exhibicionARepetirId ?? 0,
    fechaInicioNueva: fechaInicio,
    fechaFinNueva: fechaFin || null
  }, Boolean(exhibicionARepetirId && fechaInicio));
  const objetosParaRepetir = useMemo(() => objetosParaRepetirQuery.data ?? [], [objetosParaRepetirQuery.data]);
  const conflictosRepeticionIncluidos = useMemo(() => objetosParaRepetir.filter((objeto) => !objeto.disponible && objetoIds.includes(objeto.objetoId)), [objetoIds, objetosParaRepetir]);

  useEffect(() => {
    setValue("tipo", tipoDerivado, { shouldDirty: true, shouldValidate: true });
  }, [setValue, tipoDerivado]);

  useEffect(() => {
    const exhibicion = exhibicionARepetirQuery.data;
    if (!exhibicion || initialValue || repeticionPrecargadaIdRef.current === exhibicion.id) return;
    reset({
      nombre: `${exhibicion.nombre} (repetición)`,
      descripcion: exhibicion.descripcion ?? "",
      tipo: "PERMANENTE",
      fechaInicio: getValues("fechaInicio"),
      fechaFin: "",
      estado: "PLANIFICADA"
    });
    repeticionPrecargadaIdRef.current = exhibicion.id;
  }, [exhibicionARepetirQuery.data, getValues, initialValue, reset]);

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);
    Object.entries(validationErrors).forEach(([field, message]) => {
      if (field === "nombre" || field === "descripcion" || field === "tipo" || field === "fechaInicio" || field === "fechaFin" || field === "estado") {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  function aplicarBusqueda() {
    setPage(0);
    setTextoAplicado(textoBusqueda.trim());
  }

  function agregarObjeto(objeto: ObjetoDisponibilidadExhibicionResponseDTO) {
    if (!objeto.disponible) return;
    setObjetosIncluidos((actuales) => actuales.some((item) => item.id === objeto.objetoId)
      ? actuales
      : [...actuales, { id: objeto.objetoId, numeroInventario: objeto.numeroInventario, denominacion: objeto.denominacion }]);
  }

  function agregarObjetosDisponiblesRepeticion() {
    const disponibles = objetosParaRepetir.filter((objeto) => objeto.disponible && !objetoIds.includes(objeto.objetoId));
    if (disponibles.length === 0) {
      setMensajeRepeticion("No hay objetos disponibles para repetir esta exhibición en el rango de fechas seleccionado.");
      return;
    }
    setObjetosIncluidos((actuales) => [
      ...actuales,
      ...disponibles.map((objeto) => ({ id: objeto.objetoId, numeroInventario: objeto.numeroInventario, denominacion: objeto.denominacion }))
    ]);
    setMensajeRepeticion(`Se agregaron ${disponibles.length} objeto(s) disponible(s) a la nueva exhibición.`);
  }

  function quitarObjeto(objetoId: number) {
    setObjetosIncluidos((actuales) => actuales.filter((objeto) => objeto.id !== objetoId));
  }

  function submit(values: ExhibicionFormValues) {
    if (!initialValue && values.fechaInicio < today) {
      setError("fechaInicio", { message: "La fecha de inicio no puede ser anterior a la fecha actual." });
      return;
    }
    if (conflictosRepeticionIncluidos.length > 0) {
      setMensajeRepeticion("Hay objetos incluidos que ya no están disponibles para el rango de fechas seleccionado. Quite esos objetos o modifique las fechas antes de guardar.");
      return;
    }
    if (!values.fechaFin) {
      const confirmado = window.confirm("La exhibición será cargada como permanente porque no se indicó fecha de finalización. ¿Desea continuar?");
      if (!confirmado) return;
    }
    onSubmit({
      nombre: values.nombre.trim(),
      descripcion: values.descripcion?.trim() || null,
      tipo: tipoDerivado,
      fechaInicio: values.fechaInicio,
      fechaFin: values.fechaFin || null,
      estado: values.estado,
      objetoIds
    });
  }

  return (
    <form className="w-full space-y-5 rounded-lg border p-5" onSubmit={handleSubmit(submit)}>
      {exhibicionARepetirId ? <RepeticionHeader query={exhibicionARepetirQuery} /> : null}
      <Field error={errors.nombre?.message} label="Nombre">
        <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" {...register("nombre")} />
      </Field>
      <Field error={errors.descripcion?.message} label="Descripcion">
        <textarea className="min-h-28 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" {...register("descripcion")} />
      </Field>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field error={errors.tipo?.message} label="Tipo">
          <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" disabled value={tipoDerivado}>
            {tiposExhibicion.map((tipo) => <option key={tipo} value={tipo}>{tipo}</option>)}
          </select>
          <input type="hidden" value={tipoDerivado} {...register("tipo")} />
        </Field>
        <Field error={errors.estado?.message} label="Estado">
          <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" {...register("estado")}>{estadosDisponibles.map((estado) => <option key={estado} value={estado}>{estado}</option>)}</select>
        </Field>
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field error={errors.fechaInicio?.message} label="Fecha de inicio">
          <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" min={!initialValue ? today : undefined} type="date" {...register("fechaInicio")} />
        </Field>
        <Field error={errors.fechaFin?.message} label="Fecha de finalización">
          <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" type="date" {...register("fechaFin")} />
        </Field>
      </div>
      {!fechaFin ? <div className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">La exhibición será cargada como permanente porque no se indicó fecha de finalización. La disponibilidad se evaluará como exhibición permanente.</div> : <div className="rounded-md border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-900">La exhibición será cargada como temporal porque se indicó fecha de finalización.</div>}

      {exhibicionARepetirId ? (
        <section className="space-y-4 rounded-lg border bg-surface p-4">
          <div className="space-y-1">
            <h2 className="text-base font-semibold text-primary">Objetos de la exhibición original</h2>
            <p className="text-sm text-muted-foreground">La disponibilidad se recalcula con las fechas nuevas de esta exhibición.</p>
          </div>
          {mensajeRepeticion ? <div className="rounded-md border bg-muted/40 px-3 py-2 text-sm">{mensajeRepeticion}</div> : null}
          {objetosParaRepetirQuery.isLoading || objetosParaRepetirQuery.isFetching ? <LoadingState label="Verificando disponibilidad de objetos..." /> : null}
          {objetosParaRepetirQuery.isError ? <ErrorState message={getApiErrorMessage(objetosParaRepetirQuery.error)} requestId={objetosParaRepetirQuery.error instanceof ApiClientError ? objetosParaRepetirQuery.error.requestId : undefined} /> : null}
          {!objetosParaRepetirQuery.isFetching && objetosParaRepetir.length === 0 ? <div className="rounded-md border border-dashed p-4 text-sm text-muted-foreground">La exhibición original no tiene objetos registrados.</div> : null}
          {objetosParaRepetir.length > 0 ? (
            <>
              <div className="flex justify-end">
                <button className="rounded-md border px-3 py-2 text-sm font-medium hover:bg-muted disabled:opacity-60" disabled={isSubmitting || objetosParaRepetirQuery.isFetching} onClick={agregarObjetosDisponiblesRepeticion} type="button">Agregar objetos disponibles a la nueva exhibición</button>
              </div>
              <div className="space-y-2">
                {objetosParaRepetir.map((objeto) => {
                  const yaIncluido = objetoIds.includes(objeto.objetoId);
                  return <ObjetoDisponibilidadRow key={objeto.objetoId} objeto={objeto} yaIncluido={yaIncluido} onAgregar={agregarObjeto} disabled={isSubmitting} />;
                })}
              </div>
            </>
          ) : null}
        </section>
      ) : null}

      <section className="space-y-3 rounded-lg border bg-surface p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-base font-semibold text-primary">Objetos incluidos en la exhibición</h2>
          <span className="text-xs text-muted-foreground">{objetosIncluidos.length} objeto(s)</span>
        </div>
        {conflictosRepeticionIncluidos.length > 0 ? <div className="space-y-1 rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">{conflictosRepeticionIncluidos.map((objeto) => <p key={objeto.objetoId}>El objeto {objeto.numeroInventario} - {objeto.denominacion} no puede permanecer en esta exhibición porque también está incluido en “{objeto.exhibicionConflictoNombre}” en un rango de fechas coincidente.</p>)}</div> : null}
        {objetosIncluidos.length === 0 ? <div className="rounded-md border border-dashed p-5 text-sm text-muted-foreground">No hay objetos incluidos en la exhibición.</div> : (
          <div className="overflow-hidden rounded-md border">
            <table className="w-full border-collapse text-sm">
              <thead className="bg-muted/60"><tr><th className="px-4 py-3 text-left font-medium">Inventario</th><th className="px-4 py-3 text-left font-medium">Denominación</th><th className="px-4 py-3 text-right font-medium">Acciones</th></tr></thead>
              <tbody>{objetosIncluidos.map((objeto) => <tr className="border-t" key={objeto.id}><td className="px-4 py-3 align-top font-medium">{objeto.numeroInventario}</td><td className="px-4 py-3 align-top">{objeto.denominacion}</td><td className="px-4 py-3 text-right align-top"><button className="rounded-md border px-3 py-1.5 text-xs text-destructive hover:bg-muted disabled:opacity-60" disabled={isSubmitting} onClick={() => quitarObjeto(objeto.id)} type="button">Quitar</button></td></tr>)}</tbody>
            </table>
          </div>
        )}
      </section>

      <section className="space-y-4 rounded-lg border bg-surface p-4">
        <div className="space-y-1">
          <h2 className="text-base font-semibold text-primary">Buscar objetos disponibles</h2>
          <p className="text-sm text-muted-foreground">Ingrese texto para buscar por número de inventario, denominación o descripción. La disponibilidad se calcula con las fechas de la exhibición.</p>
        </div>
        {!fechaInicio ? <div className="rounded-md border border-dashed p-4 text-sm text-muted-foreground">Indique fecha de inicio antes de buscar objetos.</div> : null}
        <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
          <input className="h-10 rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" disabled={!fechaInicio} onChange={(event) => setTextoBusqueda(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") { event.preventDefault(); aplicarBusqueda(); } }} placeholder="Buscar objeto" value={textoBusqueda} />
          <button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-60" disabled={!fechaInicio || !textoBusqueda.trim()} onClick={aplicarBusqueda} type="button">Buscar</button>
        </div>
        {disponibilidadQuery.isLoading || disponibilidadQuery.isFetching ? <LoadingState label="Buscando objetos..." /> : null}
        {disponibilidadQuery.isError ? <ErrorState message={getApiErrorMessage(disponibilidadQuery.error)} requestId={disponibilidadQuery.error instanceof ApiClientError ? disponibilidadQuery.error.requestId : undefined} /> : null}
        {hayBusqueda && !disponibilidadQuery.isFetching && resultados.length === 0 ? <div className="rounded-md border border-dashed p-4 text-sm text-muted-foreground">No hay objetos que coincidan con la búsqueda.</div> : null}
        {resultados.length > 0 ? <div className="space-y-2">{resultados.map((objeto) => <ObjetoDisponibilidadRow key={objeto.objetoId} objeto={objeto} yaIncluido={objetoIds.includes(objeto.objetoId)} onAgregar={agregarObjeto} disabled={isSubmitting} />)}</div> : null}
        {disponibilidadQuery.data && disponibilidadQuery.data.totalPages > 1 ? <div className="flex items-center justify-end gap-2"><button className="rounded-md border px-3 py-1.5 text-sm disabled:opacity-60" disabled={page <= 0 || disponibilidadQuery.isFetching} onClick={() => setPage((current) => Math.max(0, current - 1))} type="button">Anterior</button><span className="text-sm text-muted-foreground">Página {page + 1} de {disponibilidadQuery.data.totalPages}</span><button className="rounded-md border px-3 py-1.5 text-sm disabled:opacity-60" disabled={page + 1 >= disponibilidadQuery.data.totalPages || disponibilidadQuery.isFetching} onClick={() => setPage((current) => current + 1)} type="button">Siguiente</button></div> : null}
      </section>

      <div className="flex items-center gap-3">
        <button className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60" disabled={isSubmitting || conflictosRepeticionIncluidos.length > 0} type="submit">{isSubmitting ? "Guardando..." : submitLabel}</button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/exhibiciones">Cancelar</Link>
      </div>
    </form>
  );
}

function RepeticionHeader({ query }: { query: ReturnType<typeof useExhibicionQuery> }) {
  if (query.isLoading) return <LoadingState label="Cargando exhibición finalizada..." />;
  if (query.isError) return <ErrorState message={getApiErrorMessage(query.error)} requestId={query.error instanceof ApiClientError ? query.error.requestId : undefined} />;
  if (!query.data) return null;
  return (
    <div className="rounded-md border bg-muted/40 px-3 py-2 text-sm">
      <p className="font-medium">Exhibición finalizada seleccionada: {query.data.nombre}</p>
      <p className="text-muted-foreground">Periodo original: {formatDate(query.data.fechaInicio)} - {formatDate(query.data.fechaFin)}</p>
    </div>
  );
}

function ObjetoDisponibilidadRow({ disabled, objeto, onAgregar, yaIncluido }: { disabled?: boolean; objeto: ObjetoDisponibilidadExhibicionResponseDTO; onAgregar: (objeto: ObjetoDisponibilidadExhibicionResponseDTO) => void; yaIncluido: boolean }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 rounded-md border p-3 text-sm">
      <div>
        <p className="font-medium">{objeto.numeroInventario} - {objeto.denominacion}</p>
        {objeto.disponible ? <p className="text-emerald-700">Disponible</p> : <p className="text-destructive">No disponible: incluido en “{objeto.exhibicionConflictoNombre}” desde {formatDate(objeto.exhibicionConflictoFechaInicio)} hasta {objeto.exhibicionConflictoPermanente ? "permanente" : formatDate(objeto.exhibicionConflictoFechaFin)}</p>}
      </div>
      <button className="rounded-md border px-3 py-1.5 text-xs hover:bg-muted disabled:opacity-60" disabled={!objeto.disponible || yaIncluido || disabled} onClick={() => onAgregar(objeto)} type="button">{yaIncluido ? "Incluido" : "Agregar"}</button>
    </div>
  );
}

function Field({ children, error, label }: { children: ReactNode; error?: string; label: string }) {
  return <label className="space-y-2 text-sm font-medium"><span>{label}</span>{children}{error ? <p className="font-normal text-destructive">{error}</p> : null}</label>;
}

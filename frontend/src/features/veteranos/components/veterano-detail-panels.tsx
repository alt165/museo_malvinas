"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { useObjetosQuery } from "@/features/objetos/queries";
import { ApiClientError } from "@/lib/errors/api-error";
import {
  useActuacionesVeteranoQuery,
  useAsociarObjetoVeteranoMutation,
  useCrearActuacionVeteranoMutation,
  useEliminarRelacionObjetoVeteranoMutation,
  useObjetosVeteranoQuery
} from "../queries";
import { actuacionVeteranoSchema, objetoVeteranoSchema, type ActuacionVeteranoFormValues, type ObjetoVeteranoFormValues } from "../schemas";
import { formatDate, getApiErrorMessage } from "../utils";

export function VeteranoDetailPanels({ canWrite, veteranoId }: { canWrite: boolean; veteranoId: number }) {
  const actuacionesQuery = useActuacionesVeteranoQuery(veteranoId);
  const objetosQuery = useObjetosVeteranoQuery(veteranoId);
  const objetosMuseoQuery = useObjetosQuery();
  const crearActuacion = useCrearActuacionVeteranoMutation(veteranoId);
  const asociarObjeto = useAsociarObjetoVeteranoMutation(veteranoId);
  const eliminarRelacion = useEliminarRelacionObjetoVeteranoMutation(veteranoId);
  const actuacionForm = useForm<ActuacionVeteranoFormValues>({ resolver: zodResolver(actuacionVeteranoSchema), defaultValues: { rango: "", unidad: "", rol: "", fechaInicio: "", fechaFin: "", descripcion: "" } });
  const objetoForm = useForm<ObjetoVeteranoFormValues>({ resolver: zodResolver(objetoVeteranoSchema), defaultValues: { objetoMuseoId: 0, tipoRelacion: "", descripcion: "" } });

  return (
    <div className="space-y-8">
      <section className="space-y-4">
        <h2 className="text-lg font-semibold">Actuaciones históricas</h2>
        {canWrite ? (
          <form className="grid gap-3 rounded-lg border p-4 md:grid-cols-3" onSubmit={actuacionForm.handleSubmit((values) => crearActuacion.mutate({
            veteranoId,
            rango: values.rango || null,
            unidad: values.unidad || null,
            rol: values.rol || null,
            fechaInicio: values.fechaInicio || null,
            fechaFin: values.fechaFin || null,
            descripcion: values.descripcion || null
          }, { onSuccess: () => actuacionForm.reset() }))}>
            <Input label="Rango" error={actuacionForm.formState.errors.rango?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...actuacionForm.register("rango")} /></Input>
            <Input label="Unidad" error={actuacionForm.formState.errors.unidad?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...actuacionForm.register("unidad")} /></Input>
            <Input label="Rol" error={actuacionForm.formState.errors.rol?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...actuacionForm.register("rol")} /></Input>
            <Input label="Fecha inicio" error={actuacionForm.formState.errors.fechaInicio?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" type="date" {...actuacionForm.register("fechaInicio")} /></Input>
            <Input label="Fecha fin" error={actuacionForm.formState.errors.fechaFin?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" type="date" {...actuacionForm.register("fechaFin")} /></Input>
            <Input label="Descripción" error={actuacionForm.formState.errors.descripcion?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...actuacionForm.register("descripcion")} /></Input>
            <button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground md:col-span-3" disabled={crearActuacion.isPending} type="submit">Agregar actuación</button>
          </form>
        ) : null}
        {crearActuacion.isError ? <ErrorState message={getApiErrorMessage(crearActuacion.error)} requestId={crearActuacion.error instanceof ApiClientError ? crearActuacion.error.requestId : undefined} /> : null}
        {actuacionesQuery.isLoading ? <LoadingState /> : null}
        {actuacionesQuery.data?.length === 0 ? <EmptyState title="Sin actuaciones" /> : null}
        {actuacionesQuery.data && actuacionesQuery.data.length > 0 ? <div className="rounded-lg border">{actuacionesQuery.data.map((a) => <div className="flex items-start justify-between gap-3 border-b p-4 text-sm last:border-b-0" key={a.id}><div><p className="font-medium">{a.rango || "Sin rango"} · {a.unidad || "Sin unidad"} · {a.rol || "Sin rol"}</p><p className="text-muted-foreground">{formatDate(a.fechaInicio)} - {formatDate(a.fechaFin)}</p><p className="mt-2">{a.descripcion || "Sin descripción"}</p></div><div className="flex shrink-0 gap-2"><Link className="rounded-md border px-3 py-1.5 text-xs hover:bg-muted" href={`/actuaciones-veteranos/${a.id}`}>Ver</Link>{canWrite ? <Link className="rounded-md border px-3 py-1.5 text-xs hover:bg-muted" href={`/actuaciones-veteranos/${a.id}/editar`}>Editar</Link> : null}</div></div>)}</div> : null}
      </section>
      <section className="space-y-4">
        <h2 className="text-lg font-semibold">Objetos asociados</h2>
        {canWrite ? (
          <form className="grid gap-3 rounded-lg border p-4 md:grid-cols-[1fr_180px_1fr_auto]" onSubmit={objetoForm.handleSubmit((values) => asociarObjeto.mutate({
            veteranoId,
            objetoMuseoId: values.objetoMuseoId,
            tipoRelacion: values.tipoRelacion,
            descripcion: values.descripcion || null
          }, { onSuccess: () => objetoForm.reset({ objetoMuseoId: 0, tipoRelacion: "", descripcion: "" }) }))}>
            <Input label="Objeto" error={objetoForm.formState.errors.objetoMuseoId?.message}><select className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...objetoForm.register("objetoMuseoId", { valueAsNumber: true })}><option value={0}>Seleccionar objeto</option>{(objetosMuseoQuery.data ?? []).map((objeto) => <option key={objeto.id} value={objeto.id}>{objeto.numeroInventario} - {objeto.denominacionObjeto}</option>)}</select></Input>
            <Input label="Tipo relación" error={objetoForm.formState.errors.tipoRelacion?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...objetoForm.register("tipoRelacion")} /></Input>
            <Input label="Descripción" error={objetoForm.formState.errors.descripcion?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...objetoForm.register("descripcion")} /></Input>
            <div className="flex items-end"><button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground" disabled={asociarObjeto.isPending} type="submit">Asociar</button></div>
          </form>
        ) : null}
        {asociarObjeto.isError ? <ErrorState message={getApiErrorMessage(asociarObjeto.error)} requestId={asociarObjeto.error instanceof ApiClientError ? asociarObjeto.error.requestId : undefined} /> : null}
        {eliminarRelacion.isError ? <ErrorState message={getApiErrorMessage(eliminarRelacion.error)} requestId={eliminarRelacion.error instanceof ApiClientError ? eliminarRelacion.error.requestId : undefined} /> : null}
        {objetosQuery.isLoading ? <LoadingState /> : null}
        {objetosQuery.data?.length === 0 ? <EmptyState title="Sin objetos asociados" /> : null}
        {objetosQuery.data && objetosQuery.data.length > 0 ? <div className="rounded-lg border">{objetosQuery.data.map((objeto) => <div className="flex items-start justify-between gap-3 border-b p-4 text-sm last:border-b-0" key={objeto.id}><div><p className="font-medium">{objeto.objetoNombre}</p><p className="text-muted-foreground">{objeto.tipoRelacion}</p><p>{objeto.descripcion || "Sin descripción"}</p></div>{canWrite ? <button className="rounded-md border px-3 py-1.5 text-xs hover:bg-muted" disabled={eliminarRelacion.isPending} onClick={() => { if (window.confirm("Eliminar relación objeto-veterano")) eliminarRelacion.mutate(objeto.id); }} type="button">Eliminar</button> : null}</div>)}</div> : null}
      </section>
    </div>
  );
}

function Input({ children, error, label }: { children: React.ReactNode; error?: string; label: string }) {
  return <label className="space-y-2 text-sm font-medium"><span>{label}</span>{children}{error ? <p className="font-normal text-destructive">{error}</p> : null}</label>;
}

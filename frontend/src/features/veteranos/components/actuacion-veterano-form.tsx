"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { LoadingState } from "@/components/common/loading-state";
import type { ActuacionVeteranoRequestDTO, ActuacionVeteranoResponseDTO, UnidadMilitarResponseDTO } from "../types";
import { actuacionVeteranoSchema, type ActuacionVeteranoFormValues } from "../schemas";
import { useRangosMilitaresQuery, useUnidadesMilitaresQuery, useVeteranosQuery } from "../queries";
import { fuerzaLabel, getValidationErrors } from "../utils";

type ActuacionVeteranoFormProps = {
  initialValue?: ActuacionVeteranoResponseDTO;
  fixedVeteranoId?: number;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: ActuacionVeteranoRequestDTO) => void;
};

export function ActuacionVeteranoForm({
  fixedVeteranoId,
  initialValue,
  isSubmitting = false,
  onSubmit,
  submitError,
  submitLabel
}: ActuacionVeteranoFormProps) {
  const veteranosQuery = useVeteranosQuery();
  const [busquedaUnidad, setBusquedaUnidad] = useState("");
  const [unidadSeleccionada, setUnidadSeleccionada] = useState<UnidadMilitarResponseDTO | null>(
    initialValue?.unidadId
      ? {
          id: initialValue.unidadId,
          fuerza: "CIVIL",
          nombre: initialValue.unidadNombre ?? initialValue.unidad ?? "Unidad seleccionada",
          sigla: initialValue.unidadSigla ?? null
        }
      : null
  );
  const {
    control,
    formState: { errors },
    handleSubmit,
    register,
    setError,
    setValue
  } = useForm<ActuacionVeteranoFormValues>({
    resolver: zodResolver(actuacionVeteranoSchema),
    defaultValues: {
      veteranoId: fixedVeteranoId ?? initialValue?.veteranoId ?? 0,
      rango: initialValue?.rango ?? "",
      unidad: initialValue?.unidad ?? "",
      rangoId: initialValue?.rangoId ?? null,
      unidadId: initialValue?.unidadId ?? null,
      rol: initialValue?.rol ?? "",
      fechaInicio: initialValue?.fechaInicio ?? "",
      fechaFin: initialValue?.fechaFin ?? "",
      descripcion: initialValue?.descripcion ?? ""
    }
  });
  const watchedVeteranoId = useWatch({ control, name: "veteranoId" });
  const watchedUnidadId = useWatch({ control, name: "unidadId" });
  const veteranoId = fixedVeteranoId ?? watchedVeteranoId ?? 0;
  const veteranos = useMemo(() => veteranosQuery.data ?? [], [veteranosQuery.data]);
  const veteranoSeleccionado = veteranos.find((veterano) => veterano.id === veteranoId);
  const fuerzaSeleccionada = veteranoSeleccionado?.fuerza;
  const rangosQuery = useRangosMilitaresQuery(fuerzaSeleccionada);
  const unidadesQuery = useUnidadesMilitaresQuery(fuerzaSeleccionada, busquedaUnidad);

  const unidadActual = useMemo(() => {
    if (!watchedUnidadId) {
      return null;
    }
    return unidadSeleccionada?.id === watchedUnidadId
      ? unidadSeleccionada
      : (unidadesQuery.data ?? []).find((unidad) => unidad.id === watchedUnidadId) ?? unidadSeleccionada;
  }, [unidadSeleccionada, unidadesQuery.data, watchedUnidadId]);

  function handleVeteranoChange(value: string) {
    const nextVeteranoId = Number(value);
    setValue("veteranoId", nextVeteranoId, { shouldDirty: true, shouldValidate: true });
    setValue("rangoId", null, { shouldDirty: true, shouldValidate: true });
    setValue("unidadId", null, { shouldDirty: true, shouldValidate: true });
    setValue("rango", "", { shouldDirty: true });
    setValue("unidad", "", { shouldDirty: true });
    setUnidadSeleccionada(null);
    setBusquedaUnidad("");
  }

  function handleSeleccionarUnidad(unidad: UnidadMilitarResponseDTO) {
    setValue("unidadId", unidad.id, { shouldDirty: true, shouldValidate: true });
    setValue("unidad", unidad.nombre, { shouldDirty: true });
    setUnidadSeleccionada(unidad);
  }

  function handleLimpiarUnidad() {
    setValue("unidadId", null, { shouldDirty: true, shouldValidate: true });
    setValue("unidad", "", { shouldDirty: true });
    setUnidadSeleccionada(null);
  }

  function formatoUnidad(unidad: UnidadMilitarResponseDTO) {
    return unidad.sigla ? `${unidad.sigla} - ${unidad.nombre}` : unidad.nombre;
  }

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);
    Object.entries(validationErrors).forEach(([field, message]) => {
      if (
        field === "veteranoId" ||
        field === "rango" ||
        field === "unidad" ||
        field === "rangoId" ||
        field === "unidadId" ||
        field === "rol" ||
        field === "fechaInicio" ||
        field === "fechaFin" ||
        field === "descripcion"
      ) {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  if (veteranosQuery.isLoading) {
    return <LoadingState label="Cargando veteranos..." />;
  }

  return (
    <form
      className="w-full space-y-5 rounded-lg border p-5"
      onSubmit={handleSubmit((values) => {
        const selectedVeteranoId = fixedVeteranoId ?? values.veteranoId ?? 0;

        if (!selectedVeteranoId) {
          setError("veteranoId", { message: "Selecciona un veterano" });
          return;
        }

        onSubmit({
          veteranoId: selectedVeteranoId,
          rango: values.rango?.trim() || null,
          unidad: values.unidad?.trim() || null,
          rangoId: values.rangoId ?? null,
          unidadId: values.unidadId ?? null,
          rol: values.rol?.trim() || null,
          fechaInicio: values.fechaInicio || null,
          fechaFin: values.fechaFin || null,
          descripcion: values.descripcion?.trim() || null
        });
      })}
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Veterano" error={errors.veteranoId?.message}>
          {fixedVeteranoId ? (
            <input
              className="h-10 w-full rounded-md border bg-muted px-3 text-sm"
              disabled
              value={veteranoSeleccionado?.nombreCompleto ?? initialValue?.veteranoNombreCompleto ?? "Veterano seleccionado"}
            />
          ) : (
            <select
              className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
              onChange={(event) => handleVeteranoChange(event.target.value)}
              value={veteranoId}
            >
              <option value={0}>Seleccionar veterano</option>
              {veteranos.map((veterano) => (
                <option key={veterano.id} value={veterano.id}>
                  {veterano.nombreCompleto}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Fuerza">
          <input
            className="h-10 w-full rounded-md border bg-muted px-3 text-sm"
            disabled
            value={fuerzaSeleccionada ? fuerzaLabel(fuerzaSeleccionada) : "Sin fuerza seleccionada"}
          />
        </Field>
      </div>
      <div className="grid gap-5 sm:grid-cols-3">
        <Field label="Rango" error={errors.rangoId?.message ?? errors.rango?.message}>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring disabled:bg-muted"
            disabled={!fuerzaSeleccionada || rangosQuery.isLoading}
            {...register("rangoId", {
              setValueAs: (value) => value ? Number(value) : null,
              onChange: (event) => {
                const rango = (rangosQuery.data ?? []).find((item) => item.id === Number(event.target.value));
                setValue("rango", rango?.nombre ?? "", { shouldDirty: true });
              }
            })}
          >
            <option value="">{fuerzaSeleccionada ? "Seleccionar rango" : "Seleccione un veterano"}</option>
            {(rangosQuery.data ?? []).map((rango) => (
              <option key={rango.id} value={rango.id}>
                {rango.nombre}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Unidad" error={errors.unidadId?.message ?? errors.unidad?.message}>
          <div className="space-y-2">
            <input
              className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring disabled:bg-muted"
              disabled={!fuerzaSeleccionada}
              onChange={(event) => setBusquedaUnidad(event.target.value)}
              placeholder={fuerzaSeleccionada ? "Buscar por nombre o sigla" : "Seleccione un veterano"}
              type="search"
              value={busquedaUnidad}
            />
            {unidadActual ? (
              <div className="flex items-center justify-between gap-2 rounded-md border bg-muted/40 px-3 py-2 text-xs">
                <span className="font-medium">{formatoUnidad(unidadActual)}</span>
                <button className="text-destructive hover:underline" onClick={handleLimpiarUnidad} type="button">Quitar</button>
              </div>
            ) : null}
            {fuerzaSeleccionada && !unidadActual ? (
              <div className="max-h-44 overflow-y-auto rounded-md border bg-white">
                {unidadesQuery.isLoading ? <p className="px-3 py-2 text-xs text-muted-foreground">Cargando unidades...</p> : null}
                {!unidadesQuery.isLoading && (unidadesQuery.data ?? []).length === 0 ? <p className="px-3 py-2 text-xs text-muted-foreground">Sin unidades disponibles.</p> : null}
                {(unidadesQuery.data ?? []).map((unidad) => (
                  <button
                    className="block w-full border-b px-3 py-2 text-left text-xs last:border-b-0 hover:bg-muted"
                    key={unidad.id}
                    onClick={() => handleSeleccionarUnidad(unidad)}
                    type="button"
                  >
                    <span className="block font-medium">{formatoUnidad(unidad)}</span>
                    {unidad.tipoUnidad ? <span className="text-muted-foreground">{unidad.tipoUnidad}</span> : null}
                  </button>
                ))}
              </div>
            ) : null}
          </div>
        </Field>
        <Field label="Rol" error={errors.rol?.message}>
          <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" {...register("rol")} />
        </Field>
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Fecha inicio" error={errors.fechaInicio?.message}>
          <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" type="date" {...register("fechaInicio")} />
        </Field>
        <Field label="Fecha fin" error={errors.fechaFin?.message}>
          <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" type="date" {...register("fechaFin")} />
        </Field>
      </div>
      <Field label="Descripcion" error={errors.descripcion?.message}>
        <textarea className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" {...register("descripcion")} />
      </Field>
      <div className="flex items-center gap-3">
        <button
          className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? "Guardando..." : submitLabel}
        </button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/actuaciones-veteranos">
          Cancelar
        </Link>
      </div>
    </form>
  );
}

function Field({ children, error, label }: { children: React.ReactNode; error?: string; label: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      {children}
      {error ? <p className="font-normal text-destructive">{error}</p> : null}
    </label>
  );
}

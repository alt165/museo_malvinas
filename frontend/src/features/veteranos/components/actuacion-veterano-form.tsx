"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect } from "react";
import { useForm, useWatch } from "react-hook-form";
import { LoadingState } from "@/components/common/loading-state";
import type { ActuacionVeteranoRequestDTO, ActuacionVeteranoResponseDTO } from "../types";
import { actuacionVeteranoSchema, type ActuacionVeteranoFormValues } from "../schemas";
import { useVeteranosQuery } from "../queries";
import { getValidationErrors } from "../utils";

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
  const {
    control,
    formState: { errors },
    handleSubmit,
    register,
    setError
  } = useForm<ActuacionVeteranoFormValues>({
    resolver: zodResolver(actuacionVeteranoSchema),
    defaultValues: {
      veteranoId: fixedVeteranoId ?? initialValue?.veteranoId ?? 0,
      rango: initialValue?.rango ?? "",
      unidad: initialValue?.unidad ?? "",
      rol: initialValue?.rol ?? "",
      fechaInicio: initialValue?.fechaInicio ?? "",
      fechaFin: initialValue?.fechaFin ?? "",
      descripcion: initialValue?.descripcion ?? ""
    }
  });
  const watchedVeteranoId = useWatch({ control, name: "veteranoId" });
  const veteranoId = fixedVeteranoId ?? watchedVeteranoId ?? 0;
  const veteranoSeleccionado = (veteranosQuery.data ?? []).find((veterano) => veterano.id === veteranoId);

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (
        field === "veteranoId" ||
        field === "rango" ||
        field === "unidad" ||
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
      className="max-w-4xl space-y-5 rounded-lg border p-5"
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
              {...register("veteranoId", { valueAsNumber: true })}
            >
              <option value={0}>Seleccionar veterano</option>
              {(veteranosQuery.data ?? []).map((veterano) => (
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
            value={veteranoSeleccionado?.fuerza ?? "Sin fuerza seleccionada"}
          />
        </Field>
      </div>
      <div className="grid gap-5 sm:grid-cols-3">
        <Field label="Rango" error={errors.rango?.message}>
          <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" {...register("rango")} />
        </Field>
        <Field label="Unidad" error={errors.unidad?.message}>
          <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" {...register("unidad")} />
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

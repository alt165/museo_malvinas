"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useObjetosQuery } from "@/features/objetos/queries";
import { useUbicacionesQuery } from "@/features/ubicaciones/queries";
import type { InventarioRequestDTO, InventarioResponseDTO } from "../types";
import { estadosConservacion, estadosInventario } from "../types";
import { inventarioSchema, type InventarioFormValues } from "../schemas";
import { getValidationErrors } from "../utils";

type InventarioFormProps = {
  initialValue?: InventarioResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: InventarioRequestDTO) => void;
};

export function InventarioForm({
  initialValue,
  isSubmitting = false,
  onSubmit,
  submitError,
  submitLabel
}: InventarioFormProps) {
  const objetosQuery = useObjetosQuery();
  const ubicacionesQuery = useUbicacionesQuery();
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError
  } = useForm<InventarioFormValues>({
    resolver: zodResolver(inventarioSchema),
    defaultValues: {
      objetoMuseoId: initialValue?.objetoMuseoId ?? 0,
      ubicacionId: initialValue?.ubicacionId ?? 0,
      estado: initialValue?.estado ?? "DISPONIBLE",
      estadoConservacion: initialValue?.estadoConservacion ?? "BUENO",
      fechaIngreso: initialValue?.fechaIngreso ?? new Date().toISOString().slice(0, 10),
      fechaSalida: initialValue?.fechaSalida ?? "",
      observaciones: initialValue?.observaciones ?? ""
    }
  });

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (
        field === "objetoMuseoId" ||
        field === "ubicacionId" ||
        field === "estado" ||
        field === "estadoConservacion" ||
        field === "fechaIngreso" ||
        field === "fechaSalida" ||
        field === "observaciones"
      ) {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  return (
    <form
      className="max-w-4xl space-y-5 rounded-lg border p-5"
      onSubmit={handleSubmit((values) =>
        onSubmit({
          objetoMuseoId: Number(values.objetoMuseoId),
          ubicacionId: Number(values.ubicacionId),
          estado: values.estado,
          estadoConservacion: values.estadoConservacion,
          fechaIngreso: values.fechaIngreso,
          fechaSalida: values.fechaSalida || null,
          observaciones: values.observaciones?.trim() || null
        })
      )}
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="objetoMuseoId">
            Objeto
          </label>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            disabled={objetosQuery.isLoading}
            id="objetoMuseoId"
            {...register("objetoMuseoId", { valueAsNumber: true })}
          >
            <option value={0}>Seleccionar objeto</option>
            {(objetosQuery.data ?? []).map((objeto) => (
              <option key={objeto.id} value={objeto.id}>
                {objeto.numeroInventario} - {objeto.nombre}
              </option>
            ))}
          </select>
          {errors.objetoMuseoId ? <p className="text-sm text-destructive">{errors.objetoMuseoId.message}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="ubicacionId">
            Ubicacion
          </label>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            disabled={ubicacionesQuery.isLoading}
            id="ubicacionId"
            {...register("ubicacionId", { valueAsNumber: true })}
          >
            <option value={0}>Seleccionar ubicacion</option>
            {(ubicacionesQuery.data ?? []).map((ubicacion) => (
              <option key={ubicacion.id} value={ubicacion.id}>
                {ubicacion.nombre}
              </option>
            ))}
          </select>
          {errors.ubicacionId ? <p className="text-sm text-destructive">{errors.ubicacionId.message}</p> : null}
        </div>
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="estado">
            Estado
          </label>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="estado"
            {...register("estado")}
          >
            {estadosInventario.map((estado) => (
              <option key={estado} value={estado}>
                {estado}
              </option>
            ))}
          </select>
          {errors.estado ? <p className="text-sm text-destructive">{errors.estado.message}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="estadoConservacion">
            Conservacion
          </label>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="estadoConservacion"
            {...register("estadoConservacion")}
          >
            {estadosConservacion.map((estado) => (
              <option key={estado} value={estado}>
                {estado}
              </option>
            ))}
          </select>
          {errors.estadoConservacion ? (
            <p className="text-sm text-destructive">{errors.estadoConservacion.message}</p>
          ) : null}
        </div>
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="fechaIngreso">
            Fecha de ingreso
          </label>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="fechaIngreso"
            type="date"
            {...register("fechaIngreso")}
          />
          {errors.fechaIngreso ? <p className="text-sm text-destructive">{errors.fechaIngreso.message}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="fechaSalida">
            Fecha de salida
          </label>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="fechaSalida"
            type="date"
            {...register("fechaSalida")}
          />
          {errors.fechaSalida ? <p className="text-sm text-destructive">{errors.fechaSalida.message}</p> : null}
        </div>
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="observaciones">
          Observaciones
        </label>
        <textarea
          className="min-h-28 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          id="observaciones"
          {...register("observaciones")}
        />
        {errors.observaciones ? <p className="text-sm text-destructive">{errors.observaciones.message}</p> : null}
      </div>
      <div className="flex items-center gap-3">
        <button
          className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? "Guardando..." : submitLabel}
        </button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/inventario">
          Cancelar
        </Link>
      </div>
    </form>
  );
}

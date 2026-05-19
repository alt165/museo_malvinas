"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import type { ExhibicionRequestDTO, ExhibicionResponseDTO } from "../types";
import { estadosExhibicion, tiposExhibicion } from "../types";
import { exhibicionSchema, type ExhibicionFormValues } from "../schemas";
import { getValidationErrors } from "../utils";

type ExhibicionFormProps = {
  initialValue?: ExhibicionResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: ExhibicionRequestDTO) => void;
};

export function ExhibicionForm({ initialValue, isSubmitting = false, onSubmit, submitError, submitLabel }: ExhibicionFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError
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

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (field === "nombre" || field === "descripcion" || field === "tipo" || field === "fechaInicio" || field === "fechaFin" || field === "estado") {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  return (
    <form
      className="w-full space-y-5 rounded-lg border p-5"
      onSubmit={handleSubmit((values) =>
        onSubmit({
          nombre: values.nombre.trim(),
          descripcion: values.descripcion?.trim() || null,
          tipo: values.tipo,
          fechaInicio: values.fechaInicio,
          fechaFin: values.fechaFin || null,
          estado: values.estado
        })
      )}
    >
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="nombre">
          Nombre
        </label>
        <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="nombre" {...register("nombre")} />
        {errors.nombre ? <p className="text-sm text-destructive">{errors.nombre.message}</p> : null}
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="descripcion">
          Descripcion
        </label>
        <textarea className="min-h-28 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" id="descripcion" {...register("descripcion")} />
        {errors.descripcion ? <p className="text-sm text-destructive">{errors.descripcion.message}</p> : null}
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="tipo">
            Tipo
          </label>
          <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="tipo" {...register("tipo")}>
            {tiposExhibicion.map((tipo) => (
              <option key={tipo} value={tipo}>
                {tipo}
              </option>
            ))}
          </select>
          {errors.tipo ? <p className="text-sm text-destructive">{errors.tipo.message}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="estado">
            Estado
          </label>
          <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="estado" {...register("estado")}>
            {estadosExhibicion.map((estado) => (
              <option key={estado} value={estado}>
                {estado}
              </option>
            ))}
          </select>
          {errors.estado ? <p className="text-sm text-destructive">{errors.estado.message}</p> : null}
        </div>
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="fechaInicio">
            Fecha de inicio
          </label>
          <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="fechaInicio" type="date" {...register("fechaInicio")} />
          {errors.fechaInicio ? <p className="text-sm text-destructive">{errors.fechaInicio.message}</p> : null}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="fechaFin">
            Fecha de fin
          </label>
          <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="fechaFin" type="date" {...register("fechaFin")} />
          {errors.fechaFin ? <p className="text-sm text-destructive">{errors.fechaFin.message}</p> : null}
        </div>
      </div>
      <div className="flex items-center gap-3">
        <button className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60" disabled={isSubmitting} type="submit">
          {isSubmitting ? "Guardando..." : submitLabel}
        </button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/exhibiciones">
          Cancelar
        </Link>
      </div>
    </form>
  );
}

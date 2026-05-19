"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { ubicacionSchema, type UbicacionFormValues } from "../schemas";
import type { UbicacionRequestDTO, UbicacionResponseDTO } from "../types";

type UbicacionFormProps = {
  initialValue?: UbicacionResponseDTO;
  isSubmitting?: boolean;
  submitLabel: string;
  onSubmit: (payload: UbicacionRequestDTO) => void;
};

export function UbicacionForm({ initialValue, isSubmitting = false, onSubmit, submitLabel }: UbicacionFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<UbicacionFormValues>({
    resolver: zodResolver(ubicacionSchema),
    defaultValues: {
      nombre: initialValue?.nombre ?? "",
      tipo: initialValue?.tipo ?? "",
      descripcion: initialValue?.descripcion ?? ""
    }
  });

  return (
    <form
      className="w-full space-y-5 rounded-lg border p-5"
      onSubmit={handleSubmit((values) =>
        onSubmit({
          nombre: values.nombre.trim(),
          tipo: values.tipo?.trim() || null,
          descripcion: values.descripcion?.trim() || null
        })
      )}
    >
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="nombre">Nombre</label>
        <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="nombre" {...register("nombre")} />
        {errors.nombre ? <p className="text-sm text-destructive">{errors.nombre.message}</p> : null}
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="tipo">Tipo</label>
        <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="tipo" {...register("tipo")} />
        {errors.tipo ? <p className="text-sm text-destructive">{errors.tipo.message}</p> : null}
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="descripcion">Descripcion</label>
        <textarea className="min-h-28 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" id="descripcion" {...register("descripcion")} />
      </div>
      <div className="flex items-center gap-3">
        <button className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60" disabled={isSubmitting} type="submit">
          {isSubmitting ? "Guardando..." : submitLabel}
        </button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/ubicaciones">
          Cancelar
        </Link>
      </div>
    </form>
  );
}

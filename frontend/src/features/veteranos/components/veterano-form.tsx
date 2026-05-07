"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import type { VeteranoRequestDTO, VeteranoResponseDTO } from "../types";
import { fuerzas } from "../types";
import { veteranoSchema, type VeteranoFormValues } from "../schemas";
import { getValidationErrors } from "../utils";

type VeteranoFormProps = {
  initialValue?: VeteranoResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: VeteranoRequestDTO) => void;
};

export function VeteranoForm({ initialValue, isSubmitting, onSubmit, submitError, submitLabel }: VeteranoFormProps) {
  const { formState: { errors }, handleSubmit, register, setError } = useForm<VeteranoFormValues>({
    resolver: zodResolver(veteranoSchema),
    defaultValues: {
      nombre: initialValue?.nombre ?? "",
      apellido: initialValue?.apellido ?? "",
      fuerza: initialValue?.fuerza ?? "EJERCITO",
      fechaNacimiento: initialValue?.fechaNacimiento ?? "",
      fechaFallecimiento: initialValue?.fechaFallecimiento ?? "",
      historia: initialValue?.historia ?? ""
    }
  });

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (
        field === "nombre" ||
        field === "apellido" ||
        field === "fuerza" ||
        field === "fechaNacimiento" ||
        field === "fechaFallecimiento" ||
        field === "historia"
      ) {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  return (
    <form className="max-w-4xl space-y-5 rounded-lg border p-5" onSubmit={handleSubmit((values) => onSubmit({
      nombre: values.nombre.trim(),
      apellido: values.apellido.trim(),
      fuerza: values.fuerza,
      fechaNacimiento: values.fechaNacimiento || null,
      fechaFallecimiento: values.fechaFallecimiento || null,
      historia: values.historia?.trim() || null
    }))}>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Nombre" error={errors.nombre?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...register("nombre")} /></Field>
        <Field label="Apellido" error={errors.apellido?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...register("apellido")} /></Field>
      </div>
      <div className="grid gap-5 sm:grid-cols-3">
        <Field label="Fuerza" error={errors.fuerza?.message}>
          <select className="h-10 w-full rounded-md border bg-background px-3 text-sm" {...register("fuerza")}>
            {fuerzas.map((fuerza) => <option key={fuerza} value={fuerza}>{fuerza}</option>)}
          </select>
        </Field>
        <Field label="Fecha de nacimiento" error={errors.fechaNacimiento?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" type="date" {...register("fechaNacimiento")} /></Field>
        <Field label="Fecha de fallecimiento" error={errors.fechaFallecimiento?.message}><input className="h-10 w-full rounded-md border bg-background px-3 text-sm" type="date" {...register("fechaFallecimiento")} /></Field>
      </div>
      <Field label="Descripcion" error={errors.historia?.message}><textarea className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm" {...register("historia")} /></Field>
      <div className="flex gap-3">
        <button className="h-10 rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground disabled:opacity-60" disabled={isSubmitting} type="submit">{isSubmitting ? "Guardando..." : submitLabel}</button>
        <Link className="inline-flex h-10 items-center rounded-md border px-4 text-sm hover:bg-muted" href="/veteranos">Cancelar</Link>
      </div>
    </form>
  );
}

function Field({ children, error, label }: { children: React.ReactNode; error?: string; label: string }) {
  return <label className="space-y-2 text-sm font-medium"><span>{label}</span>{children}{error ? <p className="font-normal text-destructive">{error}</p> : null}</label>;
}

"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect } from "react";
import type { ReactNode } from "react";
import { useForm } from "react-hook-form";
import { coleccionObjetoSchema, type ColeccionObjetoFormValues } from "../schemas";
import type { ColeccionObjetoRequestDTO, ColeccionObjetoResponseDTO } from "../types";
import { getValidationErrors } from "../utils";

type ColeccionFormProps = {
  initialValue?: ColeccionObjetoResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: ColeccionObjetoRequestDTO) => void;
};

export function ColeccionForm({ initialValue, isSubmitting = false, onSubmit, submitError, submitLabel }: ColeccionFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError
  } = useForm<ColeccionObjetoFormValues>({
    resolver: zodResolver(coleccionObjetoSchema),
    defaultValues: {
      nombre: initialValue?.nombre ?? "",
      descripcion: initialValue?.descripcion ?? ""
    }
  });

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (field === "nombre" || field === "descripcion") {
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
          descripcion: values.descripcion?.trim() || null
        })
      )}
    >
      <Field error={errors.nombre?.message} label="Nombre">
        <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" {...register("nombre")} />
      </Field>
      <Field error={errors.descripcion?.message} label="Descripcion">
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
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/objetos/colecciones">
          Cancelar
        </Link>
      </div>
    </form>
  );
}

function Field({ children, error, label }: { children: ReactNode; error?: string; label: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      {children}
      {error ? <p className="font-normal text-destructive">{error}</p> : null}
    </label>
  );
}

"use client";
import { FormLabel } from "@/components/common/form-label";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import type { CategoriaObjetoRequestDTO, CategoriaObjetoResponseDTO } from "../types";
import { categoriaSchema, type CategoriaFormValues } from "../schemas";
import { getValidationErrors } from "../utils";

type CategoriaFormProps = {
  initialValue?: CategoriaObjetoResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: CategoriaObjetoRequestDTO) => void;
};

export function CategoriaForm({
  initialValue,
  isSubmitting = false,
  onSubmit,
  submitError,
  submitLabel
}: CategoriaFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError
  } = useForm<CategoriaFormValues>({
    resolver: zodResolver(categoriaSchema),
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
      <Field required label="Nombre" error={errors.nombre?.message}>
        <input
          className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          {...register("nombre")}
        />
      </Field>
      <Field label="Descripcion" error={errors.descripcion?.message}>
        <textarea
          className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          {...register("descripcion")}
        />
      </Field>
      <div className="flex items-center gap-3">
        <button
          className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? "Guardando..." : submitLabel}
        </button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/categorias">
          Cancelar
        </Link>
      </div>
    </form>
  );
}

function Field({ children, error, label, required = false }: { children: React.ReactNode; error?: string; label: string; required?: boolean }) {
  return <FormLabel label={label} required={required}>{children}{error ? <p className="font-normal text-destructive">{error}</p> : null}</FormLabel>;
}

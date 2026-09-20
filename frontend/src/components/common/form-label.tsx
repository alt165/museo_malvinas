import type { ReactNode } from "react";

type FormLabelProps = {
  children?: ReactNode;
  className?: string;
  htmlFor?: string;
  label: ReactNode;
  required?: boolean;
};

export function FormLabel({ children, className = "space-y-2 text-sm font-medium", htmlFor, label, required = false }: FormLabelProps) {
  return (
    <label className={className} htmlFor={htmlFor}>
      <span>
        {label}
        {required ? <span aria-hidden="true" className="ml-1 text-destructive">*</span> : null}
      </span>
      {children}
    </label>
  );
}

export function RequiredAsterisk() {
  return <span aria-hidden="true" className="ml-1 text-destructive">*</span>;
}

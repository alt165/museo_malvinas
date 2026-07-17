import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

type RowActionsProps = {
  children: ReactNode;
  className?: string;
};

type RowActionVariant = "default" | "destructive";

type RowActionLinkProps = {
  href: string;
  icon: LucideIcon;
  label: string;
  variant?: RowActionVariant;
};

type RowActionButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  icon: LucideIcon;
  label: string;
  variant?: RowActionVariant;
};

const baseActionClass = "inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60";

function variantClass(variant: RowActionVariant = "default") {
  return variant === "destructive" ? "text-destructive" : undefined;
}

export function RowActions({ children, className }: RowActionsProps) {
  return <div className={cn("flex justify-end gap-2", className)}>{children}</div>;
}

export function RowActionLink({ href, icon: Icon, label, variant = "default" }: RowActionLinkProps) {
  return (
    <Link className={cn(baseActionClass, variantClass(variant))} href={href}>
      <Icon className="h-3.5 w-3.5" />
      {label}
    </Link>
  );
}

export function RowActionButton({ className, icon: Icon, label, type = "button", variant = "default", ...props }: RowActionButtonProps) {
  return (
    <button className={cn(baseActionClass, variantClass(variant), className)} type={type} {...props}>
      <Icon className="h-3.5 w-3.5" />
      {label}
    </button>
  );
}

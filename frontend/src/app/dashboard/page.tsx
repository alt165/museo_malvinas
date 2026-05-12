import Image from "next/image";
import { AppShell } from "@/components/layout/app-shell";

export default function DashboardPage() {
  return (
    <AppShell>
      <section className="flex min-h-[calc(100vh-8rem)] items-center justify-center bg-white px-4 py-10 text-center">
        <div className="mx-auto flex max-w-3xl flex-col items-center">
          <Image
            alt="Sol institucional"
            className="h-auto w-full max-w-sm object-contain"
            height={420}
            priority
            src="/images/sol-02.png"
            width={420}
          />
          <h1 className="mt-8 max-w-2xl text-3xl font-semibold leading-tight text-primary md:text-4xl">
            Archivo Histórico del Museo Malvinas, Antártida y Atlántico Sur
          </h1>
        </div>
      </section>
    </AppShell>
  );
}

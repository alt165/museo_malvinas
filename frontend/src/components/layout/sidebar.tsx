import Link from "next/link";

const navItems = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/objetos", label: "Objetos" },
  { href: "/inventario", label: "Inventario" },
  { href: "/exhibiciones", label: "Exhibiciones" }
];

export function Sidebar() {
  return (
    <aside className="fixed inset-y-0 left-0 hidden w-64 border-r bg-card md:block">
      <div className="border-b px-5 py-4">
        <p className="text-sm font-semibold">Museo Malvinas</p>
      </div>
      <nav className="space-y-1 p-3">
        {navItems.map((item) => (
          <Link
            className="block rounded-md px-3 py-2 text-sm text-muted-foreground hover:bg-muted hover:text-foreground"
            href={item.href}
            key={item.href}
          >
            {item.label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}

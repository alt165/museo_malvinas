import type { Metadata } from "next";
import "reactflow/dist/style.css";
import "./globals.css";
import { AppProviders } from "./providers";

export const metadata: Metadata = {
  title: "Museo Malvinas",
  description: "Administracion del inventario del museo",
  icons: {
    icon: "/favicon.png"
  }
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es">
      <body>
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}

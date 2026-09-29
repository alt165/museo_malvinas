export const measurementUnitGroups = [
  {
    label: "Longitud",
    units: [
      { value: "mm", label: "Milímetros (mm)" },
      { value: "cm", label: "Centímetros (cm)" },
      { value: "m", label: "Metros (m)" }
    ]
  },
  {
    label: "Masa",
    units: [
      { value: "g", label: "Gramos (g)" },
      { value: "kg", label: "Kilogramos (kg)" }
    ]
  }
] as const;

export type MeasurementUnit = typeof measurementUnitGroups[number]["units"][number]["value"];

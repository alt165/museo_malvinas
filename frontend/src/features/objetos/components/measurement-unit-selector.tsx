import { measurementUnitGroups, type MeasurementUnit } from "../measurement-units";

type MeasurementUnitSelectorProps = {
  disabled?: boolean;
  onSelect: (unit: MeasurementUnit) => void;
};

export function MeasurementUnitSelector({ disabled, onSelect }: MeasurementUnitSelectorProps) {
  return (
    <select
      aria-label="Insertar unidad de medida"
      className="h-9 rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
      defaultValue=""
      disabled={disabled}
      onChange={(event) => {
        if (event.target.value) {
          onSelect(event.target.value as MeasurementUnit);
          event.target.value = "";
        }
      }}
    >
      <option value="">Insertar unidad…</option>
      {measurementUnitGroups.map((group) => (
        <optgroup key={group.label} label={group.label}>
          {group.units.map((unit) => <option key={unit.value} value={unit.value}>{unit.label}</option>)}
        </optgroup>
      ))}
    </select>
  );
}

// Excel would run a cell starting with one of these as a formula, so such values get a leading apostrophe.
const FORMULA_TRIGGER = /^[=+\-@\t\r]/;

function csvCell(value: string | number | null | undefined) {
  const text = value == null ? "" : String(value);
  const safe = FORMULA_TRIGGER.test(text) ? `'${text}` : text;
  return /[";\n\r]/.test(safe) ? `"${safe.replace(/"/g, '""')}"` : safe;
}

// Semicolons and a byte-order mark are what Excel in a Polish locale expects to open the file correctly.
export function toCsv(header: string[], rows: (string | number | null | undefined)[][]) {
  return `﻿${[header, ...rows].map((row) => row.map(csvCell).join(";")).join("\r\n")}`;
}

export function downloadCsv(filename: string, content: string) {
  const url = URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

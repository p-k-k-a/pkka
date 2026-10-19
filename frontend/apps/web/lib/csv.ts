import Papa from "papaparse";

// Semicolons and a byte-order mark are what Excel in a Polish locale expects to open the file correctly.
export function toCsv(fields: string[], data: (string | number | null | undefined)[][]) {
  return `﻿${Papa.unparse({ fields, data }, { delimiter: ";", escapeFormulae: true })}`;
}

export function downloadCsv(filename: string, content: string) {
  const url = URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

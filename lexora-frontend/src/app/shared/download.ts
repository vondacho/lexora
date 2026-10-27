/** Opens a PDF returned by the API in a new tab; falls back to a download when pop-ups are blocked. */
export function openPdf(blob: Blob, fileName: string): void {
  // typed as PDF whatever the response said, or the browser shows the bytes instead of rendering them
  const url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
  // no 'noopener': with it window.open returns null even when the tab opens
  const opened = window.open(url, '_blank');
  if (!opened) {
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
  }
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** Options of the generated print operations: ask for the PDF itself (the generator would prefer a JSON type). */
export const ACCEPT_PDF = { httpHeaderAccept: 'application/pdf' as const };

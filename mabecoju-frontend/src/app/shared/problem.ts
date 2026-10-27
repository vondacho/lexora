import { HttpErrorResponse } from '@angular/common/http';
import { Problem } from '../api/generated';
import { translateProblem } from './errors';

/**
 * French message of a failed API call: the translation of the problem's code. The English detail of the API is
 * never shown (it is for logs); without a known code, the fallback given by the screen.
 */
export function problemMessage(error: unknown, fallback = 'L’opération n’a pas abouti.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Le serveur ne répond pas. Réessayez dans un instant.';
    }
    const body = error.error as Problem | null;
    const translated = body && typeof body === 'object' ? translateProblem(body) : null;
    if (translated) {
      return translated;
    }
    if (error.status === 403) {
      return 'Vos droits ne permettent pas cette opération.';
    }
  }
  return fallback;
}

/** Same as problemMessage, for calls answered as a Blob (PDFs): their error body arrives as a Blob too. */
export async function blobProblemMessage(error: unknown, fallback = 'L’opération n’a pas abouti.'): Promise<string> {
  if (error instanceof HttpErrorResponse && error.error instanceof Blob) {
    try {
      const body = JSON.parse(await error.error.text()) as unknown;
      return problemMessage(new HttpErrorResponse({ error: body, status: error.status }), fallback);
    } catch {
      // not JSON: fall through
    }
  }
  return problemMessage(error, fallback);
}

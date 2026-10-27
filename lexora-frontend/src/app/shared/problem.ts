import { HttpErrorResponse } from '@angular/common/http';
import { Problem } from '../api/generated';
import { texts } from '../i18n/i18n';
import { translateProblem } from './errors';

/**
 * Message of a failed API call in the current language: the translation of the problem's code. The English detail
 * of the API is never shown (it is for logs); without a known code, the fallback given by the screen.
 */
export function problemMessage(error: unknown, fallback = texts().common.failed): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return texts().common.serverDown;
    }
    const body = error.error as Problem | null;
    const translated = body && typeof body === 'object' ? translateProblem(body) : null;
    if (translated) {
      return translated;
    }
    if (error.status === 403) {
      return texts().common.forbidden;
    }
  }
  return fallback;
}

/** Same as problemMessage, for calls answered as a Blob (PDFs): their error body arrives as a Blob too. */
export async function blobProblemMessage(error: unknown, fallback = texts().common.failed): Promise<string> {
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

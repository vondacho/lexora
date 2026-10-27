import { HttpErrorResponse } from '@angular/common/http';
import { Problem } from '../api/generated';
import { setLang } from '../i18n/i18n';
import { ERROR_MESSAGES_EN, ERROR_MESSAGES_FR, translateProblem } from './errors';
import { blobProblemMessage, problemMessage } from './problem';

describe('problem messages', () => {
  it('translates the code with its values, never shows the English detail', () => {
    const e = new HttpErrorResponse({
      status: 422,
      error: { code: 'VAT_PERIOD_OVERLAP', detail: 'The VAT from … periods cannot overlap', params: { from: '2026-01-01', to: '2026-06-30', paidOn: '2026-08-28' } },
    });
    expect(problemMessage(e)).toBe('La TVA du 01.01.2026 au 30.06.2026 est déjà payée (le 28.08.2026) : les périodes ne peuvent pas se chevaucher.');
  });

  it('names fields and things in French', () => {
    expect(translateProblem({ code: 'FIELD_MANDATORY', params: { field: 'Folder name' } })).toBe('Indiquez le nom du dossier.');
    expect(translateProblem({ code: 'NOT_FOUND', params: { entity: 'Invoice', id: '12' } })).toBe('La facture n’existe pas ou plus.');
    expect(translateProblem({ code: 'NEGATIVE_VALUE', params: { field: 'delay' } })).toBe('Indiquez une valeur positive pour le délai.');
    expect(translateProblem({ code: 'FIELD_TOO_LONG', params: { field: 'Folder handler', max: 10 } })).toBe(
      'Texte trop long pour la personne en charge (10 caractères au plus).',
    );
    expect(translateProblem({ code: 'NEGATIVE_VALUE', params: { field: 'something new' } })).toBe('Indiquez une valeur positive pour ce champ.');
  });

  it('falls back to the screen’s message without a known code', () => {
    expect(problemMessage(new HttpErrorResponse({ status: 500, error: { detail: 'NullPointerException' } }), 'repli')).toBe('repli');
    expect(problemMessage(new HttpErrorResponse({ status: 403, error: null }))).toBe('Vos droits ne permettent pas cette opération.');
    expect(problemMessage(new HttpErrorResponse({ status: 0 }))).toBe('Le serveur ne répond pas. Réessayez dans un instant.');
  });

  it('reads the problem of a PDF call, which arrives as a Blob', async () => {
    const body = new Blob([JSON.stringify({ code: 'MODEL_TYPE_MISMATCH', params: { model: 'Rappel', modelType: 'RECALL', documentType: 'INVOICE' } })]);
    expect(await blobProblemMessage(new HttpErrorResponse({ status: 422, error: body }))).toBe('Le modèle « Rappel » est prévu pour les rappels.');
    expect(await blobProblemMessage(new HttpErrorResponse({ status: 500, error: new Blob(['oops']) }), 'fallback')).toBe('fallback');
  });

  it('has a text in both languages for every code of the contract', () => {
    for (const code of Object.values(Problem.CodeEnum)) {
      for (const messages of [ERROR_MESSAGES_FR, ERROR_MESSAGES_EN]) {
        const text = messages[code]({});
        expect(text.length, code).toBeGreaterThan(10);
        expect(text, code).not.toMatch(/undefined/);
      }
    }
  });

  it('follows the language of the interface', () => {
    setLang('en');
    try {
      expect(translateProblem({ code: 'FIELD_MANDATORY', params: { field: 'Folder name' } })).toBe('Enter the name of the folder.');
      expect(problemMessage(new HttpErrorResponse({ status: 0 }))).toBe('The server does not answer. Try again in a moment.');
    } finally {
      setLang('fr');
    }
  });
});

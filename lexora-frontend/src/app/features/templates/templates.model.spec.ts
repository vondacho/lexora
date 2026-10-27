import { isHtml, layoutsFor, mentionsAmount, pagesOf, textToHtml, unknownFields } from './templates.model';

describe('templates', () => {
  it('finds the placeholders the API would leave empty', () => {
    expect(unknownFields('$title_1, ${amount_2} le $date pour $client et $montant.')).toEqual(['client', 'montant']);
    expect(unknownFields('$title_3 $tvaNo')).toEqual([]);
  });

  it('checks that a letter mentions an amount', () => {
    expect(mentionsAmount('Votre part : frs. $amount_1.')).toBe(true);
    expect(mentionsAmount('Merci de régler le solde.')).toBe(false);
  });

  it('turns a plain-text template into paragraphs', () => {
    expect(textToHtml('$title_1,\n\nLigne 1\nLigne 2 <ok> & co\n\n\nFin')).toBe(
      '<p>$title_1,</p><p>Ligne 1<br>Ligne 2 &lt;ok&gt; &amp; co</p><p>Fin</p>',
    );
    expect(isHtml(textToHtml('a'))).toBe(true);
    expect(isHtml('Montant < 100')).toBe(false);
  });

  it('reads and writes the page layouts, keeping the legacy names', () => {
    const legacy = [{ layout: 'layout_invoice_double.php' }, { layout: 'layout_invoice_double_detail.php' }];
    expect(pagesOf(legacy)).toEqual(['letter', 'detail']);
    expect(pagesOf([])).toEqual(['letter']);
    expect(layoutsFor(['letter', 'detail'], legacy, 'INVOICE')).toEqual([
      { layout: 'layout_invoice_double.php', singlePage: false },
      { layout: 'layout_invoice_double_detail.php', singlePage: false },
    ]);
    expect(layoutsFor(['letter'], [], 'RECALL')).toEqual([{ layout: 'layout_recall.php', singlePage: false }]);
    expect(layoutsFor(['letter', 'detail'], [], 'INVOICE')[1].layout).toBe('layout_invoice_detail.php');
  });
});

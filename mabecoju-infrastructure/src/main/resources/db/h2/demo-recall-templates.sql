-- Second reminder and formal notice: the reminders screen proposes the n-th reminder template for the n-th reminder.
INSERT INTO invo_models (mode_name, mode_title, mode_content, mode_adr_count, mode_type, mode_layout_1)
  VALUES ('Rappel 2 - ferme', '2e rappel', '$title_1,

Malgré notre rappel du $lastRecallDate, notre envoi n° $refNo du $invoiceDate reste impayé. Le solde s''élève à frs. $amount_1.

Nous vous prions de le régler sans délai, au moyen du bulletin ci-joint.

Avec nos salutations distinguées.', 1, 3, 'layout_recall.php');
INSERT INTO invo_models (mode_name, mode_title, mode_content, mode_adr_count, mode_type, mode_layout_1)
  VALUES ('Mise en demeure', 'Mise en demeure', '$title_1,

Nos rappels étant restés sans suite, nous vous mettons en demeure de payer le solde de frs. $amount_1 dû au titre de notre envoi n° $refNo du $invoiceDate, dans un délai de dix jours.

Passé ce délai, nous nous réserverons d''engager une procédure de recouvrement.

Avec nos salutations distinguées.', 1, 3, 'layout_recall.php');

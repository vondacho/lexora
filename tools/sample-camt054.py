#!/usr/bin/env python3
"""Writes a sample camt.054 bank notification paying the open invoices and advance requests of a mabecoju API,
to try the import of "Encaissements" on demo data. Nothing is recorded: the file is only written.

    python3 tools/sample-camt054.py [--url http://localhost:8080] [--user demo] [--password demo] [--out camt054-demo.xml]

The credits: most shares paid in full by QR reference, one paid in part, one paid 20.00 too much, one without
reference (to be suggested by amount and name), one with an unknown reference and one in euros.
"""
import argparse
import base64
import datetime
import json
import urllib.request
from xml.sax.saxutils import escape

MOD10 = [0, 9, 4, 6, 8, 2, 7, 1, 3, 5]


def qr_reference(kind, document_id, customer_id):
    """Same rule as the application: payload kind (1 digit), document (9), customer (9), padded to 26, mod 10."""
    digits = f"{kind}{document_id:09d}{customer_id:09d}".rjust(26, "0")
    carry = 0
    for c in digits:
        carry = MOD10[(carry + int(c)) % 10]
    return digits + str((10 - carry) % 10)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--url", default="http://localhost:8080")
    parser.add_argument("--user", default="demo")
    parser.add_argument("--password", default="demo")
    parser.add_argument("--out", default="camt054-demo.xml")
    args = parser.parse_args()
    auth = "Basic " + base64.b64encode(f"{args.user}:{args.password}".encode()).decode()

    def get(path):
        request = urllib.request.Request(args.url + path, headers={"Authorization": auth, "X-Requested-With": "XMLHttpRequest"})
        with urllib.request.urlopen(request) as response:
            return json.loads(response.read())

    overview = get("/api/debtors/overview")
    shares = []
    for kind, key, path in ((1, "openInvoices", "/api/invoices/"), (2, "openRequests", "/api/advance-requests/")):
        for debt in overview.get(key, []):
            document = get(path + str(debt["documentId"]))
            customers = {c["id"]: c for c in get(f"/api/folders/{document['folderId']}/customers")}
            for share in document.get("shares") or []:
                remaining = share.get("remaining") or 0
                if remaining > 0:
                    customer = customers.get(share["customerId"], {})
                    name = f"{customer.get('lastName', '')} {customer.get('firstName', '')}".strip().upper()
                    shares.append((kind, debt["documentId"], share["customerId"], round(remaining, 2), name))
    if not shares:
        raise SystemExit("Nothing open to pay: no file written.")

    today = datetime.date.today().isoformat()
    credits = []
    for i, (kind, document, customer, remaining, name) in enumerate(shares):
        reference = qr_reference(kind, document, customer)
        if i == 1:
            credits.append((round(remaining / 2, 2), reference, name, None))         # paid in part
        elif i == 2:
            credits.append((round(remaining + 20, 2), reference, name, None))        # 20.00 too much
        elif i == 3:
            credits.append((remaining, None, name, "paiement facture"))              # no reference
        else:
            credits.append((remaining, reference, name, None))
    credits.append((50.0, "RF18539007547034", "INCONNU SA", None))                     # not ours

    transactions = []
    for n, (amount, reference, name, remittance) in enumerate(credits, start=1):
        rmt = (f"<RmtInf><Strd><CdtrRefInf><Tp><CdOrPrtry><Prtry>{'QRR' if reference and not reference.startswith('RF') else 'SCOR'}</Prtry></CdOrPrtry></Tp>"
               f"<Ref>{reference}</Ref></CdtrRefInf></Strd></RmtInf>" if reference
               else f"<RmtInf><Ustrd>{escape(remittance)}</Ustrd></RmtInf>")
        transactions.append(
            f"<TxDtls><Refs><AcctSvcrRef>DEMO-{today}-{n:03d}</AcctSvcrRef></Refs>"
            f"<Amt Ccy=\"CHF\">{amount:.2f}</Amt><CdtDbtInd>CRDT</CdtDbtInd>"
            f"<RltdPties><Dbtr><Pty><Nm>{escape(name)}</Nm></Pty></Dbtr>"
            f"<DbtrAcct><Id><IBAN>CH9300762011623852957</IBAN></Id></DbtrAcct></RltdPties>{rmt}</TxDtls>")
    total = sum(c[0] for c in credits)
    xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.054.001.08">
  <BkToCstmrDbtCdtNtfctn>
    <GrpHdr><MsgId>DEMO-{today}</MsgId><CreDtTm>{today}T18:00:00</CreDtTm></GrpHdr>
    <Ntfctn>
      <Id>DEMO-{today}</Id>
      <Acct><Id><IBAN>CH4431999123000889012</IBAN></Id></Acct>
      <Ntry>
        <Amt Ccy="CHF">{total:.2f}</Amt><CdtDbtInd>CRDT</CdtDbtInd><Sts><Cd>BOOK</Cd></Sts>
        <BookgDt><Dt>{today}</Dt></BookgDt><ValDt><Dt>{today}</Dt></ValDt>
        <NtryDtls>{''.join(transactions)}</NtryDtls>
      </Ntry>
      <Ntry>
        <Amt Ccy="EUR">80.00</Amt><CdtDbtInd>CRDT</CdtDbtInd><BookgDt><Dt>{today}</Dt></BookgDt><AcctSvcrRef>DEMO-{today}-EUR</AcctSvcrRef>
      </Ntry>
      <Ntry>
        <Amt Ccy="CHF">129.00</Amt><CdtDbtInd>DBIT</CdtDbtInd><BookgDt><Dt>{today}</Dt></BookgDt>
      </Ntry>
    </Ntfctn>
  </BkToCstmrDbtCdtNtfctn>
</Document>
"""
    with open(args.out, "w", encoding="utf-8") as f:
        f.write(xml)
    print(f"{args.out}: {len(credits) + 1} credits for {len(shares)} open shares")


if __name__ == "__main__":
    main()

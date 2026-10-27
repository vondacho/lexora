package io.lexora.infrastructure.bank;

import io.lexora.application.bank.BankCredit;
import io.lexora.application.bank.BankStatement;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Camt054ReaderTest {

    private final Camt054Reader reader = new Camt054Reader();

    static final String SAMPLE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.054.001.08">
              <BkToCstmrDbtCdtNtfctn>
                <GrpHdr><MsgId>N-1</MsgId><CreDtTm>2026-10-24T18:00:00</CreDtTm></GrpHdr>
                <Ntfctn>
                  <Id>N-1</Id>
                  <Acct><Id><IBAN>CH4431999123000889012</IBAN></Id></Acct>
                  <Ntry>
                    <Amt Ccy="CHF">850.00</Amt><CdtDbtInd>CRDT</CdtDbtInd><Sts><Cd>BOOK</Cd></Sts>
                    <BookgDt><Dt>2026-10-22</Dt></BookgDt><ValDt><Dt>2026-10-22</Dt></ValDt>
                    <AcctSvcrRef>E-1</AcctSvcrRef>
                    <NtryDtls>
                      <TxDtls>
                        <Refs><AcctSvcrRef>T-1</AcctSvcrRef></Refs>
                        <Amt Ccy="CHF">450.00</Amt><CdtDbtInd>CRDT</CdtDbtInd>
                        <RltdPties><Dbtr><Pty><Nm>KELLER THOMAS</Nm></Pty></Dbtr><DbtrAcct><Id><IBAN>CH9300762011623852957</IBAN></Id></DbtrAcct></RltdPties>
                        <RmtInf><Strd><CdtrRefInf><Tp><CdOrPrtry><Prtry>QRR</Prtry></CdOrPrtry></Tp><Ref>21 00000 00003 13947 14300 09017</Ref></CdtrRefInf></Strd></RmtInf>
                      </TxDtls>
                      <TxDtls>
                        <Refs><AcctSvcrRef>T-2</AcctSvcrRef></Refs>
                        <Amt Ccy="CHF">400.00</Amt><CdtDbtInd>CRDT</CdtDbtInd>
                        <RltdPties><Dbtr><Nm>Favre Nadia</Nm></Dbtr></RltdPties>
                        <RmtInf><Ustrd>provision septembre</Ustrd></RmtInf>
                      </TxDtls>
                    </NtryDtls>
                  </Ntry>
                  <Ntry>
                    <Amt Ccy="CHF">129.00</Amt><CdtDbtInd>DBIT</CdtDbtInd><BookgDt><Dt>2026-10-23</Dt></BookgDt>
                  </Ntry>
                  <Ntry>
                    <Amt Ccy="CHF">99.00</Amt><CdtDbtInd>CRDT</CdtDbtInd><RvslInd>true</RvslInd><BookgDt><Dt>2026-10-23</Dt></BookgDt>
                  </Ntry>
                  <Ntry>
                    <Amt Ccy="EUR">50.00</Amt><CdtDbtInd>CRDT</CdtDbtInd><BookgDt><DtTm>2026-10-24T09:12:00</DtTm></BookgDt>
                    <AcctSvcrRef>E-4</AcctSvcrRef>
                  </Ntry>
                </Ntfctn>
              </BkToCstmrDbtCdtNtfctn>
            </Document>
            """;

    @Test
    void reads_one_credit_per_transaction_and_leaves_debits_and_reversals_out() {
        BankStatement statement = reader.read(SAMPLE);

        assertThat(statement.accountIban()).isEqualTo("CH4431999123000889012");
        assertThat(statement.skippedDebits()).isEqualTo(1);
        assertThat(statement.from()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(statement.to()).isEqualTo(LocalDate.of(2026, 10, 24));
        assertThat(statement.credits()).hasSize(3);

        BankCredit keller = statement.credits().get(0);
        assertThat(keller).isEqualTo(new BankCredit("T-1", LocalDate.of(2026, 10, 22), Money.of("450.00"), "CHF",
                "210000000003139471430009017", "KELLER THOMAS", "CH9300762011623852957", null));
        BankCredit favre = statement.credits().get(1);
        assertThat(favre.reference()).isNull();
        assertThat(favre.debtorName()).isEqualTo("Favre Nadia");
        assertThat(favre.remittance()).isEqualTo("provision septembre");
        BankCredit euro = statement.credits().get(2);
        assertThat(euro.currency()).isEqualTo("EUR");
        assertThat(euro.entryReference()).isEqualTo("E-4");
        assertThat(euro.date()).isEqualTo(LocalDate.of(2026, 10, 24));
    }

    @Test
    void refuses_other_files_and_external_entities() {
        assertThatThrownBy(() -> reader.read("not xml")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> reader.read("<Document><Other/></Document>")).isInstanceOf(DomainException.class);
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE Document [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <Document><BkToCstmrDbtCdtNtfctn><Ntfctn><Acct><Id><IBAN>&secret;</IBAN></Id></Acct></Ntfctn></BkToCstmrDbtCdtNtfctn></Document>
                """;
        assertThatThrownBy(() -> reader.read(xxe)).isInstanceOf(DomainException.class);
    }
}

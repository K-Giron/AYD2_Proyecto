package com.megastore.sgologic.fiscal;

import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.UUID;

/** Genera XML bien formado y escapa todos los datos aportados por el cliente. */
@Component
class FiscalXmlGenerator {

    String generate(UUID documentId, UUID orderId, FiscalDocumentType type,
                    UUID originalDocumentId, String taxIdentifier, String customerName,
                    BigDecimal total, String currency, String cui, String series) {
        try {
            StringWriter output = new StringWriter();
            var writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("DTE");
            writer.writeAttribute("version", "1");
            element(writer, "DocumentId", documentId.toString());
            element(writer, "OrderId", orderId.toString());
            element(writer, "DocumentType", type.name());
            if (originalDocumentId != null) {
                element(writer, "OriginalDocumentId", originalDocumentId.toString());
            }
            element(writer, "TaxIdentifier", taxIdentifier);
            element(writer, "CustomerName", customerName);
            element(writer, "Currency", currency);
            element(writer, "Total", total.toPlainString());
            element(writer, "CUI", cui);
            element(writer, "Series", series);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
            return output.toString();
        } catch (XMLStreamException failure) {
            throw new IllegalStateException("No fue posible generar el XML fiscal.", failure);
        }
    }

    private void element(javax.xml.stream.XMLStreamWriter writer, String name, String value)
            throws XMLStreamException {
        writer.writeStartElement(name);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }
}

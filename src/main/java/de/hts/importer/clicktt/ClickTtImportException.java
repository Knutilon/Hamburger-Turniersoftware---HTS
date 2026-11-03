package de.hts.importer.clicktt;

/** Fehler beim Einlesen einer click-TT-XML-Datei, mit einer für Anwender verständlichen Meldung. */
public class ClickTtImportException extends Exception {

    private static final long serialVersionUID = 1L;

    public ClickTtImportException(String message) {
        super(message);
    }

    public ClickTtImportException(String message, Throwable cause) {
        super(message, cause);
    }
}

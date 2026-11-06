package de.hts.importer.csv;

import de.hts.player.Player;
import de.hts.player.Sex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV-Import der Teilnehmenden eines Vorgabeturniers. Aufbau: Trennzeichen Komma, Zeichensatz UTF-8, Zeile 1 =
 * Überschrift (wird übersprungen), Spalten A–D = Vorname, Nachname, TTR-Wert, Klasse („Erwachsene“ = männlich,
 * „Damen“ = weiblich); alles ab Spalte E wird ignoriert. Fehlerhafte Zeilen werden übersprungen und gemeldet.
 */
public final class HandicapCsvImporter {

    /** Inhalt der Vorlage, die HTS mitliefert. */
    public static final String TEMPLATE = "Vorname,Nachname,TTR-Wert,Klasse\n"
            + "Max,Mustermann,1520,Erwachsene\n"
            + "Erika,Musterfrau,1380,Damen\n";

    /** Ergebnis: gültige Personen und Hinweise zu ignorierten Zeilen („Zeile 7: TTR-Wert ungültig“). */
    public record Result(List<Player> players, List<String> ignored) {
    }

    public static final class CsvException extends Exception {
        private static final long serialVersionUID = 1L;

        public CsvException(String message) {
            super(message);
        }
    }

    public Result importFile(Path file) throws IOException, CsvException {
        String name = file.getFileName() == null ? "" : file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (!name.endsWith(".csv")) throw new CsvException("Es können nur CSV-Dateien (.csv) importiert werden.");
        return parse(Files.readAllBytes(file));
    }

    public Result parse(byte[] bytes) throws CsvException {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new CsvException("Die Datei ist nicht im Zeichensatz UTF-8 gespeichert. In Excel bitte „Speichern unter“ → "
                    + "„CSV UTF-8 (durch Trennzeichen getrennt)“ wählen.");
        }
        if (text.startsWith("﻿")) text = text.substring(1);       // BOM
        String[] lines = text.split("\r\n|\n|\r", -1);
        List<Player> players = new ArrayList<>();
        List<String> ignored = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            int row = i + 1;
            String line = lines[i];
            if (line.isBlank() || line.replace(",", "").isBlank()) continue;   // leere Zeile
            List<String> cols = split(line);
            String first = col(cols, 0), last = col(cols, 1), ttr = col(cols, 2), cls = col(cols, 3);
            if (first.isEmpty() || last.isEmpty()) {
                ignored.add("Zeile " + row + ": Vorname oder Nachname fehlt");
                continue;
            }
            Integer ttrValue = null;
            if (!ttr.isEmpty()) {
                try {
                    ttrValue = Integer.parseInt(ttr);
                    if (ttrValue < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    ignored.add("Zeile " + row + ": TTR-Wert ungültig („" + ttr + "“)");
                    continue;
                }
            }
            Sex sex;
            if (cls.equalsIgnoreCase("Erwachsene")) sex = Sex.MALE;
            else if (cls.equalsIgnoreCase("Damen")) sex = Sex.FEMALE;
            else {
                ignored.add("Zeile " + row + ": Klasse ungültig („" + cls + "“ – erlaubt: Erwachsene, Damen)");
                continue;
            }
            Player p = new Player(first, last);
            p.setTtr(ttrValue);
            p.setSex(sex);
            p.setInternalNr("csv-" + row);
            players.add(p);
        }
        return new Result(players, ignored);
    }

    private static String col(List<String> cols, int i) {
        return i < cols.size() ? cols.get(i).trim() : "";
    }

    /** Eine CSV-Zeile mit Komma trennen; Felder in Anführungszeichen dürfen Kommas enthalten („“ = ein “). */
    static List<String> split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else quoted = false;
                } else cur.append(ch);
            } else if (ch == '"') quoted = true;
            else if (ch == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else cur.append(ch);
        }
        out.add(cur.toString());
        return out;
    }
}

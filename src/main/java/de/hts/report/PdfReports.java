package de.hts.report;

import de.hts.tournament.Entrant;
import de.hts.tournament.GroupDraw;
import de.hts.tournament.GroupStanding;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import de.hts.tournament.TournamentMode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static de.hts.report.PdfCanvas.BOLD;
import static de.hts.report.PdfCanvas.REGULAR;

/**
 * PDF-Dokumente von HTS:
 * <ul>
 *   <li>{@link #slip}: Spielzettel A6 (Hochformat), vorausgefüllt mit Tisch, Zeit, Spiel-Nr., Konkurrenz und Namen.</li>
 *   <li>{@link #phase}: eine Phase einer Klasse – je Gruppe eine A4-Seite mit Zwischenstand und Spielplan.</li>
 *   <li>{@link #finalResult}: Endergebnis einer Klasse (alle Platzierungen).</li>
 * </ul>
 * Die Dokumente müssen vom Aufrufer geschlossen werden.
 */
public final class PdfReports {

    private static final DateTimeFormatter STAND = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    /** A6 hoch: 105 × 148 mm */
    public static final PDRectangle A6 = new PDRectangle(105 / 25.4f * 72, 148 / 25.4f * 72);
    private static final float ROW = 13f;

    private PdfReports() {
    }

    private static PDDocument newDoc(String title) {
        PDDocument doc = new PDDocument();
        PDDocumentInformation info = doc.getDocumentInformation();
        info.setTitle(title);
        info.setCreator("HTS – Hamburger Turniersoftware");
        return doc;
    }

    // --- Spielzettel ------------------------------------------------------------

    /** Text „Konkurrenz“ des Spielzettels, z. B. „Mädchen U15 · Gruppe A · Runde 2“. */
    public static String competition(Tournament t, Match m) {
        PlayingClass c = t.findClass(m.getClassName()).orElse(null);
        PlayingClass root = c == null ? null : t.rootOf(c);
        return (root == null ? m.getClassName() : root.getName())
                + (c != null && c.isPhase() ? " · Phase " + t.phaseNumber(c) : "") + " · " + m.getStage()
                + (koStyle(m) || m.getPlacement() > 0 || m.getStage().startsWith("Runde") ? "" : " · Runde " + m.getRound());
    }

    public static PDDocument slip(Tournament t, Match m) {
        if (t.isTeamMatch(m)) return teamSlip(t, m);
        PDDocument doc = newDoc("Spielzettel Spiel " + m.getNumber());
        PdfCanvas c = new PdfCanvas(doc, A6, 16);
        c.newPage();
        float x = c.left(), w = c.width();
        c.y += c.text(x, c.y, "TISCHTENNIS – SPIELZETTEL", BOLD, 11, w, PdfCanvas.Align.CENTER, 0) + 4;

        float top = c.y;
        String tn = m.getTable() == null ? "Tisch ____" : t.tableName(m.getTable());
        c.text(x, top, tn, BOLD, tn.length() > 12 ? (tn.length() > 20 ? 10 : 13) : 18, w * 0.6f);
        c.text(x, top, "Spiel-Nr. " + m.getNumber(), BOLD, 9, w, PdfCanvas.Align.RIGHT, 0);
        String time = m.getStartedAt() != null ? m.getStartedAt().format(HM) : LocalDateTime.now().format(HM);
        c.text(x, top + 12, "Zeit: " + time, REGULAR, 9, w, PdfCanvas.Align.RIGHT, 0);
        c.y = top + 26;
        c.paragraph("Konkurrenz: " + competition(t, m), REGULAR, 8.5f, w);
        c.y += 3;
        c.line(x, c.y, x + w, c.y, 0.8f);
        c.y += 5;

        side(c, t, m, m.getA(), "Spieler/in bzw. Doppel 1");
        side(c, t, m, m.getB(), "Spieler/in bzw. Doppel 2");

        // Satztabelle
        c.y += 2;
        float c0 = 54, c1 = (w - c0) / 2, hh = 15, rh = 17;
        c.fill(x, c.y, w, hh, 0.9f);
        c.rect(x, c.y, c0, hh, 0.6f);
        c.rect(x + c0, c.y, c1, hh, 0.6f);
        c.rect(x + c0 + c1, c.y, c1, hh, 0.6f);
        c.text(x + 3, c.y + 4, "Satz", BOLD, 7.5f, c0 - 6);
        c.text(x + c0 + 3, c.y + 4, "Bälle Spieler/in 1", BOLD, 7.5f, c1 - 6);
        c.text(x + c0 + c1 + 3, c.y + 4, "Bälle Spieler/in 2", BOLD, 7.5f, c1 - 6);
        c.y += hh;
        int max = 2 * t.winningSets(m) - 1;
        for (int i = 1; i <= max + 1; i++) {
            c.rect(x, c.y, c0, rh, 0.6f);
            c.rect(x + c0, c.y, c1, rh, 0.6f);
            c.rect(x + c0 + c1, c.y, c1, rh, 0.6f);
            c.text(x + 3, c.y + 5, i <= max ? i + ". Satz" : "Sätze", i <= max ? REGULAR : BOLD, 8, c0 - 6);
            c.y += rh;
        }
        c.y += 7;
        // Wahl
        float bx = x;
        c.text(bx, c.y, "Wahl:", REGULAR, 8, 30);
        bx += 30;
        for (String opt : new String[]{"Aufschlag", "Rückschlag", "Seite"}) {
            c.rect(bx, c.y + 0.5f, 7, 7, 0.6f);
            c.text(bx + 10, c.y, opt, REGULAR, 8, 60);
            bx += 12 + PdfCanvas.textWidth(opt, REGULAR, 8) + 10;
        }
        c.y += 12;
        // Vorgabeturnier: Aufschlag automatisch für die Partei ohne Punktevorsprung
        int hc = t.handicapOf(m);
        if (hc != 0 && t.getHandicapRules().getServe() == de.hts.tournament.HandicapRules.Serve.NO_ADVANTAGE) {
            String server = hc > 0 ? t.partyFullName(m, m.getB()) : t.partyFullName(m, m.getA());
            c.text(x, c.y, "Aufschlag: " + server + " (ohne Vorsprung)", REGULAR, 7.5f, w);
            c.y += 6;
        }
        c.y += 10;
        signature(c, "Gewinner/in:");
        signature(c, "Unterschrift Schiedsrichter/in:");
        c.finish();
        return doc;
    }

    /**
     * Spielzettel (DIN A4) einer Begegnung im 2er/3er Turnier: Schema aller Partien mit freien Namensfeldern für die
     * Aufstellung, je Partie Sätze und Sieger zum Eintragen; dazu die Teams mit TTR und die Vorgabe-Tabelle aus den
     * Vorgaberegeln.
     */
    public static PDDocument teamSlip(Tournament t, Match m) {
        PDDocument doc = newDoc("Spielzettel Spiel " + m.getNumber());
        PdfCanvas c = new PdfCanvas(doc, PDRectangle.A4, 36);
        c.newPage();
        float x = c.left(), w = c.width();
        PlayingClass cls = t.findClass(m.getClassName()).orElse(null);
        PlayingClass root = cls == null ? null : t.rootOf(cls);
        c.y += c.text(x, c.y, "TISCHTENNIS – SPIELZETTEL " + (root == null ? "" : root.getDiscipline().getLabel().toUpperCase(java.util.Locale.GERMAN)),
                BOLD, 13, w, PdfCanvas.Align.CENTER, 0) + 6;
        float top = c.y;
        String tn = m.getTable() == null ? "Tisch ____" : t.tableName(m.getTable());
        c.text(x, top, tn, BOLD, 18, w * 0.6f);
        c.text(x, top, "Spiel-Nr. " + m.getNumber(), BOLD, 10, w, PdfCanvas.Align.RIGHT, 0);
        String time = m.getStartedAt() != null ? m.getStartedAt().format(HM) : LocalDateTime.now().format(HM);
        c.text(x, top + 13, "Zeit: " + time, REGULAR, 9, w, PdfCanvas.Align.RIGHT, 0);
        c.y = top + 28;
        c.paragraph("Konkurrenz: " + competition(t, m), REGULAR, 9, w);
        c.y += 4;
        c.line(x, c.y, x + w, c.y, 0.8f);
        c.y += 8;

        // Teams mit Mitgliedern, TTR (und Klasse) und freien Feldern für die Aufstellung
        List<String> positions = cls == null ? List.of("1", "2") : t.teamPositions(cls);
        float half = (w - 16) / 2;
        float yTeams = c.y, yEnd = c.y;
        for (int side = 0; side < 2; side++) {
            String id = side == 0 ? m.getA() : m.getB();
            String letter = side == 0 ? "A" : "B";
            float bx = x + side * (half + 16), yy = yTeams;
            String team = id == null ? "______________________" : t.entrantName(m, id);
            yy += c.text(bx, yy, "Team " + letter + ": " + team, BOLD, 11, half);
            for (de.hts.player.Player p : t.teamMembers(id)) {
                String info = p.getDisplayName() + (p.getTtr() != null ? " · TTR " + p.getTtr() : "") + handicapClass(t, root, p);
                yy += c.text(bx + 6, yy, info, REGULAR, 8.5f, half - 6);
            }
            yy += 6;
            yy += c.text(bx, yy, "Aufstellung (Namen eintragen):", REGULAR, 7.5f, half, PdfCanvas.Align.LEFT, 0.35f);
            for (String pos : positions) {
                String lbl = letter + pos;
                c.text(bx, yy + 4, lbl, BOLD, 9, 30);
                c.line(bx + 32, yy + 14, bx + half, yy + 14, 0.6f);
                yy += 20;
            }
            yEnd = Math.max(yEnd, yy);
        }
        c.y = yEnd + 6;

        // Schema der Begegnung
        List<Tournament.Rubber> rubbers = t.rubbersOf(m);
        float[] col = {54, 96, (w - 54 - 96 - 70 - 70) / 2, (w - 54 - 96 - 70 - 70) / 2, 70, 70};
        float hh = 16, rh = 22;
        String[] heads = {"Spiel", "Partie", "Team A", "Team B", "Sätze", "Sieger"};
        float cx = x;
        c.fill(x, c.y, w, hh, 0.9f);
        for (int k = 0; k < heads.length; k++) {
            c.rect(cx, c.y, col[k], hh, 0.6f);
            c.text(cx + 3, c.y + 4, heads[k], BOLD, 8, col[k] - 6);
            cx += col[k];
        }
        c.y += hh;
        for (int r = 0; r < rubbers.size(); r++) {
            Tournament.Rubber rb = rubbers.get(r);
            String[] v = {String.valueOf(r + 1), rb.label(), posText("A", positions, rb.posA()), posText("B", positions, rb.posB()), "   :", "A   /   B"};
            cx = x;
            for (int k = 0; k < v.length; k++) {
                c.rect(cx, c.y, col[k], rh, 0.6f);
                c.text(cx + 3, c.y + 7, v[k], k < 2 ? BOLD : REGULAR, 9, col[k] - 6);
                cx += col[k];
            }
            c.y += rh;
        }
        cx = x;
        for (int k = 0; k < 4; k++) cx += col[k];
        c.rect(x, c.y, cx - x, rh, 0.6f);
        c.text(x + 3, c.y + 7, "Ergebnis der Begegnung (gewonnene Partien A : B)", BOLD, 9, cx - x - 6);
        c.rect(cx, c.y, col[4] + col[5], rh, 0.6f);
        c.text(cx + 3, c.y + 7, "       :", BOLD, 10, col[4] + col[5] - 6);
        c.y += rh + 6;
        c.paragraph("Es werden immer alle Partien gespielt. Die Partei mit Vorsprung beginnt jeden Satz mit der Vorgabe (X:0).",
                REGULAR, 8, w);
        int hcServe = 0;
        if (t.isHandicapReady() && t.getHandicapRules().getServe() == de.hts.tournament.HandicapRules.Serve.NO_ADVANTAGE) hcServe = 1;
        if (hcServe == 1) c.paragraph("Aufschlag: jeweils die Partei ohne Punktevorsprung.", REGULAR, 8, w);
        c.y += 6;

        // Vorgabe-Tabelle aus den Vorgaberegeln
        if (t.isHandicapReady()) vorgabeTable(c, t.getHandicapRules());
        c.y += 10;
        signature(c, "Unterschrift Team A:");
        signature(c, "Unterschrift Team B:");
        signature(c, "Unterschrift Schiedsrichter/in:");
        c.finish();
        return doc;
    }

    private static String posText(String letter, List<String> positions, int[] pos) {
        List<String> out = new java.util.ArrayList<>();
        for (int i : pos) out.add(letter + (i < positions.size() ? positions.get(i) : String.valueOf(i + 1)));
        return String.join(" / ", out);
    }

    /** Im Klassen-Modus: „ · Klasse n“ (nach den Grenzen des Klassentyps), sonst leer. */
    private static String handicapClass(Tournament t, PlayingClass root, de.hts.player.Player p) {
        if (!t.isHandicapReady() || p.getTtr() == null || root == null) return "";
        de.hts.tournament.HandicapRules r = t.getHandicapRules();
        if (r.getMode() != de.hts.tournament.HandicapRules.Mode.CLASSES) return "";
        boolean women = root.getType() == de.hts.tournament.ClassType.WOMEN || root.getType() == de.hts.tournament.ClassType.GIRLS;
        int k = r.bounds(r.getMode(), women).indexOf(p.getTtr());
        String s = k >= 0 ? " · Klasse " + (k + 1) : "";
        if (!women && p.getSex() == de.hts.player.Sex.FEMALE) {
            int kw = r.bounds(r.getMode(), true).indexOf(p.getTtr());
            if (kw >= 0) s += " (Damen: Klasse " + (kw + 1) + ")";
        }
        return s;
    }

    /** Die Vorgaberegeln als Tabelle: Grenzen Erwachsene/Damen und Bälle. */
    private static void vorgabeTable(PdfCanvas c, de.hts.tournament.HandicapRules r) {
        float x = c.left(), w = c.width();
        boolean classes = r.getMode() == de.hts.tournament.HandicapRules.Mode.CLASSES;
        c.y += c.text(x, c.y, "Vorgabe-Tabelle (" + r.getMode().getLabel() + ")", BOLD, 10, w) + 2;
        float[] col = {70, (w - 70 - 70) / 2, (w - 70 - 70) / 2, 70};
        String[] heads = {classes ? "Klasse" : "Abstand", "Erwachsene (> … <=)", "Damen (> … <=)", classes ? "" : "Bälle"};
        float hh = 14, rh = 14, cx = x;
        c.fill(x, c.y, w, hh, 0.9f);
        for (int k = 0; k < 4; k++) {
            c.rect(cx, c.y, col[k], hh, 0.6f);
            c.text(cx + 3, c.y + 3, heads[k], BOLD, 7.5f, col[k] - 6);
            cx += col[k];
        }
        c.y += hh;
        int n = r.rowCount(r.getMode());
        for (int i = 0; i < n; i++) {
            de.hts.tournament.HandicapRules.Bounds a = r.bounds(r.getMode(), false), d = r.bounds(r.getMode(), true);
            String balls = classes ? "" : String.valueOf(r.balls(r.getMode()).get(i));
            String[] v = {(classes ? "Klasse " : "Abstand ") + (i + 1), "> " + a.lower(i) + "  <= " + a.getUppers().get(i),
                    "> " + d.lower(i) + "  <= " + d.getUppers().get(i), balls};
            cx = x;
            for (int k = 0; k < 4; k++) {
                c.rect(cx, c.y, col[k], rh, 0.6f);
                c.text(cx + 3, c.y + 3, v[k], REGULAR, 7.5f, col[k] - 6);
                cx += col[k];
            }
            c.y += rh;
        }
        if (classes) {
            c.y += 4;
            StringBuilder sb = new StringBuilder("Bälle je Klassenunterschied: ");
            List<Integer> b = r.balls(r.getMode());
            for (int i = 0; i < b.size(); i++) sb.append(i == 0 ? "" : " · ").append(i + 1).append(i == 0 ? " Klasse: " : " Klassen: ").append(b.get(i)).append(" Bälle");
            c.paragraph(sb.toString(), REGULAR, 8, w);
        }
        c.paragraph("Doppel: die TTR-Höheren und die TTR-Tieferen werden verglichen (Mixed: Herr–Herr, Dame–Dame), beide Vorgaben "
                + "verrechnet" + (r.isHalve() ? ", dann halbiert und abgerundet." : "."), REGULAR, 7.5f, w);
    }

    private static void side(PdfCanvas c, Tournament t, Match m, String id, String caption) {
        Entrant e = t.entrant(m, id);
        List<String[]> persons = t.personLines(id);
        if (persons != null) {          // Doppel: je Person eine Zeile, Name fett, Verein klein dahinter
            c.y += c.text(c.left(), c.y, caption, REGULAR, 7, c.width(), PdfCanvas.Align.LEFT, 0.35f);
            for (String[] pl : persons) {
                nameWithClub(c, c.left(), c.y, pl[0], pl[1], true, 11, c.width(), 0f);
                c.y += 14;
            }
            handicapLine(c, t, m, id);
            c.y += 3;
            return;
        }
        c.y += c.text(c.left(), c.y, caption, REGULAR, 7, c.width(), PdfCanvas.Align.LEFT, 0.35f);
        c.y += c.text(c.left(), c.y, e == null ? "______________________" : e.name(), BOLD, 12, c.width()) - 1;
        if (e != null && e.detail() != null) c.y += c.text(c.left(), c.y, e.detail(), REGULAR, 8, c.width());
        handicapLine(c, t, m, id);
        c.y += 5;
    }

    /** Vorgabeturnier: Personenzeilen einer Partei mit „ (+X)“ hinter dem ersten Namen (Kopie; {@code null} bleibt null). */
    static List<String[]> withHandicap(Tournament t, Match m, String id, List<String[]> lines) {
        if (lines == null || lines.isEmpty() || m == null) return lines;
        String suffix = t.handicapSuffix(m, id);
        if (suffix.isEmpty()) return lines;
        List<String[]> out = new java.util.ArrayList<>();
        for (String[] l : lines) out.add(l.clone());
        out.get(0)[0] = out.get(0)[0] + suffix;
        return out;
    }

    /** Vorgabeturnier: „+ X Punkte“ klein unter dem Namen der Partei mit Vorsprung (sonst nichts). */
    private static void handicapLine(PdfCanvas c, Tournament t, Match m, String id) {
        String h = t.handicapLabel(m, id);
        if (h.isEmpty()) return;
        c.y += c.text(c.left(), c.y, "+ " + h.substring(1) + " Punkte", BOLD, 8, c.width());
    }

    private static void signature(PdfCanvas c, String caption) {
        float w = PdfCanvas.textWidth(caption, REGULAR, 8) + 6;
        c.text(c.left(), c.y, caption, REGULAR, 8, w);
        c.line(c.left() + w, c.y + 9, c.left() + c.width(), c.y + 9, 0.6f);
        c.y += 22;
    }

    // --- Phase (A4) ---------------------------------------------------------------

    /** Eine einzelne Gruppe (Tabelle + Spielplan) – dieselbe Seite wie im Phasen-PDF. */
    public static PDDocument group(Tournament t, PlayingClass phase, int g) {
        PlayingClass root = t.rootOf(phase);
        String title = root.getName() + " · Phase " + t.phaseNumber(phase) + " – " + modeTitle(phase.getMode());
        PDDocument doc = newDoc(title + " · Gruppe " + GroupDraw.groupName(g));
        PdfCanvas c = new PdfCanvas(doc, new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()), 40);   // A4 quer
        List<Match> ms = t.matchesOf(phase).stream().filter(m -> m.getGroupIndex() == g).toList();
        section(c, t, phase, title, "Gruppe " + GroupDraw.groupName(g), g, ms, List.of());
        if (doc.getNumberOfPages() == 0) c.newPage();
        c.finish();
        return doc;
    }

    public static PDDocument phase(Tournament t, PlayingClass phase) {
        PlayingClass root = t.rootOf(phase);
        String title = root.getName() + " · Phase " + t.phaseNumber(phase) + " – " + modeTitle(phase.getMode());
        PDDocument doc = newDoc(title);
        PdfCanvas c = new PdfCanvas(doc, new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()), 40);   // A4 quer
        List<Match> all = t.matchesOf(phase);
        switch (phase.getMode()) {
            case GROUPS -> {
                for (int g = 0; g < phase.getGroups().size(); g++) {
                    int gi = g;
                    section(c, t, phase, title, "Gruppe " + GroupDraw.groupName(g), g,
                            all.stream().filter(m -> m.getGroupIndex() == gi).toList(), List.of());
                }
            }
            case SWISS -> section(c, t, phase, title, "Runde " + t.swissRound(phase) + " von " + t.swissRounds(phase), 0, all,
                    List.of("Wertung: Punkte (Siege + Freilos), Buchholz (BHZ), Feinbuchholz (FBHZ), Satz- und Balldifferenz."));
            case PLACEMENT -> {
                section(c, t, phase, title, "Endgruppe – Platz 1 bis 4", 0, all.stream().filter(m -> m.getGroupIndex() == 0).toList(),
                        List.of("Wertung: Spielverhältnis, direkter Vergleich, Satz- und Ballverhältnis. Das Spiel aus der Vorrunde ist übernommen."));
                List<Match> pm = all.stream().filter(m -> m.getPlacement() > 0).toList();
                List<String> notes = new ArrayList<>();
                for (Tournament.Placing p : t.finalResult(phase).rows()) {
                    if (p.info().startsWith("ohne Gegner")) notes.add("Platz " + p.from() + ": " + p.entrant().name() + " – ohne Gegner, kampflos.");
                }
                if (!pm.isEmpty() || !notes.isEmpty()) section(c, t, phase, title, "Platzierungsspiele", -1, pm, notes);
            }
            case KNOCKOUT -> {
                KoTree.draw(c, t, phase, title);
                knockoutPlan(c, t, phase, title, all);
            }
            case QUALIFICATION -> {
                List<String> notes = new ArrayList<>();
                List<String> d = new ArrayList<>();
                java.util.Map<String, de.hts.tournament.Entrant> idx = new java.util.HashMap<>();
                for (var e : t.entrantsOf(phase)) idx.put(e.id(), e);
                for (String id : phase.getQualifiedDirect()) if (idx.containsKey(id)) d.add(clubbed(idx.get(id)));
                notes.add("Direkt qualifiziert (" + d.size() + "): " + (d.isEmpty() ? "–" : String.join(", ", d)));
                if (t.qualificationRound(phase) > 0) {
                    notes.add("KO-Qualifikation: " + phase.getQualField().size() + " Spieler – qualifiziert nach "
                            + phase.getQualRounds() + (phase.getQualRounds() == 1 ? " Sieg" : " Siegen")
                            + (phase.isQualExtra() ? ", mit Zusatzrunde für die Verlierer der letzten Runde" : "") + ".");
                    List<String> viaKo = t.qualified(phase).stream().filter(e -> !phase.getQualifiedDirect().contains(e.id()))
                            .map(PdfReports::clubbed).toList();
                    if (!viaKo.isEmpty()) notes.add("Über die KO qualifiziert (" + viaKo.size() + "): " + String.join(", ", viaKo));
                }
                List<List<de.hts.tournament.Entrant>> tiers = t.qualificationTiers(phase);
                for (int i = 0; i < tiers.size(); i++) {
                    List<String> names = tiers.get(i).stream().map(PdfReports::clubbed).toList();
                    notes.add(t.qualificationTierLabel(phase, i) + " (" + names.size() + "): " + String.join(", ", names));
                }
                section(c, t, phase, title, "Qualifikation", -1, all, notes);
            }
            default -> {
                c.newPage();
                c.text(c.left(), c.y, "Noch nicht ausgelost.", REGULAR, 11, c.width());
            }
        }
        if (doc.getNumberOfPages() == 0) c.newPage();
        c.finish();
        return doc;
    }

    private static String modeTitle(TournamentMode m) {
        return switch (m) {
            case GROUPS -> "Gruppenübersicht";
            case KNOCKOUT -> "KO-System";
            case PLACEMENT -> "Platzierungsspiele";
            case SWISS -> "Schweizer System";
            case QUALIFICATION -> "Qualifikation";
            case NONE -> "Auslosung";
        };
    }

    static void header(PdfCanvas c, Tournament t, String title, String sub) {
        c.y += c.text(c.left(), c.y, (t.getName() == null ? "" : t.getName()) + (t.getDate() != null ? " · " + t.getDate().format(DATE) : ""),
                REGULAR, 9, c.width(), PdfCanvas.Align.LEFT, 0.3f);
        c.y += c.text(c.left(), c.y, title, BOLD, 15, c.width()) + 1;
        c.y += c.text(c.left(), c.y, sub + "   ·   Stand: " + LocalDateTime.now().format(STAND), BOLD, 11, c.width());
        c.y += 4;
        c.line(c.left(), c.y, c.left() + c.width(), c.y, 0.8f);
        c.y += 8;
    }

    /** Eine Gruppe (bzw. ein Teil): neue Seite, Tabelle (wenn groupIndex ≥ 0), Spielplan; bei Bedarf Fortsetzungsseiten. */
    private static void section(PdfCanvas c, Tournament t, PlayingClass phase, String title, String sub, int groupIndex,
                                List<Match> matches, List<String> notes) {
        c.newPage();
        header(c, t, title, sub);
        if (groupIndex >= 0) {
            c.y += c.text(c.left(), c.y, "Tabelle", BOLD, 11, c.width()) + 2;
            standings(c, t, t.standings(phase, groupIndex), phase.getMode() == TournamentMode.SWISS);
            c.y += 10;
        }
        for (String n : notes) c.paragraph(n, REGULAR, 8.5f, c.width());
        if (!notes.isEmpty()) c.y += 6;
        if (groupIndex >= 0 && phase.getMode() == TournamentMode.GROUPS) {
            roundPlan(c, t, title, sub, matches);
            return;
        }
        c.y += c.text(c.left(), c.y, "Spielplan", BOLD, 11, c.width()) + 2;
        float[] cols = matchColumns(c.width());
        matchHeader(c, cols);
        if (matches.isEmpty()) c.y += c.text(c.left(), c.y, "Keine Spiele.", REGULAR, 9, c.width());
        String lastStage = null;
        for (Match m : matches) {
            boolean ko = koStyle(m);
            if (ko && !m.getStage().equals(lastStage)) {
                if (!c.fits(ROW * 2)) continuation(c, t, title, sub, cols);
                c.y += 3;
                c.y += c.text(c.left(), c.y, m.getStage(), BOLD, 9, c.width());
                lastStage = m.getStage();
            }
            if (!c.fits(ROW * planLines(t, m) + 2)) continuation(c, t, title, sub, cols);
            matchRow(c, t, m, cols);
        }
    }

    /**
     * Gruppen-Spielplan zweispaltig (je halbe Seitenbreite) ohne Nr./Runde je Zeile: jede Runde mit Titel „Runde k“
     * und kleinem Absatz; eine Runde bleibt zusammen. Die Spalten füllen sich nacheinander (links, rechts,
     * Fortsetzungsseite). Doppel: je Person eine Zeile.
     */
    private static void roundPlan(PdfCanvas c, Tournament t, String title, String sub, List<Match> matches) {
        c.y += c.text(c.left(), c.y, "Spielplan", BOLD, 11, c.width()) + 4;
        if (matches.isEmpty()) {
            c.text(c.left(), c.y, "Keine Spiele.", REGULAR, 9, c.width());
            return;
        }
        float gap = 24, colW = (c.width() - gap) / 2;
        float sets = 104, res = 24, dash = 10, name = (colW - sets - res - dash) / 2;
        float[] cols = {name, dash, name, res, sets};
        java.util.Map<Integer, List<Match>> byRound = new java.util.TreeMap<>();
        for (Match m : matches) byRound.computeIfAbsent(m.getRound(), k -> new ArrayList<>()).add(m);
        float top = c.y;
        int col = 0;
        final float row = 11f, head = 13f, gapRound = 6f;
        for (var e : byRound.entrySet()) {
            List<Match> rm = e.getValue();
            float block = head;
            for (Match m : rm) block += row * planLines(t, m);
            if (!c.fits(block)) {         // eine Runde bleibt zusammen
                if (col == 0) {
                    col = 1;
                    c.y = top;
                } else {
                    c.newPage();
                    header(c, t, title, sub + " (Fortsetzung)");
                    top = c.y;
                    col = 0;
                }
            }
            float x0 = c.left() + col * (colW + gap);
            c.text(x0, c.y, "Runde " + e.getKey(), BOLD, 8.5f, colW);
            c.y += 10;
            c.line(x0, c.y, x0 + colW, c.y, 0.4f);
            c.y += 2.5f;
            for (Match m : rm) {
                planRow(c, t, m, x0, cols, row);
                c.y += row * planLines(t, m);
            }
            c.y += gapRound;
        }
    }

    private static int planLines(Tournament t, Match m) {
        List<String[]> a = t.personLines(m.getA()), b = t.personLines(m.getB());
        return Math.max(1, Math.max(a == null ? 1 : a.size(), b == null ? 1 : b.size()));
    }

    private static void planRow(PdfCanvas c, Tournament t, Match m, float x0, float[] cols, float row) {
        boolean done = m.getStatus() == MatchStatus.DONE;
        float gray = done ? 0f : 0.35f;
        String w = m.winner();
        float x = x0;
        for (int side = 0; side < 2; side++) {
            String id = side == 0 ? m.getA() : m.getB();
            boolean bold = w != null && w.equals(id);
            List<String[]> ps = withHandicap(t, m, id, t.personLines(id));
            if (ps == null) {
                String n = t.entrantName(m, id);
                if (n != null) n += t.handicapSuffix(m, id);
                c.text(x, c.y, n == null ? "offen" : n, bold ? BOLD : REGULAR, 8f, cols[side == 0 ? 0 : 2] - 3, PdfCanvas.Align.LEFT, gray);
            } else {
                float yy = c.y;
                for (String[] pl : ps) {
                    c.text(x, yy, pl[0], bold ? BOLD : REGULAR, 8f, cols[side == 0 ? 0 : 2] - 3, PdfCanvas.Align.LEFT, gray);
                    yy += row;
                }
            }
            x += cols[side == 0 ? 0 : 2];
            if (side == 0) {
                c.text(x, c.y, "–", REGULAR, 8f, cols[1] - 3, PdfCanvas.Align.LEFT, gray);
                x += cols[1];
            }
        }
        c.text(x, c.y, m.getResultText(), BOLD, 8f, cols[3] - 3, PdfCanvas.Align.LEFT, gray);
        x += cols[3];
        c.text(x, c.y, m.getSetsText(), REGULAR, 7f, cols[4] - 3, PdfCanvas.Align.LEFT, gray);
    }

    // --- KO: Spielplan als Karten je Runde -------------------------------------------

    /** „Sieger/in Spiel 9“ bzw. „Verlierer/in Spiel 13“ für noch offene Plätze. */
    static String placeholder(Tournament t, PlayingClass phase, Match m, int side) {
        if (m.getPlacement() == 3) return t.thirdPlaceLabel(phase, side);
        for (Match x : t.matchesOf(phase)) {
            if (x.getKoRound() == m.getKoRound() - 1 && x.getKoIndex() == 2 * m.getKoIndex() + side) return "Sieger/in Spiel " + x.getNumber();
        }
        return "noch offen";
    }

    /** Spielplan je Runde mit Titel (Achtelfinale … Finale), ohne Nr./Runde-Spalten: Karten mit ausgeschriebenen Namen. */
    private static void knockoutPlan(PdfCanvas c, Tournament t, PlayingClass phase, String title, List<Match> all) {
        if (all.isEmpty()) return;
        c.newPage();
        header(c, t, title, "Spielplan");
        boolean doubles = all.stream().anyMatch(m -> t.personLines(m.getA()) != null || t.personLines(m.getB()) != null);
        int perRow = 3;
        float gap = 16, cardW = (c.width() - gap * (perRow - 1)) / perRow;
        float pt = 8.5f, line = pt + 3;
        float cardH = 13 + (doubles ? 4 : 2) * line + 7 + 12;
        java.util.LinkedHashMap<String, List<Match>> byStage = new java.util.LinkedHashMap<>();
        for (Match m : all) byStage.computeIfAbsent(m.getStage(), k -> new ArrayList<>()).add(m);
        for (var e : byStage.entrySet()) {
            if (!c.fits(16 + cardH)) {
                c.newPage();
                header(c, t, title, "Spielplan (Fortsetzung)");
            }
            c.y += c.text(c.left(), c.y, e.getKey(), BOLD, 10.5f, c.width()) + 1;
            c.line(c.left(), c.y, c.left() + c.width(), c.y, 0.5f);
            c.y += 5;
            int k = 0;
            for (Match m : e.getValue()) {
                if (k % perRow == 0 && k > 0) {
                    c.y += cardH + 8;
                    if (!c.fits(cardH)) {
                        c.newPage();
                        header(c, t, title, "Spielplan (Fortsetzung)");
                    }
                }
                float x = c.left() + (k % perRow) * (cardW + gap), y0 = c.y;
                boolean done = m.getStatus() == MatchStatus.DONE;
                String w = m.winner();
                c.text(x, y0, "Spiel " + m.getNumber(), BOLD, 8, cardW * 0.5f);
                if (done) c.text(x, y0, m.getResultText(), BOLD, 9, cardW, PdfCanvas.Align.RIGHT, 0);
                else c.text(x, y0, m.getStatus().getLabel(), REGULAR, 7.5f, cardW, PdfCanvas.Align.RIGHT, 0.45f);
                float yy = y0 + 13;
                for (int side = 0; side < 2; side++) {
                    String id = side == 0 ? m.getA() : m.getB();
                    List<String[]> ls = withHandicap(t, m, id, t.partyLines(m, id));
                    boolean win = id != null && id.equals(w);
                    if (ls.isEmpty()) {
                        c.text(x, yy, placeholder(t, phase, m, side), REGULAR, pt, cardW, PdfCanvas.Align.LEFT, 0.55f);
                        yy += line * (doubles ? 2 : 1);
                    } else {
                        for (String[] l : ls) {
                            nameWithClub(c, x, yy, l[0], l[1], win, pt, cardW, done && !win ? 0.35f : 0f);
                            yy += line;
                        }
                        if (doubles && ls.size() == 1) yy += line;
                    }
                    if (side == 0) {
                        c.text(x, yy - 1, "gegen", REGULAR, 6.5f, cardW, PdfCanvas.Align.LEFT, 0.55f);
                        yy += 7;
                    }
                }
                if (done && !m.getSetsText().isEmpty()) c.text(x, yy + 1, "Sätze: " + m.getSetsText(), REGULAR, 7, cardW, PdfCanvas.Align.LEFT, 0.3f);
                k++;
            }
            c.y += cardH + 10;
        }
    }

    private static void continuation(PdfCanvas c, Tournament t, String title, String sub, float[] cols) {
        c.newPage();
        header(c, t, title, sub + " (Fortsetzung)");
        matchHeader(c, cols);
    }

    private static void standings(PdfCanvas c, Tournament t, List<GroupStanding.Row> rows, boolean swiss) {
        String[] heads = swiss ? new String[]{"Pl.", "Name", "Verein", "Sp", "Pkt", "S:N", "Sätze", "Bälle", "BHZ", "FBHZ"}
                : new String[]{"Pl.", "Name", "Verein", "Sp", "S:N", "Sätze", "Bälle"};
        float[] widths = swiss ? new float[]{24, 140, 110, 22, 26, 32, 38, 54, 30, 34} : new float[]{24, 160, 140, 22, 34, 40, 56};
        // auf die Seitenbreite verteilen: Zusatzplatz an Name und Verein
        float extra = Math.max(0, Math.min(c.width() - sum(widths), 260));
        widths[1] += extra * 0.55f;
        widths[2] += extra * 0.45f;
        float x = c.left();
        for (int k = 0; k < heads.length; k++) {
            c.text(x, c.y, heads[k], BOLD, 8, widths[k] - 4);
            x += widths[k];
        }
        c.y += 11;
        c.line(c.left(), c.y, c.left() + sum(widths), c.y, 0.5f);
        c.y += 3;
        for (GroupStanding.Row r : rows) {
            List<String> v = new ArrayList<>(List.of(r.place() + ".", r.entrant().name(),
                    r.entrant().detail() == null ? "" : r.entrant().detail(), String.valueOf(r.played())));
            if (swiss) v.add(String.valueOf(r.score()));
            v.add(r.won() + ":" + r.lost());
            v.add(r.setsWon() + ":" + r.setsLost());
            v.add(r.pointsWon() + ":" + r.pointsLost());
            if (swiss) {
                v.add(String.valueOf(r.buchholz()));
                v.add(String.valueOf(r.fineBuchholz()));
            }
            x = c.left();
            List<String[]> persons = t.personLines(r.entrant().id());
            for (int k = 0; k < v.size(); k++) {
                if (persons != null && (k == 1 || k == 2)) {      // Doppel: je Person eine Zeile (Name bzw. Verein)
                    float yy = c.y;
                    for (String[] pl : persons) {
                        c.text(x, yy, k == 1 ? pl[0] : (pl[1] == null ? "" : pl[1]), k == 1 ? BOLD : REGULAR, 9, widths[k] - 4);
                        yy += ROW;
                    }
                } else {
                    c.text(x, c.y, v.get(k), k == 1 ? BOLD : REGULAR, 9, widths[k] - 4);
                }
                x += widths[k];
            }
            c.y += persons != null ? ROW * persons.size() + 2 : ROW;
        }
    }

    /** Nr., Runde, Partei 1, „–“, Partei 2, Ergebnis, Sätze (5 Sätze mit Verlängerung, z. B. 13:11, 12:14, …). */
    private static float[] matchColumns(float width) {
        float sets = 205;
        float fixed = 28 + 56 + 12 + 34 + sets;
        float name = (width - fixed) / 2;
        return new float[]{28, 56, name, 12, name, 34, sets};
    }

    private static void matchHeader(PdfCanvas c, float[] cols) {
        String[] heads = {"Nr.", "Runde", "Spieler/in bzw. Doppel 1", "", "Spieler/in bzw. Doppel 2", "Erg.", "Sätze"};
        float x = c.left();
        for (int k = 0; k < heads.length; k++) {
            c.text(x, c.y, heads[k], BOLD, 8, cols[k] - 4);
            x += cols[k];
        }
        c.y += 11;
        c.line(c.left(), c.y, c.left() + c.width(), c.y, 0.5f);
        c.y += 3;
    }

    /** KO-Spiele und Spiele der KO-Qualifikation: nach Runde gegliedert, Namen mit Verein. */
    static boolean koStyle(Match m) {
        return m.isKnockout() || m.getStage().startsWith("Qualifikation") || Tournament.THIRD_PLACE_STAGE.equals(m.getStage());
    }

    private static void matchRow(PdfCanvas c, Tournament t, Match m, float[] cols) {
        List<String[]> pa = withHandicap(t, m, m.getA(), t.personLines(m.getA())), pb = withHandicap(t, m, m.getB(), t.personLines(m.getB()));
        if (pa != null || pb != null) {
            doublesMatchRow(c, t, m, cols, pa, pb);
            return;
        }
        String a = t.entrantName(m, m.getA()), b = t.entrantName(m, m.getB());
        if (koStyle(m)) {     // KO-Darstellungen: „Name (Verein)“
            Entrant ea = t.entrant(m, m.getA()), eb = t.entrant(m, m.getB());
            if (ea != null) a = clubbed(ea);
            if (eb != null) b = clubbed(eb);
        }
        if (a != null) a += t.handicapSuffix(m, m.getA());
        if (b != null) b += t.handicapSuffix(m, m.getB());
        String round = m.isCarried() ? "übern." : m.getPlacement() > 0 ? "Pl. " + m.getPlacement() + "/" + (m.getPlacement() + 1)
                : m.isKnockout() ? "R" + m.getRound() : "R" + m.getRound();
        boolean done = m.getStatus() == MatchStatus.DONE;
        float gray = done ? 0f : 0.35f;
        String w = m.winner();
        String[] v = {String.valueOf(m.getNumber()), round, a == null ? "offen" : a, "–", b == null ? "offen" : b,
                m.getResultText(), m.getSetsText()};
        float x = c.left();
        for (int k = 0; k < v.length; k++) {
            boolean bold = (k == 2 && w != null && w.equals(m.getA())) || (k == 4 && w != null && w.equals(m.getB())) || k == 5;
            PDFont f = bold ? BOLD : REGULAR;
            c.text(x, c.y, v[k], f, k == 6 ? 8f : 9f, cols[k] - 4, PdfCanvas.Align.LEFT, gray);
            x += cols[k];
        }
        c.y += ROW;
    }

    /** Doppel im Spielplan: je Person eine Zeile (ausgeschrieben), Ergebnis/Sätze in der ersten Zeile. */
    private static void doublesMatchRow(PdfCanvas c, Tournament t, Match m, float[] cols, List<String[]> pa, List<String[]> pb) {
        int lines = Math.max(pa == null ? 1 : pa.size(), pb == null ? 1 : pb.size());
        String round = m.isCarried() ? "übern." : m.getPlacement() > 0 ? "Pl. " + m.getPlacement() + "/" + (m.getPlacement() + 1) : "R" + m.getRound();
        boolean done = m.getStatus() == MatchStatus.DONE;
        float gray = done ? 0f : 0.35f;
        String w = m.winner();
        float x = c.left();
        c.text(x, c.y, String.valueOf(m.getNumber()), REGULAR, 9, cols[0] - 4, PdfCanvas.Align.LEFT, gray);
        x += cols[0];
        c.text(x, c.y, round, REGULAR, 9, cols[1] - 4, PdfCanvas.Align.LEFT, gray);
        x += cols[1];
        personCell(c, x, c.y, pa, t.entrantName(m, m.getA()), w != null && w.equals(m.getA()), cols[2] - 4, gray, koStyle(m));
        x += cols[2];
        c.text(x, c.y, "–", REGULAR, 9, cols[3] - 4, PdfCanvas.Align.LEFT, gray);
        x += cols[3];
        personCell(c, x, c.y, pb, t.entrantName(m, m.getB()), w != null && w.equals(m.getB()), cols[4] - 4, gray, koStyle(m));
        x += cols[4];
        c.text(x, c.y, m.getResultText(), BOLD, 9, cols[5] - 4, PdfCanvas.Align.LEFT, gray);
        x += cols[5];
        c.text(x, c.y, m.getSetsText(), REGULAR, 8, cols[6] - 4, PdfCanvas.Align.LEFT, gray);
        c.y += ROW * lines + 2;
    }

    /** Zelle mit einer Person je Zeile (Doppel) bzw. dem Namen. */
    private static void personCell(PdfCanvas c, float x, float y, List<String[]> persons, String fallback, boolean bold, float w,
                                   float gray, boolean withClub) {
        if (persons == null) {
            c.text(x, y, fallback == null ? "offen" : fallback, bold ? BOLD : REGULAR, 9, w, PdfCanvas.Align.LEFT, gray);
            return;
        }
        float yy = y;
        for (String[] pl : persons) {
            if (withClub) nameWithClub(c, x, yy, pl[0], pl[1], bold, 9, w, gray);
            else c.text(x, yy, pl[0], bold ? BOLD : REGULAR, 9, w, PdfCanvas.Align.LEFT, gray);
            yy += ROW;
        }
    }

    /** Name (fett/normal), dahinter klein und grau der Verein – gekürzt, wenn der Platz knapp ist. */
    static void nameWithClub(PdfCanvas c, float x, float y, String name, String club, boolean bold, float pt, float w, float gray) {
        PDFont f = bold ? BOLD : REGULAR;
        String n = PdfCanvas.fit(PdfCanvas.clean(name == null ? "" : name, f), f, pt, w);
        c.text(x, y, n, f, pt, w, PdfCanvas.Align.LEFT, gray);
        float nw = PdfCanvas.textWidth(n, f, pt);
        if (club != null && !club.isBlank() && nw + 24 < w) {
            c.text(x + nw + 3, y + pt * 0.08f, "(" + club + ")", REGULAR, pt - 1.6f, w - nw - 3, PdfCanvas.Align.LEFT, Math.max(gray, 0.45f));
        }
    }

    private static String clubbed(Entrant e) {
        return e.detail() == null || e.detail().isBlank() ? e.name() : e.name() + " (" + e.detail() + ")";
    }

    private static float sum(float[] a) {
        float s = 0;
        for (float f : a) s += f;
        return s;
    }

    // --- Endergebnis ----------------------------------------------------------------

    public static PDDocument finalResult(Tournament t, PlayingClass cls) {
        PlayingClass root = t.rootOf(cls);
        PDDocument doc = newDoc("Endergebnis " + root.getName());
        PdfCanvas c = new PdfCanvas(doc, PDRectangle.A4, 40);
        writeFinal(c, t, root);
        c.finish();
        return doc;
    }

    /** Gesamtergebnis des Turniers: je Klasse das Endergebnis (jede Klasse auf neuer Seite). */
    public static PDDocument tournamentResults(Tournament t) {
        PDDocument doc = newDoc("Ergebnisse " + (t.getName() == null ? "" : t.getName()));
        PdfCanvas c = new PdfCanvas(doc, PDRectangle.A4, 40);
        for (PlayingClass root : t.rootClasses()) {
            if (root.isDrawn()) writeFinal(c, t, root);
        }
        if (doc.getNumberOfPages() == 0) c.newPage();
        c.finish();
        return doc;
    }

    private static void writeFinal(PdfCanvas c, Tournament t, PlayingClass root) {
        Tournament.FinalResult r = t.finalResult(root);
        String title = "Endergebnis " + root.getName();
        float[] cols = {48, 170, 150, 40, 0};
        cols[4] = c.width() - 48 - 170 - 150 - 40;
        String sub = (r.complete() && root.getFinishedAt() != null ? "beendet am " + root.getFinishedAt().format(STAND) : "Zwischenstand")
                + " · " + r.rows().size() + " Platzierungen";
        boolean first = true;
        for (int i = 0; i < r.rows().size() || first; i++) {
            if (first || !c.fits(2 * ROW + 6)) {
                c.newPage();
                c.y += c.text(c.left(), c.y, (t.getName() == null ? "" : t.getName()) + (t.getDate() != null ? " · " + t.getDate().format(DATE) : ""),
                        REGULAR, 9, c.width(), PdfCanvas.Align.LEFT, 0.3f);
                c.y += c.text(c.left(), c.y, title, BOLD, 16, c.width()) + 1;
                c.y += c.text(c.left(), c.y, sub, REGULAR, 10, c.width());
                c.y += 4;
                c.line(c.left(), c.y, c.left() + c.width(), c.y, 0.8f);
                c.y += 8;
                String[] heads = {"Platz", "Name", "Verein", "TTR", "Ermittelt durch"};
                float x = c.left();
                for (int k = 0; k < heads.length; k++) {
                    c.text(x, c.y, heads[k], BOLD, 8.5f, cols[k] - 4);
                    x += cols[k];
                }
                c.y += 12;
                c.line(c.left(), c.y, c.left() + c.width(), c.y, 0.5f);
                c.y += 4;
                first = false;
            }
            if (i >= r.rows().size()) break;
            Tournament.Placing p = r.rows().get(i);
            List<String[]> persons = t.personLines(p.entrant().id());
            String[] v = {p.placeText(), p.entrant().name(), p.entrant().detail() == null ? "" : p.entrant().detail(),
                    persons != null || p.entrant().ttr() == null ? "" : String.valueOf(p.entrant().ttr()), p.info()};
            float x = c.left();
            for (int k = 0; k < v.length; k++) {
                if (persons != null && (k == 1 || k == 2)) {      // Doppel: je Person eine Zeile
                    float yy = c.y;
                    for (String[] pl : persons) {
                        c.text(x, yy, k == 1 ? pl[0] : (pl[1] == null ? "" : pl[1]), k <= 1 ? BOLD : REGULAR, k <= 1 ? 10 : 9, cols[k] - 4);
                        yy += ROW + 1;
                    }
                } else {
                    c.text(x, c.y, v[k], k <= 1 ? BOLD : REGULAR, k <= 1 ? 10 : 9, cols[k] - 4);
                }
                x += cols[k];
            }
            c.y += persons != null ? (ROW + 1) * persons.size() + 3 : ROW + 2;
        }
    }
}

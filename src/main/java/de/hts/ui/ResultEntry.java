package de.hts.ui;

import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.Tournament;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.function.Consumer;

/** Ergebnis eintragen bzw. korrigieren – gemeinsam für Tische, Spiele, Gruppenübersicht und Turnierbaum. */
final class ResultEntry {

    private ResultEntry() {
    }

    /** Kann für dieses Spiel ein Ergebnis eingetragen oder korrigiert werden? */
    static boolean isEditable(Match m) {
        return m != null && m.isReady()
                && (m.getStatus() == MatchStatus.ACTIVE || m.getStatus() == MatchStatus.QUEUED || m.getStatus() == MatchStatus.DONE);
    }

    /** @return {@code true}, wenn gespeichert wurde */
    static boolean open(Window owner, Tournament t, Match m, Consumer<String> status) {
        if (!isEditable(m)) return false;
        boolean correction = m.getStatus() == MatchStatus.DONE;
        if (t.isTeamMatch(m)) return openTeam(owner, t, m, status, correction);
        var result = new ResultDialog(owner, t, m).showAndWait();
        if (result.isEmpty()) return false;
        Integer table = m.getTable();
        try {
            if (correction) {
                try {
                    t.correctResult(m, result.get().sets(), result.get().retired());
                } catch (Tournament.FollowUpConflict conflict) {
                    Alert ask = new Alert(Alert.AlertType.CONFIRMATION, conflict.getMessage()
                            + "\n\nDiese Folgespiele werden zurückgesetzt (Ergebnis gelöscht) und mit dem neuen Sieger neu eingereiht. "
                            + "Ist die Klasse schon beendet, muss sie danach erneut abgeschlossen werden.\n\nKorrektur trotzdem speichern?",
                            ButtonType.YES, ButtonType.NO);
                    ask.initOwner(owner);
                    ask.setHeaderText(null);
                    ask.setTitle("Sieger ändert sich");
                    if (ask.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return false;
                    t.correctResult(m, result.get().sets(), result.get().retired(), true);
                }
            } else {
                t.recordResult(m, result.get().sets(), result.get().retired());
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            a.initOwner(owner);
            a.setHeaderText(null);
            a.setTitle(correction ? "Korrektur nicht möglich" : "Ergebnis nicht gespeichert");
            a.showAndWait();
            return false;
        }
        status.accept("Spiel " + m.getNumber() + " (" + m.getClassName() + "): " + m.getResultText() + " – " + m.getSetsText()
                + (correction ? " (korrigiert)" : table != null ? " · " + t.tableName(table) + " ist frei" : ""));
        askQualification(owner, t, m, status);
        return true;
    }

    /** 0.38 (#60): Ist Runde 1 der KO-Qualifikation fertig, fragen, ob die Gewinner qualifiziert sind. */
    static void askQualification(Window owner, Tournament t, Match m, Consumer<String> status) {
        de.hts.tournament.PlayingClass c = t.findClass(m.getClassName()).orElse(null);
        if (!t.qualificationDecisionPending(c)) return;
        ButtonType yes = new ButtonType("Gewinner sind qualifiziert", javafx.scene.control.ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("Weitere KO-Runde für die Gewinner", javafx.scene.control.ButtonBar.ButtonData.NO);
        ButtonType later = new ButtonType("Später entscheiden", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Runde 1 der KO-Qualifikation von „" + t.rootOf(c).getName() + "“ ist gespielt.\n\n"
                        + "Sind die Gewinner jetzt qualifiziert? Dann sind die Verlierer Nachrücker 1. Ranges (nach TTR).\n"
                        + "Sonst spielen die Gewinner eine weitere KO-Runde (höchstens 2 Runden).\n\n"
                        + "„Später entscheiden“: Die Knöpfe stehen in der Auslosungsansicht der Phase.", yes, no, later);
        a.initOwner(owner);
        a.setHeaderText(null);
        a.setTitle("Qualifikation nach Runde 1");
        DialogSizing.fit(a);
        ButtonType r = a.showAndWait().orElse(later);
        if (r == later) return;
        t.decideQualification(c, r == yes);
        status.accept(r == yes ? "Die Gewinner der Runde 1 sind qualifiziert." : "Runde 2 für die Gewinner angelegt.");
    }

    /** 2er/3er Turnier: Aufstellung und Sieger je Partie. */
    private static boolean openTeam(Window owner, Tournament t, Match m, Consumer<String> status, boolean correction) {
        var result = new TeamResultDialog(owner, t, m).showAndWait();
        if (result.isEmpty()) return false;
        Integer table = m.getTable();
        TeamResultDialog.Result r = result.get();
        try {
            try {
                t.recordTeamResult(m, r.lineupA(), r.lineupB(), r.winners(), false);
            } catch (Tournament.FollowUpConflict conflict) {
                Alert ask = new Alert(Alert.AlertType.CONFIRMATION, conflict.getMessage()
                        + "\n\nDiese Folgespiele werden zurückgesetzt und mit dem neuen Sieger neu eingereiht.\n\nKorrektur trotzdem speichern?",
                        ButtonType.YES, ButtonType.NO);
                ask.initOwner(owner);
                ask.setHeaderText(null);
                ask.setTitle("Sieger ändert sich");
                if (ask.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return false;
                t.recordTeamResult(m, r.lineupA(), r.lineupB(), r.winners(), true);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            a.initOwner(owner);
            a.setHeaderText(null);
            a.setTitle(correction ? "Korrektur nicht möglich" : "Ergebnis nicht gespeichert");
            a.showAndWait();
            return false;
        }
        status.accept("Spiel " + m.getNumber() + " (" + m.getClassName() + "): " + m.getResultText()
                + (correction ? " (korrigiert)" : table != null ? " · " + t.tableName(table) + " ist frei" : ""));
        askQualification(owner, t, m, status);
        return true;
    }
}

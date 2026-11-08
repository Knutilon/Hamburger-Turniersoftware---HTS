package de.hts.tournament;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** HTS 0.41.0 – Qualifikationsrunde 2 erst per „Qualifikation Runde 2 starten“ (#70). */
class Version041Test {

    @Test
    void rundeZweiWartetAufStartUndIstTauschbar() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = PhaseFixtures.groupsClass(t, "Q", 16, 4);
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        PlayingClass q = t.createQualificationPhase(c, 1, 2, 2, 0, new Random(1));
        assertTrue(q.isAwaitingStart(), "Runde 1 wartet auf den Start");
        t.resumeClass(q);
        for (Match m : List.copyOf(t.matchesOf(q))) PhaseFixtures.play(t, m);
        t.decideQualification(q, false);
        assertTrue(q.isAwaitingStart(), "Runde 2 wartet auf den Start");
        assertEquals(2, t.qualificationRound(q));
        List<Match> r2 = t.matchesOf(q).stream().filter(m -> m.getRound() == 2).toList();
        assertEquals(2, r2.size());
        assertTrue(r2.stream().allMatch(m -> m.getStatus() == MatchStatus.PLANNED), "noch keine Tische");
        String a = r2.get(0).getA(), b = r2.get(1).getA();
        t.swapQualificationPlayers(q, a, b);                                   // Drag & Drop in Runde 2
        assertEquals(b, r2.get(0).getA());
        t.resumeClass(q);
        assertFalse(q.isAwaitingStart());
        assertTrue(r2.stream().allMatch(m -> m.getStatus() == MatchStatus.ACTIVE || m.getStatus() == MatchStatus.QUEUED));
        // zurücknehmen vor dem Start hebt das Warten auf
        PlayingClass c2 = PhaseFixtures.groupsClass(t, "R", 16, 4);
        t.startClass(c2);
        PhaseFixtures.playAll(t, c2);
        PlayingClass q2 = t.createQualificationPhase(c2, 1, 2, 2, 0, new Random(2));
        t.resumeClass(q2);
        for (Match m : List.copyOf(t.matchesOf(q2))) PhaseFixtures.play(t, m);
        t.decideQualification(q2, false);
        t.undoQualificationDecision(q2);
        assertFalse(q2.isAwaitingStart());
        assertFalse(q2.isPaused());
        assertTrue(t.qualificationDecisionPending(q2));
    }
}

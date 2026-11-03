package de.hts.ui;

/** Programmweite Einstellungen der Oberfläche. */
public final class Ui {

    /** „1 Spiel“ / „n Spiele“. */
    public static String spiele(long n) {
        return n + (n == 1 ? " Spiel" : " Spiele");
    }


    private Ui() {
    }

    /** Alle Knöpfe in allen Fenstern ausgeschrieben (nie mit „…“ gekürzt). */
    public static void installButtonSizing() {
        ButtonSizing.installEverywhere();
    }
}

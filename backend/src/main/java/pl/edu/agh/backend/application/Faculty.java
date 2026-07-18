package pl.edu.agh.backend.application;

public enum Faculty {
    WE("Wydział Elektromechaniczny (1952-1957)"),
    WEGH("Wydział Elektrotechniki Górniczej i Hutniczej (1957-1975)"),
    WEAIE("Wydział Elektrotechniki, Automatyki i Elektroniki (1975-1998)"),
    WEAIIE("Wydział Elektrotechniki, Automatyki, Informatyki i Elektroniki (1998-2011)"),
    WIET("Wydział Informatyki, Elektroniki i Telekomunikacji (2012-2023)"),
    WI("Wydział Informatyki (2023-obecnie)");

    private final String displayName;

    Faculty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

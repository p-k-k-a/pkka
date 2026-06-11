package pl.edu.agh.backend.application;

public enum Faculty {
    WILGZ("Wydział Inżynierii Lądowej i Gospodarki Zasobami"),
    WIMIIP("Wydział Inżynierii Metali i Informatyki Przemysłowej"),
    WEAIIB("Wydział Elektrotechniki, Automatyki, Informatyki i Inżynierii Biomedycznej"),
    WIET("Wydział Informatyki, Elektroniki i Telekomunikacji"),
    WIMIR("Wydział Inżynierii Mechanicznej i Robotyki"),
    WGGIOS("Wydział Geologii, Geofizyki i Ochrony Środowiska"),
    WGGIIS("Wydział Geodezji Górniczej i Inżynierii Środowiska"),
    WIMIC("Wydział Inżynierii Materiałowej i Ceramiki"),
    WO("Wydział Odlewnictwa"),
    WMN("Wydział Metali Nieżelaznych"),
    WWNIG("Wydział Wiertnictwa, Nafty i Gazu"),
    WZ("Wydział Zarządzania"),
    WEIP("Wydział Energetyki i Paliw"),
    WFIIS("Wydział Fizyki i Informatyki Stosowanej"),
    WMS("Wydział Matematyki Stosowanej"),
    WH("Wydział Humanistyczny"),
    WI("Wydział Informatyki"),
    WTK("Wydział Technologii Kosmicznych");

    private final String displayName;

    Faculty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

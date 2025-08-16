
# Prüfungs-Requirements – Thema „Protokolle / Logdateien“

## Projektbeschreibung (/P1–/P6)

### /P1 (Experiment)
Implementiere zweimal dieselbe Web-Anwendung zur Verwaltung von Protokollen (Logeinträgen):  
- 1× mit **Spring**  
- 1× mit **Javalin**  
Vergleiche anschließend Umsetzungsaufwand und Ergebnisqualität.

### /P2 (Frameworks)
- **Spring**: in der Vorlesung behandelt.
- **Javalin**: frei gewählt, ermöglicht Java-Webentwicklung, ist quelloffen und kostenfrei auch kommerziell nutzbar.

### /P3 (Vergleich)
Bewerte nach der Doppel-Implementierung:
1. Wie leicht fiel die Umsetzung je Framework?
2. Welche Qualität haben die Ergebnisse?

### /P4 (Vergleichsobjekt)
Log-Management-App zum Einsehen und Filtern von Logeinträgen.

### /P5 (Funktional)
CRUD+Search für die Entität **Protokoll** mit Persistenz in DB:
- Anlegen
- Ändern
- Löschen
- Anzeigen
- Suchen/Filtern (z. B. nach Zeitraum, Level, Quelle, Text)

### /P6 (Domänenmodell)
Entität **Protokoll** mit ≥ 7 editierbaren Attributen, z. B.:
- `id`
- `timestamp`
- `logLevel` (INFO/WARN/ERROR …)
- `source` (Modul/Service)
- `message` (Text)
- `username` (optional)
- `category/tags`
- `status` (offen/erledigt/ignoriert)

---

## Hinweise & Umfang (/R1–/R5)

- **/R1–/R2**: Anforderungen sind bewusst nicht vollständig und können widersprüchlich sein (realitätsnah).
- **/R3**: Identifiziere Lücken/Unklarheiten und führe zu geeigneten Lösungen.
- **/R4**: Triff begründete Annahmen (an den Qualifikationszielen orientiert), um die Ziele bestmöglich nachzuweisen.
- **/R5**: Hausarbeit 10–20 Seiten (gem. Prüfungsamt).

---

## Abzuliefernde Lösungen (/A1–/A7)

1. **A1**: Architekturentwurf der Log-App (z. B. Schichten + MVC; REST-Endpunkte für Filter/Suche).
2. **A2**: Framework-Auswahl: Spring & Javalin.
3. **A3**: Begründung der Entscheidungen (Motivation, Trade-offs, Eignung fürs Thema).


4. **A4**: Implementierung der App in beiden Frameworks (gleiches Feature-Set).
5. **A5**: Vergleich & Beschreibung der Lösungen (inkl. Screenshots/Flows).
6. **A6**: Quellcodes vollständig im Anhang (maschinenlesbar, zuordenbar zu Dateien/Projekten).
7. **A7**: Lauffähigkeit in der Referenz-Laufzeitumgebung (Eclipse 4.22, Java 17, Tomcat, PostgreSQL, Windows 10 Enterprise). Falls nötig: Installations-/Startanleitung beilegen.

---

## Konkretisierung für dein Thema

### Funktionale Anforderungen (Auszug)
- Listenansicht mit Paginierung und Mehrfachfiltern (Datumsbereich, Level, Source, Textsuche)
- Detailansicht eines Logeintrags
- CRUD inkl. Statuspflege (z. B. Logs „bearbeitet/ignoriert“ markieren)
- Export der gefilterten Liste (CSV/JSON) – optional, aber stark für die Ergebnisqualität
- Einfache Rechteprüfung (optional: nur lesen vs. bearbeiten)

### Nicht-funktionale Anforderungen (naheliegende Kriterien für den Vergleich)
- Bedienbarkeit (Klarheit der UI, Filter-UX)
- Wartbarkeit (Struktur, Testbarkeit)
- Performance (Filter-Antwortzeiten bei typischen Datenmengen)
- Implementierungsaufwand (LOC, Konfigurationsaufwand, Doku/Community)
- Deployment-Einfachheit (Startskript/README, WAR/JAR)

### Datenmodell (Beispiel-DDL, beide Frameworks identisch)
- Tabelle `log_entry` mit o. g. Attributen; Indizes auf `timestamp`, `logLevel`, `source` für schnelle Filter.

### Vergleichsdesign (Leitplanken)
**Messpunkte:**
- Zeit für Grundgerüst (Projektsetup, Routing, DB-Anbindung)
- Zeit für CRUD
- Zeit für komplexere Filter
- Umfang (klassen-/methoden-/zeilenbasiert)
- Qualität (Tests, Fehlerhandling, Validierung)

**Artefakte:**
Screenshots, kurze Code-Ausschnitte (repräsentativ), Tabellen mit Metriken, kurze Reflexion.

---

## Abgabe- und Laufzeitpaket (Praxis-Check)
- Beide Projekte (Spring & Javalin) als importfähige Eclipse-Projekte + auslieferbare Artefakte (z. B. WAR/JAR)
- README pro Projekt: Startvoraussetzungen, DB-Schema/DDL, Konfiguration, Startbefehle, Testnutzer (falls nötig)
- Verifikation: Lokalstart mit Java 17 und der genannten Umgebung sicherstellen

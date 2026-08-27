import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Zaklada role i bazy dla projektu Ankietor.
 *
 * Wszystkie credentiale czytane sa ze zmiennych srodowiskowych i nigdzie nie sa
 * wypisywane. Skrypt jest idempotentny - mozna go uruchomic ponownie.
 *
 * Wymagane zmienne:
 *   PG_HOST              podaj adres serwera
 *   PG_PORT              domyslnie 5433
 *   PG_ADMIN_DB          baza do polaczenia administracyjnego, domyslnie postgres
 *   PG_ADMIN_USER        uzytkownik z prawem CREATE ROLE / CREATE DATABASE
 *   PG_ADMIN_PASSWORD    jego haslo
 *   ANKIETOR_DB_PASSWORD haslo, ktore ma dostac nowa rola ankietor
 *
 * Uruchomienie:
 *   java -cp "<sciezka-do-postgresql.jar>" DbBootstrap.java
 */
public class DbBootstrap {

    private static final String ROLE = "ankietor";
    private static final String[] DATABASES = {"ankietor", "ankietor_test"};
    private static final String[] EXTENSIONS = {"pg_trgm", "unaccent"};

    public static void main(String[] args) {
        String host = env("PG_HOST", "localhost");
        String port = env("PG_PORT", "5433");
        String adminDb = env("PG_ADMIN_DB", "postgres");
        String adminUser = required("PG_ADMIN_USER");
        String adminPass = required("PG_ADMIN_PASSWORD");
        String rolePass = required("ANKIETOR_DB_PASSWORD");

        String adminUrl = "jdbc:postgresql://" + host + ":" + port + "/" + adminDb;
        System.out.println("Serwer      : " + host + ":" + port);
        System.out.println("Baza admin  : " + adminDb);
        System.out.println("Uzytkownik  : " + adminUser);
        System.out.println();

        try (Connection admin = DriverManager.getConnection(adminUrl, adminUser, adminPass)) {
            printServerVersion(admin);
            createRole(admin, rolePass);
            for (String db : DATABASES) {
                createDatabase(admin, db);
            }
        } catch (SQLException e) {
            fail("Polaczenie administracyjne nie powiodlo sie", e);
            return;
        }

        // Rozszerzenia zakladamy juz w kazdej bazie osobno - CREATE EXTENSION
        // dziala w kontekscie konkretnej bazy, nie klastra.
        for (String db : DATABASES) {
            String dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db;
            try (Connection conn = DriverManager.getConnection(dbUrl, adminUser, adminPass)) {
                grantSchema(conn, db);
                for (String ext : EXTENSIONS) {
                    createExtension(conn, db, ext);
                }
            } catch (SQLException e) {
                fail("Nie udalo sie skonfigurowac bazy " + db, e);
                return;
            }
        }

        System.out.println();
        System.out.println("GOTOWE. Ustaw teraz zmienne aplikacji:");
        System.out.println("  DB_URL      = jdbc:postgresql://" + host + ":" + port + "/ankietor");
        System.out.println("  DB_USER     = " + ROLE);
        System.out.println("  DB_PASSWORD = (to, co podales w ANKIETOR_DB_PASSWORD)");
    }

    private static void printServerVersion(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT version()")) {
            if (rs.next()) {
                System.out.println("[i] " + rs.getString(1));
                System.out.println();
            }
        }
    }

    private static void createRole(Connection conn, String password) throws SQLException {
        if (exists(conn, "SELECT 1 FROM pg_roles WHERE rolname = '" + ROLE + "'")) {
            System.out.println("[=] rola " + ROLE + " juz istnieje - haslo bez zmian");
            return;
        }
        // CREATE ROLE nie przyjmuje parametrow JDBC, wiec haslo musi znalezc sie
        // w tresci polecenia. Uzywamy format(%L) po stronie serwera, zeby poprawnie
        // zacytowac wartosc i uniknac wstrzykniecia.
        //
        // UWAGA: haslo bedzie widoczne w tresci wykonanego polecenia, wiec przy
        // log_statement = all / ddl trafi do logu serwera. To ograniczenie
        // samego CREATE ROLE, nie tego skryptu. Nie trafia natomiast do historii
        // powloki ani do transkryptu sesji.
        try (var ps = conn.prepareStatement(
                "SELECT format('CREATE ROLE " + ROLE + " WITH LOGIN PASSWORD %L', ?)")) {
            ps.setString(1, password);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                String ddl = rs.getString(1);
                try (Statement st = conn.createStatement()) {
                    st.execute(ddl);
                }
            }
        }
        System.out.println("[+] utworzono role " + ROLE);
    }

    private static void createDatabase(Connection conn, String db) throws SQLException {
        if (exists(conn, "SELECT 1 FROM pg_database WHERE datname = '" + db + "'")) {
            System.out.println("[=] baza " + db + " juz istnieje");
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE DATABASE " + db + " OWNER " + ROLE + " ENCODING 'UTF8'");
        }
        System.out.println("[+] utworzono baze " + db);
    }

    private static void grantSchema(Connection conn, String db) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("GRANT ALL ON SCHEMA public TO " + ROLE);
        }
        System.out.println("[+] " + db + ": GRANT ALL ON SCHEMA public TO " + ROLE);
    }

    private static void createExtension(Connection conn, String db, String ext) {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE EXTENSION IF NOT EXISTS " + ext);
            System.out.println("[+] " + db + ": rozszerzenie " + ext + " gotowe");
        } catch (SQLException e) {
            System.out.println("[!] " + db + ": nie udalo sie zalozyc " + ext
                    + " -> " + e.getMessage());
            System.out.println("    Potrzebne konto z wyzszymi uprawnieniami.");
        }
    }

    private static boolean exists(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String required(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            System.err.println("BRAK zmiennej srodowiskowej: " + name);
            System.exit(2);
        }
        return v;
    }

    private static void fail(String message, SQLException e) {
        System.err.println();
        System.err.println("BLAD: " + message);
        System.err.println("  SQLState : " + e.getSQLState());
        System.err.println("  Komunikat: " + e.getMessage());
        System.exit(1);
    }
}

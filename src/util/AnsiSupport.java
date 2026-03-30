package util;

/**
 * Detects whether ANSI escape sequences can be used on stdout.
 * <p>
 * Local and network clients use this so behaviour matches. On Windows, call
 * {@code org.fusesource.jansi.AnsiConsole.systemInstall()} in {@code main} before
 * constructing the UI so PowerShell and cmd interpret colour codes.
 * </p>
 */
public final class AnsiSupport {

    private AnsiSupport() {
    }

    /**
     * @return true when interactive output should use ANSI styling
     */
    public static boolean isSupported() {
        if (System.console() == null) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            // Jansi's systemInstall() enables VT processing on Windows 10+ consoles.
            return true;
        }
        String term = System.getenv("TERM");
        return term != null && !term.equalsIgnoreCase("dumb");
    }
}

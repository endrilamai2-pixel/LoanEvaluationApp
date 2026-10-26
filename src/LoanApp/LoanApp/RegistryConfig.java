package LoanApp.LoanApp;


    /**
     * Επιλέγει την υπηρεσία μητρώου.
     * Αν οριστεί API key (μεταβλητή περιβάλλοντος GEMI_API_KEY ή -Dgemi.api.key=...) χρησιμοποιείται το πραγματικό ΓΕΜΗ,
     * αλλιώς τα δοκιμαστικά δεδομένα.
     */
    public final class RegistryConfig {
        private RegistryConfig() {}

        public static CompanyRegistryService create() {
            String key = System.getenv("GEMI_API_KEY");
            if (key == null || key.isBlank()) key = System.getProperty("gemi.api.key");
            if (key == null || key.isBlank()) return new MockCompanyRegistry();
            return new GemiOpenDataClient(key.trim());
        }
    }



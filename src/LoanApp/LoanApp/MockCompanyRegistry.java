package LoanApp.LoanApp;


    import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

    /**
     * Δοκιμαστικό μητρώο με ΦΑΝΤΑΣΤΙΚΕΣ επιχειρήσεις, για επίδειξη και δοκιμές χωρίς API key.
     * ΔΕΝ περιέχει πραγματικά δεδομένα.
     */
    public class MockCompanyRegistry implements CompanyRegistryService {

        private static final Map<String, CompanyRecord> DATA = Map.of(
                "123456783", new CompanyRecord("100000000001", "123456783", "ΑΛΦΑ ΤΡΟΦΙΜΑ Α.Ε.", "Ανώνυμη Εταιρεία",
                        "Ενεργή", LocalDate.of(2012, 3, 15), "Αθήνα"),
                "234567897", new CompanyRecord("100000000002", "234567897", "ΒΗΤΑ ΥΠΗΡΕΣΙΕΣ Ο.Ε.", "Ομόρρυθμη Εταιρεία",
                        "Ενεργή", LocalDate.of(2021, 9, 1), "Θεσσαλονίκη"),
                "345678914", new CompanyRecord("100000000003", "345678914", "ΓΑΜΜΑ ΜΕΤΑΦΟΡΕΣ Ι.Κ.Ε.", "Ιδιωτική Κεφαλαιουχική Εταιρεία",
                        "Υπό εκκαθάριση", LocalDate.of(2015, 6, 10), "Πάτρα"),
                "456789125", new CompanyRecord("100000000004", "456789125", "ΔΕΛΤΑ ΚΑΤΑΣΚΕΥΕΣ Α.Ε.", "Ανώνυμη Εταιρεία",
                        "Πτώχευση", LocalDate.of(2005, 1, 20), "Ηράκλειο")
        );

        @Override
        public Optional<CompanyRecord> findByTaxId(String taxId) {
            return Optional.ofNullable(DATA.get(taxId));
        }

        @Override
        public String sourceName() {
            return "δοκιμαστικά δεδομένα (δεν έχει οριστεί GEMI_API_KEY)";
        }
    }



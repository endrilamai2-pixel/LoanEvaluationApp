package LoanApp.LoanApp;



import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

    /**
     * Πελάτης για το ΓΕΜΗ Open Data API (https://opendata-api.businessportal.gr).
     *
     * ΠΡΟΣΟΧΗ: Αυτός ο πελάτης γράφτηκε χωρίς πρόσβαση στο API (απαιτείται προσωπικό API key από
     * https://opendata.businessportal.gr/register/) και ΔΕΝ έχει δοκιμαστεί με πραγματική απάντηση.
     * Τα σημεία που πρέπει να επιβεβαιωθούν στο Swagger (https://opendata-api.businessportal.gr/opendata/docs/)
     * είναι τα σταθερά ΜΕΡΟΣ 1 και τα ονόματα πεδίων ΜΕΡΟΣ 2 παρακάτω.
     *
     * Το API έχει όριο ~8 αιτημάτων/λεπτό. Η αναζήτηση γίνεται μόνο όταν πατηθεί το κουμπί.
     */
    public class GemiOpenDataClient implements CompanyRegistryService {

        // ===================== ΜΕΡΟΣ 1: ΕΠΙΒΕΒΑΙΩΣΕ ΣΤΟ SWAGGER =====================
        static final String DEFAULT_BASE_URL = "https://opendata-api.businessportal.gr/opendata";
        static final String SEARCH_PATH = "/companies";          // ΥΠΟΘΕΤΙΚΟ path αναζήτησης
        static final String SEARCH_PARAM_TAX_ID = "afm";         // ΥΠΟΘΕΤΙΚΟ όνομα παραμέτρου ΑΦΜ
        static final String API_KEY_NAME = "api_key";            // στέλνεται και ως header και ως query param

        // ===================== ΜΕΡΟΣ 2: ΟΝΟΜΑΤΑ ΠΕΔΙΩΝ ΣΤΗΝ ΑΠΑΝΤΗΣΗ (πεζά) =====================
        static final Set<String> NAME_KEYS   = Set.of("name", "legalname", "companyname", "cotitle", "title", "onomasia");
        static final Set<String> TAX_KEYS    = Set.of("afm", "taxnumber", "taxid", "vatnumber");
        static final Set<String> GEMI_KEYS   = Set.of("gemi", "gemiid", "gemi_number", "gemiidnumber", "argemi", "ar_gemi");
        static final Set<String> FORM_KEYS   = Set.of("legaltype", "legaltypedescr", "legalform", "legalformdescr");
        static final Set<String> STATUS_KEYS = Set.of("status", "companystatus", "statusdescr", "companystatusdescr");
        static final Set<String> DATE_KEYS   = Set.of("incorporationdate", "registrationdate", "dateincorporated", "dateofincorporation");
        static final Set<String> CITY_KEYS   = Set.of("city", "town", "address", "registeredoffice", "area");

        private final String apiKey;
        private final String baseUrl;
        private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        public GemiOpenDataClient(String apiKey) {
            this(apiKey, DEFAULT_BASE_URL);
        }

        public GemiOpenDataClient(String apiKey, String baseUrl) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
        }

        @Override
        public Optional<CompanyRecord> findByTaxId(String taxId) {
            String url = baseUrl + SEARCH_PATH + "?" + SEARCH_PARAM_TAX_ID + "=" + enc(taxId)
                    + "&" + API_KEY_NAME + "=" + enc(apiKey);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header(API_KEY_NAME, apiKey)
                    .GET().build();
            try {
                HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int code = resp.statusCode();
                if (code == 404) return Optional.empty();
                if (code == 401 || code == 403)
                    throw new RegistryException("Άκυρο ή ανενεργό API key του ΓΕΜΗ (HTTP " + code + ").");
                if (code == 429)
                    throw new RegistryException("Υπέρβαση ορίου αιτημάτων του ΓΕΜΗ. Δοκιμάστε ξανά σε λίγο.");
                if (code / 100 != 2)
                    throw new RegistryException("Σφάλμα από το ΓΕΜΗ (HTTP " + code + ").");
                return toRecord(resp.body(), taxId);
            } catch (IOException e) {
                throw new RegistryException("Αδυναμία σύνδεσης με το ΓΕΜΗ: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RegistryException("Η αναζήτηση διακόπηκε.", e);
            }
        }

        /** Μετατρέπει την απάντηση JSON σε CompanyRecord (package-private ώστε να δοκιμάζεται χωρίς δίκτυο). */
        static Optional<CompanyRecord> toRecord(String body, String requestedTaxId) {
            Object json;
            try {
                json = MiniJson.parse(body);
            } catch (RuntimeException e) {
                throw new RegistryException("Μη αναγνώσιμη απάντηση από το ΓΕΜΗ.", e);
            }
            Map<String, Object> obj = MiniJson.firstObjectWithAnyKey(json, NAME_KEYS);
            if (obj == null) return Optional.empty();

            String taxId = MiniJson.find(obj, TAX_KEYS);
            return Optional.of(new CompanyRecord(
                    MiniJson.find(obj, GEMI_KEYS),
                    taxId != null ? taxId : requestedTaxId,
                    MiniJson.find(obj, NAME_KEYS),
                    MiniJson.find(obj, FORM_KEYS),
                    MiniJson.find(obj, STATUS_KEYS),
                    parseDate(MiniJson.find(obj, DATE_KEYS)),
                    MiniJson.find(obj, CITY_KEYS)));
        }

        static LocalDate parseDate(String s) {
            if (s == null || s.isBlank()) return null;
            try {
                return LocalDate.parse(s.substring(0, Math.min(10, s.length())));          // 2012-03-15 ή 2012-03-15T...
            } catch (RuntimeException ignored) {
            }
            try {
                return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy")); // 15/03/2012
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        @Override
        public String sourceName() {
            return "ΓΕΜΗ Open Data API";
        }

        private static String enc(String s) {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        }
}

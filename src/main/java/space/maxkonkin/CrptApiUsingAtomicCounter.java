package space.maxkonkin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApiUsingAtomicCounter {

    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final long intervalInMillis;
    private final int requestLimit;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .build();
    private final AtomicInteger requestCount;
    private long lastRequestTime;

    public CrptApiUsingAtomicCounter(TimeUnit timeUnit, int requestLimit) {
        this.intervalInMillis = timeUnit.toMillis(1); // Interval of 1 time unit
        this.requestLimit = requestLimit;
        this.lastRequestTime = System.currentTimeMillis();
        this.requestCount = new AtomicInteger(0);
    }

    public static void main(String[] args) {
        CrptApiUsingAtomicCounter crptApi = new CrptApiUsingAtomicCounter(TimeUnit.SECONDS, 2);
        for (int i = 0; i < 10; i++) {
            final int j = i;
            new Thread(() -> {
                try {
                    crptApi.createDocument(new CrptDocument(
                            new CrptDocument.Description("string"),
                            "" + j,
                            "string",
                            "string",
                            true,
                            "string",
                            "string",
                            "string",
                            LocalDate.now(),
                            "string",
                            List.of(new CrptDocument.Product(
                                    "string",
                                    LocalDate.now(),
                                    "string",
                                    "string",
                                    "string",
                                    LocalDate.now(),
                                    "string",
                                    "string",
                                    "string"
                            )),
                            LocalDate.now(),
                            "string"
                    ), "Signature#" + j);
                } catch (InterruptedException | IOException e) {
                    System.out.println(e.getMessage());
                }
            }).start();
        }
    }

    public synchronized void createDocument(CrptDocument document, String signature) throws InterruptedException, IOException {
        long currentTime = System.currentTimeMillis();

        // Check if the interval has elapsed since the last request
        if (currentTime - lastRequestTime >= intervalInMillis) {
            // If so, reset request count and update last request time
            requestCount.set(0);
            lastRequestTime = currentTime;
        }

        // Check if the request limit has been reached
        if (requestCount.get() >= requestLimit) {
            // If so, wait until the end of the interval
            try {
                long timeElapsed = currentTime - lastRequestTime;
                long timeRemaining = intervalInMillis - timeElapsed;
                TimeUnit.MILLISECONDS.sleep(timeRemaining);
                // After sleeping, update last request time and reset request count
                lastRequestTime = System.currentTimeMillis();
                requestCount.set(0);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
        requestCount.incrementAndGet();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(document)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.printf("Document with signature %s sent.\nResponse code: %s\nResponse body: %s\n\n",
                    signature, response.statusCode(), response.body());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public record CrptDocument(
            @JsonProperty("description")
            Description description,
            @JsonProperty("doc_id")
            String docId,
            @JsonProperty("doc_status")
            String docStatus,
            @JsonProperty("doc_type")
            String docType,
            @JsonProperty("importRequest")
            Boolean importRequest,
            @JsonProperty("owner_inn")
            String ownerInn,
            @JsonProperty("participant_inn")
            String participantInn,
            @JsonProperty("producer_inn")
            String producerInn,
            @JsonProperty("production_date")
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate productionDate,
            @JsonProperty("production_type")
            String productionType,
            @JsonProperty("products")
            List<Product> products,
            @JsonProperty("reg_date")
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate regDate,
            @JsonProperty("reg_number")
            String regNumber) {
        private record Description(@JsonProperty("participantInn") String participantInn) {
        }

        private record Product(
                @JsonProperty("certificate_document")
                String certificateDocument,
                @JsonProperty("certificate_document_date")
                @JsonFormat(pattern = "yyyy-MM-dd")
                LocalDate certificateDocumentDate,
                @JsonProperty("certificate_document_number")
                String certificateDocumentNumber,
                @JsonProperty("owner_inn")
                String ownerInn,
                @JsonProperty("producer_inn")
                String producerInn,
                @JsonProperty("production_date")
                @JsonFormat(pattern = "yyyy-MM-dd")
                LocalDate productionDate,
                @JsonProperty("tnved_code")
                String tnvedCode,
                @JsonProperty("uit_code")
                String uitCode,
                @JsonProperty("uitu_code")
                String uituCode) {
        }
    }

}

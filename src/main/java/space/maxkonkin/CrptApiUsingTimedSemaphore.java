package space.maxkonkin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.lang3.concurrent.TimedSemaphore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CrptApiUsingTimedSemaphore {

    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final TimedSemaphore semaphore;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    public CrptApiUsingTimedSemaphore(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new TimedSemaphore(1, timeUnit, requestLimit);
    }

    public static void main(String[] args) {
        CrptApiUsingTimedSemaphore crptApi = new CrptApiUsingTimedSemaphore(TimeUnit.SECONDS, 2);
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
        semaphore.acquire();

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

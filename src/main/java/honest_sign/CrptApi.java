package honest_sign;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final String API_URI = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Gson gson = new Gson();
    private final Semaphore semaphore;
    private final HttpClient httpClient;
    private final AtomicInteger counter = new AtomicInteger(0);

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClient.newHttpClient();
        scheduleSemaphoreRelease(timeUnit);
    }

    public static void main(String[] args) {
        int limit = 3;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        CrptApi crptApi = new CrptApi(timeUnit, limit);

        Document.Description description = new Document.Description("1234567890");

        Document.Product product = new Document.Product(
                "certificate123",
                new Date(),
                "certificate123",
                "ownerInn123",
                "producerInn123",
                new Date(),
                "tnved123",
                "uit123",
                "uitu123"
        );
        List<Document.Product> products = new ArrayList<>();
        products.add(product);
        Document document = new Document(description,
                "doc123",
                "status123",
                "type123",
                true,
                "ownerInn123",
                "participantInn123",
                "producerInn123",
                new Date(),
                "type123",
                products,
                new Date(),
                "number123");
        String signature = "Signature";
        crptApi.createDocument(document, signature, limit, timeUnit);

    }

    private void scheduleSemaphoreRelease(TimeUnit timeUnit) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        scheduledExecutorService.scheduleAtFixedRate(this::resetCounter,
                1,
                1,
                timeUnit);
    }

    private void resetCounter() {
        counter.set(0);
    }

    public void createDocument(Document document, String sign, int requestLimit, TimeUnit timeUnit) {
        try {
            if (semaphore.tryAcquire(requestLimit, 5, TimeUnit.SECONDS)) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                for (int i = 0; i < requestLimit; i++) {
                    scheduler.schedule(() -> {
                        try {
                            String jsonDoc = gson.toJson(document);
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(API_URI))
                                    .header("Content-Type", "application/json")
                                    .header("Signature", sign)
                                    .POST(HttpRequest.BodyPublishers.ofString(jsonDoc))
                                    .build();
                            CompletableFuture<Integer> responseFuture = sendRequest(request);
                            int statusCode = responseFuture.get();
                            if (statusCode >= 200 && statusCode < 300) {
                                System.out.println("Запрос успешно отправлен.");
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        } finally {
                            semaphore.release();
                        }
                    }, i, timeUnit);
                }
                scheduler.shutdown();
            } else {
                System.out.println("Недоступно");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    private CompletableFuture<Integer> sendRequest(HttpRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.format("Код отправки: %d\n", response.statusCode());
                return response.statusCode();
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }, ForkJoinPool.commonPool());
    }

    static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private Date productionDate;
        private String productionType;
        private List<Product> products;
        private Date regDate;
        private String regNumber;

        public Document(Description description, String docId, String docStatus,
                        String docType, boolean importRequest, String ownerInn,
                        String participantInn, String producerInn, Date productionDate,
                        String productionType, List<Product> products, Date regDate, String regNumber) {
            this.description = description;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.products = products;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }

        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public void setDocStatus(String docStatus) {
            this.docStatus = docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public Date getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(Date productionDate) {
            this.productionDate = productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public void setProductionType(String productionType) {
            this.productionType = productionType;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public Date getRegDate() {
            return regDate;
        }

        public void setRegDate(Date regDate) {
            this.regDate = regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public void setRegNumber(String regNumber) {
            this.regNumber = regNumber;
        }

        public static class Description {
            private String participantInn;

            public Description(String participantInn) {
                this.participantInn = participantInn;
            }

            public String getParticipantInn() {
                return participantInn;
            }

            public void setParticipantInn(String participantInn) {
                this.participantInn = participantInn;
            }
        }

        public static class Product {
            private String certificateDocument;
            private Date certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private Date productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;

            public Product(String certificateDocument, Date certificateDocumentDate, String certificateDocumentNumber,
                           String ownerInn, String producerInn, Date productionDate,
                           String tnvedCode, String uitCode, String uituCode) {
                this.certificateDocument = certificateDocument;
                this.certificateDocumentDate = certificateDocumentDate;
                this.certificateDocumentNumber = certificateDocumentNumber;
                this.ownerInn = ownerInn;
                this.producerInn = producerInn;
                this.productionDate = productionDate;
                this.tnvedCode = tnvedCode;
                this.uitCode = uitCode;
                this.uituCode = uituCode;
            }

            public String getCertificateDocument() {
                return certificateDocument;
            }

            public void setCertificateDocument(String certificateDocument) {
                this.certificateDocument = certificateDocument;
            }

            public Date getCertificateDocumentDate() {
                return certificateDocumentDate;
            }

            public void setCertificateDocumentDate(Date certificateDocumentDate) {
                this.certificateDocumentDate = certificateDocumentDate;
            }

            public String getCertificateDocumentNumber() {
                return certificateDocumentNumber;
            }

            public void setCertificateDocumentNumber(String certificateDocumentNumber) {
                this.certificateDocumentNumber = certificateDocumentNumber;
            }

            public String getOwnerInn() {
                return ownerInn;
            }

            public void setOwnerInn(String ownerInn) {
                this.ownerInn = ownerInn;
            }

            public String getProducerInn() {
                return producerInn;
            }

            public void setProducerInn(String producerInn) {
                this.producerInn = producerInn;
            }

            public Date getProductionDate() {
                return productionDate;
            }

            public void setProductionDate(Date productionDate) {
                this.productionDate = productionDate;
            }

            public String getTnvedCode() {
                return tnvedCode;
            }

            public void setTnvedCode(String tnvedCode) {
                this.tnvedCode = tnvedCode;
            }

            public String getUitCode() {
                return uitCode;
            }

            public void setUitCode(String uitCode) {
                this.uitCode = uitCode;
            }

            public String getUituCode() {
                return uituCode;
            }

            public void setUituCode(String uituCode) {
                this.uituCode = uituCode;
            }
        }
    }
}

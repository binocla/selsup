package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final Logger LOGGER = LoggerFactory.getLogger(CrptApi.class);
    private static final String DOCUMENT =
            """
                    {
                    	"description": {
                    		"participantInn": "string"
                    	},
                    	"doc_id": "string",
                    	"doc_status": "string",
                    	"doc_type": "LP_INTRODUCE_GOODS",
                    	"importRequest": true,
                    	"owner_inn": "string",
                    	"participant_inn": "string",
                    	"producer_inn": "string",
                    	"production_date": "2020-01-23",
                    	"production_type": "string",
                    	"products": [
                    		{
                    			"certificate_document": "string",
                    			"certificate_document_date": "2020-01-23",
                    			"certificate_document_number": "string",
                    			"owner_inn": "string",
                    			"producer_inn": "string",
                    			"production_date": "2020-01-23",
                    			"tnved_code": "string",
                    			"uit_code": "string",
                    			"uitu_code": "string"
                    		}
                    	],
                    	"reg_date": "2020-01-23",
                    	"reg_number": "string"
                    }
                    """;

    private final Semaphore semaphore;
    private final Client client;
    private final TimeUnit timeUnit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) throws InterruptedException, JsonProcessingException {
        this.semaphore = new Semaphore(requestLimit);
        this.client = ClientBuilder.newClient();
        this.timeUnit = timeUnit;
        var json = new ObjectMapper().readValue(DOCUMENT, new TypeReference<JsonNode>() {
        });
        createDocument(json, "Test");
    }

    /**
     * Создание документа по API Честного знака
     *
     * @param document  JSON тела запроса
     * @param signature Подпись запроса
     * @throws InterruptedException Если текущий поток прерван (interrupted)
     * @apiNote По условию не описано применение "подписи", поэтому я просто логгирую эту информацию.
     * Я бы написал тестирующий код, Mock'ирующий запросы, однако это не смогло бы уместиться в одном и данном классе - CrptApi :)
     */
    public void createDocument(JsonNode document, String signature) throws InterruptedException {
        LOGGER.debug("Document {}, signature {}", document, signature);
        semaphore.acquire();
        var target = client.target(URL);
        try (var response = target.request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(document, MediaType.APPLICATION_JSON))) {
            var result = response.readEntity(JsonNode.class);
            LOGGER.info("Response is {}", result);
        } finally {
            semaphore.release();
            if (semaphore.availablePermits() == 0) {
                LOGGER.debug("Connection is unavailable");
                timeUnit.sleep(1L);
            }
        }
    }
}

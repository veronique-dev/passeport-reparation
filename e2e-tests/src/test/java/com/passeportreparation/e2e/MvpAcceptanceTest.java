package com.passeportreparation.e2e;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'acceptation MVP via gateway.
 * Prérequis : docker compose up (API sur http://localhost:8090)
 *
 * mvn -pl e2e-tests -Pe2e test -De2e.base.url=http://localhost:8090
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MVP — Parcours Photo → Diagnostic → Réparateurs")
class MvpAcceptanceTest {

    private static String baseUrl;
    private static String mediaId;
    private static String diagnosisId;

    @BeforeAll
    static void setup() {
        baseUrl = System.getProperty("e2e.base.url", "http://localhost:8090");
    }

    @Test
    @Order(1)
    @DisplayName("US-01 — Upload photo PNG")
    void us01_uploadMedia() {
        mediaId = given()
                .baseUri(baseUrl)
                .multiPart("file", "appareil.png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, "image/png")
                .when()
                .post("/api/media")
                .then()
                .statusCode(201)
                .body("mediaId", not(emptyString()))
                .body("contentType", equalTo("image/png"))
                .extract()
                .path("mediaId");

        given()
                .baseUri(baseUrl)
                .when()
                .get("/api/media/{id}", mediaId)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(2)
    @DisplayName("US-01 — Rejette un PDF")
    void us01_rejectsUnsupportedType() {
        given()
                .baseUri(baseUrl)
                .multiPart("file", "doc.pdf", new byte[]{1, 2, 3, 4}, "application/pdf")
                .when()
                .post("/api/media")
                .then()
                .statusCode(400)
                .body("message", containsString("Type non supporté"));
    }

    @Test
    @Order(3)
    @DisplayName("US-03 — Liste des pannes pour un four")
    void us03_listOvenIssues() {
        List<Map<String, Object>> issues = given()
                .baseUri(baseUrl)
                .queryParam("category", "OVEN")
                .when()
                .get("/api/diagnoses/issues")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        assertThat(issues).isNotEmpty();
        assertThat(issues).anySatisfy(issue ->
                assertThat(issue.get("code")).isEqualTo("OV_DOOR_SEAL"));
    }

    @Test
    @Order(4)
    @DisplayName("US-04 — Hors périmètre sans estimation")
    void us04_unsupportedCategory() {
        given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "mediaId", mediaId,
                        "category", "UNSUPPORTED"
                ))
                .when()
                .post("/api/diagnoses")
                .then()
                .statusCode(201)
                .body("supported", equalTo(false))
                .body("estimate", nullValue())
                .body("verdict", nullValue())
                .body("issueCode", equalTo("UNSUPPORTED_OTHER"))
                .body("disclaimer", not(emptyString()));
    }

    @Test
    @Order(5)
    @DisplayName("US-05 / US-06 / US-07 — Estimation joint de porte four")
    void us05_us06_us07_ovenDoorSealEstimate() {
        Object extractedId = given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "mediaId", mediaId,
                        "category", "OVEN",
                        "issueCode", "OV_DOOR_SEAL"
                ))
                .when()
                .post("/api/diagnoses")
                .then()
                .statusCode(201)
                .body("supported", equalTo(true))
                .body("applianceLabel", equalTo("Four"))
                .body("probableIssue", containsString("Joint"))
                .body("estimate.repairLow", equalTo(50))
                .body("estimate.repairHigh", equalTo(120))
                .body("estimate.replacementApprox", equalTo(400))
                .body("estimate.currency", equalTo("EUR"))
                .body("verdict", equalTo("REPAIR"))
                .body("disclaimer", containsString("indicative"))
                .body("userConfirmed", equalTo(true))
                .extract()
                .path("id");

        assertThat(extractedId).isNotNull();
        diagnosisId = extractedId.toString();
    }

    @Test
    @Order(6)
    @DisplayName("US-05 — Panne carte électronique plus chère que joint")
    void us05_electronicBoardMoreExpensiveThanSeal() {
        Number boardHigh = given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "mediaId", mediaId,
                        "category", "OVEN",
                        "issueCode", "OV_ELECTRONIC_BOARD"
                ))
                .when()
                .post("/api/diagnoses")
                .then()
                .statusCode(201)
                .extract()
                .path("estimate.repairHigh");

        assertThat(boardHigh.intValue()).isGreaterThan(120);
    }

    @Test
    @Order(7)
    @DisplayName("US-10 — Relire un diagnostic par id")
    void us10_getDiagnosisById() {
        given()
                .baseUri(baseUrl)
                .when()
                .get("/api/diagnoses/{id}", diagnosisId)
                .then()
                .statusCode(200)
                .body("id", equalTo(diagnosisId))
                .body("category", equalTo("OVEN"))
                .body("issueCode", equalTo("OV_DOOR_SEAL"));
    }

    @Test
    @Order(8)
    @DisplayName("US-08 — Réparateurs Lyon filtrés par catégorie")
    void us08_listRepairersForOvenInLyon() {
        List<Map<String, Object>> repairers = given()
                .baseUri(baseUrl)
                .queryParam("category", "OVEN")
                .queryParam("city", "Lyon")
                .when()
                .get("/api/repairers")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        assertThat(repairers).isNotEmpty();
        assertThat(repairers).allSatisfy(r -> {
            assertThat(r.get("city").toString()).isEqualToIgnoringCase("Lyon");
            @SuppressWarnings("unchecked")
            List<String> categories = (List<String>) r.get("categories");
            assertThat(categories).contains("OVEN");
        });
    }

    @Test
    @Order(9)
    @DisplayName("US-09 — Fiches réparateurs avec contacts")
    void us09_repairersExposeContactChannels() {
        List<Map<String, Object>> repairers = given()
                .baseUri(baseUrl)
                .queryParam("category", "OVEN")
                .queryParam("city", "Lyon")
                .when()
                .get("/api/repairers")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        assertThat(repairers).anySatisfy(r ->
                assertThat(r.get("phone")).isNotNull());
        assertThat(repairers).anySatisfy(r ->
                assertThat(r.get("email")).isNotNull());
    }

    @Test
    @Order(10)
    @DisplayName("Parcours bout-en-bout lave-linge")
    void endToEnd_washingMachineFlow() {
        String uploadedMediaId = given()
                .baseUri(baseUrl)
                .multiPart("file", "ll.png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}, "image/png")
                .when()
                .post("/api/media")
                .then()
                .statusCode(201)
                .extract()
                .path("mediaId");

        given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "mediaId", uploadedMediaId,
                        "category", "WASHING_MACHINE",
                        "issueCode", "WM_DRAIN_PUMP"
                ))
                .when()
                .post("/api/diagnoses")
                .then()
                .statusCode(201)
                .body("supported", equalTo(true))
                .body("verdict", equalTo("REPAIR"))
                .body("estimate.repairLow", equalTo(80));

        given()
                .baseUri(baseUrl)
                .queryParam("category", "WASHING_MACHINE")
                .queryParam("city", "Lyon")
                .when()
                .get("/api/repairers")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }
}

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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * US-22 / US-12 — Auth + claim via gateway.
 * Prérequis : docker compose up (API :8090, Mailpit :8025)
 *
 * mvn -pl e2e-tests -Pe2e test -De2e.base.url=http://localhost:8090
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth — Register → Confirm → Login → Mine / Claim")
class AuthAcceptanceTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    private static String baseUrl;
    private static String mailpitUrl;
    private static String email;
    private static final String PASSWORD = "secret123";

    private static String accessToken;
    private static String refreshToken;
    private static String anonymousDiagnosisId;
    private static String mediaId;

    @BeforeAll
    static void setup() {
        baseUrl = System.getProperty("e2e.base.url", "http://localhost:8090");
        mailpitUrl = System.getProperty("e2e.mailpit.url", "http://localhost:8025");
        email = "e2e-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        given().baseUri(mailpitUrl).when().delete("/api/v1/messages").then().statusCode(anyOf(is(200), is(204)));
    }

    @Test
    @Order(1)
    @DisplayName("US-22 — Register crée le compte")
    void us22_register() {
        given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", PASSWORD,
                        "firstName", "E2E"
                ))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("message", containsString("confirm"));
    }

    @Test
    @Order(2)
    @DisplayName("US-22 — Confirm email via token Mailpit")
    void us22_confirmFromMailpit() {
        String confirmToken = waitForEmailToken(email, "Confirme");

        given()
                .baseUri(baseUrl)
                .queryParam("token", confirmToken)
                .when()
                .get("/api/auth/confirm")
                .then()
                .statusCode(200)
                .body("message", containsString("confirm"));
    }

    @Test
    @Order(3)
    @DisplayName("US-22 — Login retourne JWT + refresh")
    void us22_login() {
        Map<String, Object> tokens = given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", not(emptyString()))
                .body("refreshToken", not(emptyString()))
                .body("user.email", equalTo(email))
                .extract()
                .jsonPath()
                .getMap("$");

        accessToken = tokens.get("accessToken").toString();
        refreshToken = tokens.get("refreshToken").toString();
    }

    @Test
    @Order(4)
    @DisplayName("US-22 — Diagnostic authentifié apparaît dans /mine")
    void us22_authenticatedDiagnosisInMine() {
        mediaId = given()
                .baseUri(baseUrl)
                .multiPart("file", "auth.png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, "image/png")
                .when()
                .post("/api/media")
                .then()
                .statusCode(201)
                .extract()
                .path("mediaId");

        String diagnosisId = given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + accessToken)
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
                .body("userId", notNullValue())
                .extract()
                .path("id")
                .toString();

        List<Map<String, Object>> mine = given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/diagnoses/mine")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        assertThat(mine).anySatisfy(item -> assertThat(item.get("id").toString()).isEqualTo(diagnosisId));
    }

    @Test
    @Order(5)
    @DisplayName("US-12 — Claim d’un passeport anonyme")
    void us12_claimAnonymousPassport() {
        anonymousDiagnosisId = given()
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
                .body("userId", nullValue())
                .extract()
                .path("id")
                .toString();

        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/diagnoses/{id}/claim", anonymousDiagnosisId)
                .then()
                .statusCode(200)
                .body("id", equalTo(anonymousDiagnosisId))
                .body("userId", notNullValue());

        List<Map<String, Object>> mine = given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/diagnoses/mine")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        assertThat(mine).anySatisfy(item ->
                assertThat(item.get("id").toString()).isEqualTo(anonymousDiagnosisId));
    }

    @Test
    @Order(6)
    @DisplayName("US-12 — Claim déjà possédé reste idempotent pour le même user")
    void us12_claimIdempotent() {
        given()
                .baseUri(baseUrl)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/diagnoses/{id}/claim", anonymousDiagnosisId)
                .then()
                .statusCode(200)
                .body("id", equalTo(anonymousDiagnosisId));
    }

    @Test
    @Order(7)
    @DisplayName("US-22 — Refresh puis logout")
    void us22_refreshAndLogout() {
        Map<String, Object> rotated = given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", not(emptyString()))
                .body("refreshToken", not(emptyString()))
                .extract()
                .jsonPath()
                .getMap("$");

        String newRefresh = rotated.get("refreshToken").toString();
        assertThat(newRefresh).isNotEqualTo(refreshToken);

        given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", newRefresh))
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(200)
                .body("message", containsString("Déconnecté"));

        given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", newRefresh))
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(anyOf(is(400), is(401), is(403)));
    }

    @Test
    @Order(8)
    @DisplayName("US-22 — Forgot + reset password")
    void us22_forgotAndResetPassword() {
        given()
                .baseUri(mailpitUrl)
                .when()
                .delete("/api/v1/messages")
                .then()
                .statusCode(anyOf(is(200), is(204)));

        given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email))
                .when()
                .post("/api/auth/forgot-password")
                .then()
                .statusCode(200)
                .body("message", containsString("réinitialisation"));

        String resetToken = waitForEmailToken(email, "réinitialis");
        String newPassword = "newsecret99";

        given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("token", resetToken, "newPassword", newPassword))
                .when()
                .post("/api/auth/reset-password")
                .then()
                .statusCode(200)
                .body("message", containsString("Mot de passe"));

        given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", newPassword))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", not(emptyString()));
    }

    private static String waitForEmailToken(String toEmail, String subjectHint) {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            String token = extractTokenFromMailpit(toEmail, subjectHint);
            if (token != null && !token.isBlank()) {
                return token;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Mailpit email", e);
            }
        }
        throw new AssertionError("No Mailpit email with subject hint '" + subjectHint + "' for " + toEmail);
    }

    private static String extractTokenFromMailpit(String toEmail, String subjectHint) {
        List<Map<String, Object>> messages = given()
                .baseUri(mailpitUrl)
                .when()
                .get("/api/v1/messages")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("messages");

        if (messages == null || messages.isEmpty()) {
            return null;
        }

        for (Map<String, Object> summary : messages) {
            String subject = String.valueOf(summary.get("Subject"));
            if (subjectHint != null && !subject.toLowerCase().contains(subjectHint.toLowerCase())) {
                continue;
            }
            String id = String.valueOf(summary.get("ID"));
            String text = given()
                    .baseUri(mailpitUrl)
                    .when()
                    .get("/api/v1/message/{id}", id)
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("Text");

            if (text == null) {
                continue;
            }
            Matcher matcher = TOKEN_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}

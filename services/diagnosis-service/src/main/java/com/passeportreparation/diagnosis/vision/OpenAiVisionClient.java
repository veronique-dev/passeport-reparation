package com.passeportreparation.diagnosis.vision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.common.enums.IssueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Appel OpenAI Vision. Nécessite OPENAI_API_KEY.
 * La suggestion reste éditable côté UI — jamais source de vérité seule.
 */
@Component
@ConditionalOnProperty(name = "app.vision.provider", havingValue = "openai")
@RequiredArgsConstructor
@Slf4j
public class OpenAiVisionClient implements VisionClient {

    private static final Set<String> SUPPORTED_CATEGORIES = Set.of(
            "WASHING_MACHINE", "DISHWASHER", "OVEN", "UNSUPPORTED"
    );

    private final ObjectMapper objectMapper;

    @Value("${app.vision.openai.api-key:}")
    private String apiKey;

    @Value("${app.vision.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${app.vision.openai.model:gpt-4o-mini}")
    private String model;

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public VisionSuggestion suggest(String mediaId, byte[] imageBytes, String contentType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new VisionProviderException("OPENAI_API_KEY manquante pour VISION_PROVIDER=openai");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return unsupported("Image vide");
        }

        String mime = (contentType == null || contentType.isBlank()) ? "image/jpeg" : contentType;
        String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

        String systemPrompt = """
                Tu analyses une photo d'appareil électroménager en panne.
                Réponds UNIQUEMENT en JSON compact avec les clés:
                category (WASHING_MACHINE|DISHWASHER|OVEN|UNSUPPORTED),
                applianceLabel (string),
                suggestedIssueCode (un code parmi: WM_DRAIN_PUMP,WM_DOOR_LOCK,WM_NO_SPIN,WM_ELECTRONIC_BOARD,WM_UNKNOWN,
                DW_HEATING,DW_DRAIN,DW_SPRAY_ARM,DW_ELECTRONIC_BOARD,DW_UNKNOWN,
                OV_THERMOSTAT,OV_HEATING_ELEMENT,OV_DOOR_SEAL,OV_ELECTRONIC_BOARD,OV_UNKNOWN,UNSUPPORTED_OTHER),
                probableIssue (string courte FR),
                confidence (0..1),
                supported (boolean),
                rationale (string courte FR).
                Si l'objet n'est pas lave-linge, lave-vaisselle ou four → category=UNSUPPORTED.
                """;

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", "Identifie l'appareil et la panne probable."),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                        ))
                )
        );

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .build();

            String raw = client.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new VisionProviderException("Réponse OpenAI vide");
            }
            return parseSuggestion(content);
        } catch (VisionProviderException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OpenAI vision failed: {}", e.getMessage());
            throw new VisionProviderException("Échec appel OpenAI Vision: " + e.getMessage(), e);
        }
    }

    VisionSuggestion parseSuggestion(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        String categoryRaw = node.path("category").asText("UNSUPPORTED");
        if (!SUPPORTED_CATEGORIES.contains(categoryRaw)) {
            categoryRaw = "UNSUPPORTED";
        }
        ApplianceCategory category = ApplianceCategory.valueOf(categoryRaw);
        boolean supported = category != ApplianceCategory.UNSUPPORTED && node.path("supported").asBoolean(true);

        IssueCode issueCode;
        try {
            issueCode = IssueCode.valueOf(node.path("suggestedIssueCode").asText("UNSUPPORTED_OTHER"));
        } catch (Exception e) {
            issueCode = supported
                    ? switch (category) {
                case WASHING_MACHINE -> IssueCode.WM_UNKNOWN;
                case DISHWASHER -> IssueCode.DW_UNKNOWN;
                case OVEN -> IssueCode.OV_UNKNOWN;
                default -> IssueCode.UNSUPPORTED_OTHER;
            }
                    : IssueCode.UNSUPPORTED_OTHER;
        }

        if (supported && !issueCode.name().startsWith(prefixFor(category))) {
            issueCode = switch (category) {
                case WASHING_MACHINE -> IssueCode.WM_UNKNOWN;
                case DISHWASHER -> IssueCode.DW_UNKNOWN;
                case OVEN -> IssueCode.OV_UNKNOWN;
                default -> IssueCode.UNSUPPORTED_OTHER;
            };
        }

        double confidence = Math.min(1.0, Math.max(0.0, node.path("confidence").asDouble(0.5)));
        String label = node.path("applianceLabel").asText(defaultLabel(category));
        String issue = node.path("probableIssue").asText("Panne non identifiée");
        String rationale = node.path("rationale").asText("Suggestion OpenAI — à confirmer");

        return new VisionSuggestion(category, label, issueCode, issue, confidence, supported, rationale);
    }

    private static String prefixFor(ApplianceCategory category) {
        return switch (category) {
            case WASHING_MACHINE -> "WM_";
            case DISHWASHER -> "DW_";
            case OVEN -> "OV_";
            default -> "UNSUPPORTED";
        };
    }

    private static String defaultLabel(ApplianceCategory category) {
        return switch (category) {
            case WASHING_MACHINE -> "Lave-linge";
            case DISHWASHER -> "Lave-vaisselle";
            case OVEN -> "Four";
            default -> "Appareil hors périmètre";
        };
    }

    private static VisionSuggestion unsupported(String rationale) {
        return new VisionSuggestion(
                ApplianceCategory.UNSUPPORTED,
                "Appareil hors périmètre",
                IssueCode.UNSUPPORTED_OTHER,
                "Catégorie non supportée pour l’instant",
                0.2,
                false,
                rationale
        );
    }
}

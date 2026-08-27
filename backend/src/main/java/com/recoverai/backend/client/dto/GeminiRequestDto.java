package com.recoverai.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiRequestDto {

    private List<Content> contents;

    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;

    public GeminiRequestDto() {
    }

    public GeminiRequestDto(List<Content> contents, GenerationConfig generationConfig) {
        this.contents = contents;
        this.generationConfig = generationConfig;
    }

    public static GeminiRequestDto fromPrompt(String promptText) {
        Part part = new Part(promptText);
        Content content = new Content(Collections.singletonList(part));
        GenerationConfig config = new GenerationConfig("application/json", 0.2);
        return new GeminiRequestDto(Collections.singletonList(content), config);
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }

    public static class Content {
        private List<Part> parts;

        public Content() {
        }

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private String text;

        public Part() {
        }

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GenerationConfig {
        @JsonProperty("response_mime_type")
        private String responseMimeType;

        private Double temperature;

        public GenerationConfig() {
        }

        public GenerationConfig(String responseMimeType, Double temperature) {
            this.responseMimeType = responseMimeType;
            this.temperature = temperature;
        }

        public String getResponseMimeType() {
            return responseMimeType;
        }

        public void setResponseMimeType(String responseMimeType) {
            this.responseMimeType = responseMimeType;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
    }
}

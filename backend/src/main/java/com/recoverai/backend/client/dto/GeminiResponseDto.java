package com.recoverai.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponseDto {

    private List<Candidate> candidates;

    @JsonProperty("usageMetadata")
    private UsageMetadata usageMetadata;

    @JsonProperty("modelVersion")
    private String modelVersion;

    public GeminiResponseDto() {
    }

    public GeminiResponseDto(List<Candidate> candidates, UsageMetadata usageMetadata, String modelVersion) {
        this.candidates = candidates;
        this.usageMetadata = usageMetadata;
        this.modelVersion = modelVersion;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public UsageMetadata getUsageMetadata() {
        return usageMetadata;
    }

    public void setUsageMetadata(UsageMetadata usageMetadata) {
        this.usageMetadata = usageMetadata;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String extractFirstCandidateText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Candidate candidate = candidates.get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null || candidate.getContent().getParts().isEmpty()) {
            return null;
        }
        return candidate.getContent().getParts().get(0).getText();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
        private String finishReason;

        public Candidate() {
        }

        public Candidate(Content content, String finishReason) {
            this.content = content;
            this.finishReason = finishReason;
        }

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private List<Part> parts;
        private String role;

        public Content() {
        }

        public Content(List<Part> parts, String role) {
            this.parts = parts;
            this.role = role;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsageMetadata {
        private Integer promptTokenCount;
        private Integer candidatesTokenCount;
        private Integer totalTokenCount;

        public UsageMetadata() {
        }

        public UsageMetadata(Integer promptTokenCount, Integer candidatesTokenCount, Integer totalTokenCount) {
            this.promptTokenCount = promptTokenCount;
            this.candidatesTokenCount = candidatesTokenCount;
            this.totalTokenCount = totalTokenCount;
        }

        public Integer getPromptTokenCount() {
            return promptTokenCount;
        }

        public void setPromptTokenCount(Integer promptTokenCount) {
            this.promptTokenCount = promptTokenCount;
        }

        public Integer getCandidatesTokenCount() {
            return candidatesTokenCount;
        }

        public void setCandidatesTokenCount(Integer candidatesTokenCount) {
            this.candidatesTokenCount = candidatesTokenCount;
        }

        public Integer getTotalTokenCount() {
            return totalTokenCount;
        }

        public void setTotalTokenCount(Integer totalTokenCount) {
            this.totalTokenCount = totalTokenCount;
        }
    }
}

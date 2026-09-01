package com.Vijay.TalentIq.Model;

import java.util.List;

public class ResumeAnalysisResult {

    private Integer matchScore;
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    private String feedbackSummary;

    public ResumeAnalysisResult() {
    }

    public ResumeAnalysisResult(Integer matchScore, List<String> matchedKeywords,
                                 List<String> missingKeywords, String feedbackSummary) {
        this.matchScore = matchScore;
        this.matchedKeywords = matchedKeywords;
        this.missingKeywords = missingKeywords;
        this.feedbackSummary = feedbackSummary;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public void setMatchedKeywords(List<String> matchedKeywords) {
        this.matchedKeywords = matchedKeywords;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(List<String> missingKeywords) {
        this.missingKeywords = missingKeywords;
    }

    public String getFeedbackSummary() {
        return feedbackSummary;
    }

    public void setFeedbackSummary(String feedbackSummary) {
        this.feedbackSummary = feedbackSummary;
    }

    @Override
    public String toString() {
        return "ResumeAnalysisResult{" +
                "matchScore=" + matchScore +
                ", matchedKeywords=" + matchedKeywords +
                ", missingKeywords=" + missingKeywords +
                ", feedbackSummary='" + feedbackSummary + '\'' +
                '}';
    }
}
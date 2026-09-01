package com.Vijay.TalentIq.Controller;


import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.Vijay.TalentIq.Model.ResumeAnalysisResult;
import com.Vijay.TalentIq.service.AtsAnalyzerService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/ats")
public class AtsAnalyzerController {

    private final AtsAnalyzerService atsAnalyzerService;

    public AtsAnalyzerController(AtsAnalyzerService atsAnalyzerService) {
        this.atsAnalyzerService = atsAnalyzerService;
    }

    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResumeAnalysisResult analyze(
            @RequestPart("resume") MultipartFile resume,
            @RequestParam("jobDescription") String jobDescription)
            throws Exception {

        return atsAnalyzerService.analyzeResume(resume, jobDescription);
    }
}
package com.Vijay.TalentIq.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.Vijay.TalentIq.Model.ResumeAnalysisResult;

import tools.jackson.databind.ObjectMapper;

@Service
public class AtsAnalyzerService {

	private final ChatClient chatClient;
	private final PdfReaderService pdfReaderService;
	private final ObjectMapper objectMapper;

	public AtsAnalyzerService(ChatClient.Builder chatClientBuilder, PdfReaderService pdfReaderService,
			ObjectMapper objectMapper) {

		this.chatClient = chatClientBuilder.build();
		this.pdfReaderService = pdfReaderService;
		this.objectMapper = objectMapper;
	}

	public ResumeAnalysisResult analyzeResume(MultipartFile resume, String jobDescription) throws Exception {

		// 1. Extract text from PDF
		String resumeText = pdfReaderService.extractText(resume);

		if (resumeText == null || resumeText.isBlank()) {
			throw new RuntimeException("Could not extract text from PDF.");
		}

		// Limit resume size
		if (resumeText.length() > 10000) {
			resumeText = resumeText.substring(0, 10000);
		}

		// Limit job description size
		if (jobDescription == null || jobDescription.isBlank()) {
			throw new RuntimeException("Job description cannot be empty.");
		}

		if (jobDescription.length() > 8000) {
			jobDescription = jobDescription.substring(0, 8000);
		}

		// 2. Create prompt
		String prompt = """
				You are an intelligent ATS resume analyzer.

				Your task is to analyze ONLY the provided resume against ONLY the provided
				job description.

				RESUME:
				%s

				JOB DESCRIPTION:
				%s

				IMPORTANT ANALYSIS RULES:

				1. Extract the important skills, technologies, tools, qualifications,
				   and requirements directly from THIS JOB DESCRIPTION.

				2. Compare those requirements against THIS RESUME.

				3. matchedKeywords:
				   Include only important skills, technologies, tools, qualifications,
				   or requirements that are present in BOTH the job description and
				   the resume.

				4. missingKeywords:
				   Include only important skills, technologies, tools, qualifications,
				   or requirements that are required by THIS JOB DESCRIPTION but are
				   not found in the resume.

				5. NEVER use a predefined or hardcoded keyword list.

				6. NEVER add Python, Java, React, AWS, Docker, GenAI, SQL, or any other
				   technology unless it is actually required or clearly mentioned in
				   THIS JOB DESCRIPTION.

				7. Do not copy keywords from examples or previous requests.

				8. The result must change when the job description changes.

				9. matchScore must represent how well THIS RESUME matches THIS JOB
				   DESCRIPTION, from 0 to 100.

				10. feedbackSummary must be based only on the actual comparison between
				    THIS RESUME and THIS JOB DESCRIPTION.

				11. If the job description mentions only JavaScript, do not add Java,
				    Spring Boot, SQL, React, Python, or any other technology.

				12. If the resume contains JavaScript and the job description requires
				    JavaScript, JavaScript should be included in matchedKeywords.

				13. If the job description requires Java, SQL and Spring Boot but the
				    resume contains only Java, then SQL and Spring Boot should appear
				    in missingKeywords.

				Return ONLY ONE complete valid JSON object.

				Use exactly this JSON structure:

				{
				  "matchScore": 0,
				  "matchedKeywords": [],
				  "missingKeywords": [],
				  "feedbackSummary": ""
				}

				JSON RULES:

				- matchScore must be an integer from 0 to 100.
				- matchedKeywords must be an array of strings.
				- missingKeywords must be an array of strings.
				- feedbackSummary must be a string.
				- Return ONLY JSON.
				- Do NOT return Markdown.
				- Do NOT return ```json.
				- Do NOT add explanations.
				- Do NOT add text before JSON.
				- Do NOT add text after JSON.
				- Make sure the JSON starts with {.
				- Make sure the JSON ends with }.
				- Make sure every { has a matching }.
				- Make sure every [ has a matching ].
				- Make sure the JSON is completely closed.
				""".formatted(resumeText, jobDescription);

		// 3. Call Gemini AI through Spring AI
		String aiResponse = chatClient.prompt().user(prompt).call().content();

		// 4. Debug response
		System.out.println("=================================");
		System.out.println("AI RESPONSE:");
		System.out.println(aiResponse);
		System.out.println("=================================");

		if (aiResponse == null || aiResponse.isBlank()) {
			throw new RuntimeException("Gemini AI returned an empty response.");
		}

		// 5. Remove Markdown if Gemini AI adds it
		aiResponse = aiResponse.replace("```json", "").replace("```", "").trim();

		// 6. Find JSON object
		int start = aiResponse.indexOf("{");

		if (start == -1) {
			throw new RuntimeException("Gemini AI did not return a JSON object.\nAI Response:\n" + aiResponse);
		}

		// Keep everything from the first {
		aiResponse = aiResponse.substring(start).trim();

		// Remove accidental final ] if Gemini AI closes the object incorrectly
		if (aiResponse.endsWith("]")) {
			aiResponse = aiResponse.substring(0, aiResponse.length() - 1).trim();
		}

		// Add missing closing } if necessary
		if (!aiResponse.endsWith("}")) {
			aiResponse = aiResponse + "}";
		}

		System.out.println("========== CLEAN JSON ==========");
		System.out.println(aiResponse);
		System.out.println("================================");

		// 7. Convert JSON → Java object
		try {

			ResumeAnalysisResult result = objectMapper.readValue(aiResponse, ResumeAnalysisResult.class);

			// Debug: confirm the object is actually populated before it goes back
			// to the controller. Thanks to Lombok's @Data this prints all fields.
			System.out.println("========== PARSED RESULT ==========");
			System.out.println(result);
			System.out.println("====================================");

			return result;

		} catch (Exception e) {

			throw new RuntimeException(
					"Gemini AI JSON could not be converted to ResumeAnalysisResult." + "\nClean JSON:\n" + aiResponse, e);

		}
	}
}
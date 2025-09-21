import { onCall, HttpsError } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";
import OpenAI from "openai";

interface SummarizeEntryRequest {
  text: string;
}

interface SummarizeEntryResponse {
  summary: string;
}

/**
 * Cloud Function to generate AI summaries of journal entries
 * Only authenticated users can call this function
 */
export const summarizeEntry = onCall<SummarizeEntryRequest, Promise<SummarizeEntryResponse>>(
  {
    region: "australia-southeast1",
    secrets: ["OPENAI_API_KEY"],
  },
  async (request) => {
    // Check if user is authenticated
    if (!request.auth) {
      logger.warn("Unauthenticated request to summarizeEntry");
      throw new HttpsError(
        "unauthenticated",
        "User must be authenticated to use this function"
      );
    }

    // Validate input
    const { text } = request.data;

    if (!text || typeof text !== "string" || text.trim().length === 0) {
      logger.warn("Invalid input to summarizeEntry", { userId: request.auth.uid });
      throw new HttpsError(
        "invalid-argument",
        "Journal entry text is required and must be a non-empty string"
      );
    }

    // Check text length (OpenAI has token limits)
    if (text.length > 8000) {
      logger.warn("Text too long for summarizeEntry", {
        userId: request.auth.uid,
        textLength: text.length
      });
      throw new HttpsError(
        "invalid-argument",
        "Journal entry text is too long. Maximum 8000 characters allowed."
      );
    }

    try {
      logger.info("Generating summary for user", {
        userId: request.auth.uid,
        textLength: text.length
      });

      const openai = new OpenAI({
        apiKey: process.env.OPENAI_API_KEY,
      });

      const completion = await openai.chat.completions.create({
        model: "gpt-4o-mini",
        messages: [
          {
            role: "system",
            content: `You are an AI assistant that creates thoughtful, concise summaries of personal journal entries.
                     Your summaries should:
                     - Be 2-3 sentences long
                     - Capture the main themes, emotions, and insights
                     - Be supportive and non-judgmental in tone
                     - Help the user reflect on their thoughts
                     - Use warm, empathetic language`
          },
          {
            role: "user",
            content: `Please summarize this journal entry in 2-3 sentences:\n\n${text}`
          }
        ],
        max_tokens: 150,
        temperature: 0.7,
      });

      const summary = completion.choices[0]?.message?.content?.trim();

      if (!summary) {
        logger.error("OpenAI returned empty summary", { userId: request.auth.uid });
        throw new HttpsError(
          "internal",
          "Failed to generate summary. Please try again."
        );
      }

      logger.info("Successfully generated summary", {
        userId: request.auth.uid,
        summaryLength: summary.length
      });

      return { summary };

    } catch (error: any) {
      logger.error("Error in summarizeEntry function", {
        userId: request.auth.uid,
        error: error.message,
        stack: error.stack
      });

      // Handle OpenAI specific errors
      if (error.status === 429) {
        throw new HttpsError(
          "resource-exhausted",
          "AI service is currently busy. Please try again in a moment."
        );
      }

      if (error.status === 401 || error.status === 403) {
        throw new HttpsError(
          "internal",
          "AI service configuration error. Please contact support."
        );
      }

      // Generic error for unexpected issues
      throw new HttpsError(
        "internal",
        "An unexpected error occurred while generating the summary. Please try again."
      );
    }
  }
);
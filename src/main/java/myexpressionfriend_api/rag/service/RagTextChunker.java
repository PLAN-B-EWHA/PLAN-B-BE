package myexpressionfriend_api.rag.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RagTextChunker {

    private static final int DEFAULT_CHUNK_SIZE = 1_200;
    private static final int DEFAULT_CHUNK_OVERLAP = 200;
    private static final int MIN_NATURAL_SPLIT_DISTANCE = 200;

    public List<String> split(String content) {
        return split(content, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    public List<String> split(String content, int chunkSize, int overlap) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive.");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap must be between 0 and chunk size.");
        }

        String normalized = content.replace("\r\n", "\n").trim();
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            int splitAt = findSplitPoint(normalized, start, end);
            String chunk = normalized.substring(start, splitAt).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (splitAt == normalized.length()) {
                break;
            }
            start = Math.max(splitAt - overlap, start + 1);
        }

        return chunks;
    }

    private int findSplitPoint(String text, int start, int hardEnd) {
        if (hardEnd == text.length()) {
            return hardEnd;
        }

        int paragraphBreak = text.lastIndexOf("\n\n", hardEnd);
        if (isNaturalSplit(start, paragraphBreak)) {
            return paragraphBreak;
        }

        int sentenceBreak = Math.max(text.lastIndexOf(". ", hardEnd), text.lastIndexOf("? ", hardEnd));
        sentenceBreak = Math.max(sentenceBreak, text.lastIndexOf("! ", hardEnd));
        sentenceBreak = Math.max(sentenceBreak, text.lastIndexOf("다. ", hardEnd));
        sentenceBreak = Math.max(sentenceBreak, text.lastIndexOf("요. ", hardEnd));
        if (isNaturalSplit(start, sentenceBreak)) {
            return sentenceBreak + 1;
        }

        int lineBreak = text.lastIndexOf('\n', hardEnd);
        if (isNaturalSplit(start, lineBreak)) {
            return lineBreak;
        }

        return hardEnd;
    }

    private boolean isNaturalSplit(int start, int splitAt) {
        return splitAt > start + MIN_NATURAL_SPLIT_DISTANCE;
    }
}

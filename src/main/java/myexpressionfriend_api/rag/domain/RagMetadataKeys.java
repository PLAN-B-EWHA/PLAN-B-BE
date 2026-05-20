package myexpressionfriend_api.rag.domain;

public final class RagMetadataKeys {

    public static final String SOURCE_TYPE = "sourceType";
    public static final String USE_CASE = "useCase";
    public static final String CHILD_ID = "childId";
    public static final String USER_ID = "userId";
    public static final String DOCUMENT_ID = "documentId";
    public static final String SOURCE_ID = "sourceId";
    public static final String CREATED_AT = "createdAt";
    public static final String CHUNK_INDEX = "chunkIndex";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String ORIGINAL_FILENAME = "originalFilename";

    /**
     * Empty childId means globally reusable context, such as therapy guidelines.
     */
    public static final String GLOBAL_CHILD_ID = "";

    private RagMetadataKeys() {
    }
}

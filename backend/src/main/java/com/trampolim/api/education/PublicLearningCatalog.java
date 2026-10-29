package com.trampolim.api.education;

/** Public integration contract. It deliberately exposes no connection or student records. */
public interface PublicLearningCatalog {
    record Summary(Long publishedContents) {}
    Summary summary();
}

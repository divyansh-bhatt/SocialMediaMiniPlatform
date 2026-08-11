package com.connectsphere.search.repository;

import com.connectsphere.search.entity.PostDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {

    /**
     * Full-text search on the content field.
     * Uses Elasticsearch match query — handles partial words, stemming etc.
     * Only PUBLIC posts are returned.
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"content\": \"?0\"}}], \"filter\": [{\"term\": {\"visibility\": \"PUBLIC\"}}]}}")
    List<PostDocument> searchByContent(String keyword);

    /**
     * Find all posts that contain a specific hashtag.
     * Uses term query on the keyword-mapped hashtags field.
     */
    @Query("{\"bool\": {\"filter\": [{\"term\": {\"hashtags\": \"?0\"}}, {\"term\": {\"visibility\": \"PUBLIC\"}}]}}")
    List<PostDocument> findByHashtag(String tag);

    /** All posts by a specific author in the index */
    List<PostDocument> findByAuthorId(int authorId);
}

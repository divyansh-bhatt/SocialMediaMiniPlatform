package com.connectsphere.search.repository;

import com.connectsphere.search.entity.UserDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSearchRepository extends ElasticsearchRepository<UserDocument, String> {

    /**
     * Search users by username OR fullName.
     * Uses multi_match query — searches both fields simultaneously.
     */
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"username\", \"fullName\"], \"type\": \"best_fields\", \"fuzziness\": \"AUTO\"}}")
    List<UserDocument> searchByUsernameOrFullName(String query);
}

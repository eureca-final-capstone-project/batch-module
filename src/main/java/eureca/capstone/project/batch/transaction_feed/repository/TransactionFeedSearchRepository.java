package eureca.capstone.project.batch.transaction_feed.repository;

import eureca.capstone.project.batch.transaction_feed.document.TransactionFeedDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TransactionFeedSearchRepository extends ElasticsearchRepository<TransactionFeedDocument, Long> {
}

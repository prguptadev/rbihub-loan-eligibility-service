package in.rbihub.lending.repository;

import in.rbihub.lending.domain.Decision;

import java.util.Optional;
import java.util.UUID;


public interface DecisionRepository {

    void save(Decision decision);

    Optional<Decision> findById(UUID applicationId);
}

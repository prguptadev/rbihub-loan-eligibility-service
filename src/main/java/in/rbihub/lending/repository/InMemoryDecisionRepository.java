package in.rbihub.lending.repository;

import in.rbihub.lending.domain.Decision;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Repository
public class InMemoryDecisionRepository implements DecisionRepository {

    private final ConcurrentHashMap<UUID, Decision> decisions = new ConcurrentHashMap<>();

    @Override
    public void save(Decision decision) {
        decisions.put(decision.applicationId(), decision);
    }

    @Override
    public Optional<Decision> findById(UUID applicationId) {
        return Optional.ofNullable(decisions.get(applicationId));
    }
}

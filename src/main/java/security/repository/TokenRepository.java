package security.repository;

import org.springframework.data.repository.CrudRepository;
import security.dto.Token;

public interface TokenRepository extends CrudRepository<Token, Long> {
}
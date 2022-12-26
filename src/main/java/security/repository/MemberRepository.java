package security.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import security.dto.Member;

import java.util.Optional;

@Transactional
public interface MemberRepository extends JpaRepository<Member, Long> {


    Optional<Member> findByEmail(String email);


}

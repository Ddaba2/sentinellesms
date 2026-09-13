package com.sentinellesms.repository;

import com.sentinellesms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long countByEnabledTrue();

    @Query("select u from User u where lower(u.username) like lower(concat('%', :q, '%')) "
            + "or lower(u.email) like lower(concat('%', :q, '%'))")
    List<User> search(@Param("q") String query);
}

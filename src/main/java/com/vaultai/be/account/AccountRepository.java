package com.vaultai.be.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    Optional<Account> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerId(UUID ownerId);

    boolean existsByIban(String iban);

    /** Only the id, so the entity is not loaded before it is locked. */
    @Query("select a.id from Account a where a.iban = :iban")
    Optional<UUID> findIdByIban(@Param("iban") String iban);
}
package com.vaultai.be.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {

    List<AccountTransaction> findByAccount_IdOrderByBookedAtDesc(UUID accountId, Pageable pageable);

    /** Returns null when there is no transaction. */
    @Query("""
            select sum(t.amount) from AccountTransaction t
            where t.account.ownerId = :ownerId
              and t.type = :type
              and t.bookedAt >= :since
            """)
    BigDecimal sumAmountByOwnerAndTypeSince(@Param("ownerId") UUID ownerId,
                                            @Param("type") TransactionType type,
                                            @Param("since") Instant since);
}
/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import models.entities.Bid;
import models.entities.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, UUID> {

	@Query("""
			select distinct b.itemId
			from Bid b
			where b.expiresAt <= CURRENT_TIMESTAMP
			""")
	List<UUID> findItemIdsWithExpiredBids();

	@Query("""
			select b
			from Bid b
			where b.itemId = :itemId
			and b.expiresAt > CURRENT_TIMESTAMP
			and b.status in (models.entities.BidStatus.ACTIVE, models.entities.BidStatus.OUTBID)
			order by b.amount desc
			""")
	List<Bid> findUnexpiredBids(@Param("itemId") UUID itemId);

	@Query("""
			select b
			from Bid b
			where b.bidderId = :bidderId
			order by b.placedAt
			""")
	List<Bid> findBidsByBidderId(@Param("bidderId") UUID bidderId);

	List<Bid> findByItemId(UUID itemId);

	Optional<Bid> findByItemIdAndStatus(UUID itemId, BidStatus status);

	boolean existsByItemIdAndStatus(UUID itemId, BidStatus status);
}

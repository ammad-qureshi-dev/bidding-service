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
			select b
			from Bid b
			where b.expiresAt <= CURRENT_TIMESTAMP
			""")
	List<Bid> findItemIdsWithExpiredBids();

	@Query("""
			select b
			from Bid b
			where b.itemId = :itemId
			and b.expiresAt > CURRENT_TIMESTAMP
			and b.status in (models.entities.BidStatus.ACTIVE, models.entities.BidStatus.OUTBID)
			order by b.amount desc
			""")
	List<Bid> findMaxValidBidByStatus(@Param("itemId") UUID itemId);

	@Query("""
			select b
			from Bid b
			where b.bidderId = :bidderId
			order by b.placedAt
			""")
	List<Bid> findBidsByBidderId(@Param("bidderId") UUID bidderId);

	Optional<Bid> findByItemIdAndStatus(UUID itemId, BidStatus status);

	List<Bid> findByItemId(UUID itemId);

	@Query("""
			select b
			from Bid b
			where b.itemId = :itemId
			order by
				case b.status
					when models.entities.BidStatus.WINNER then 0
					when models.entities.BidStatus.ACTIVE then 1
					when models.entities.BidStatus.PENDING_VALIDATION then 2
					when models.entities.BidStatus.REJECTED then 3
					when models.entities.BidStatus.OUTBID then 4
					when models.entities.BidStatus.WITHDRAWN then 5
					else 6
				end,
				b.amount desc
			limit 10
			""")
	List<Bid> findTopTenBids(@Param("itemId") UUID itemId);

	boolean existsByItemIdAndStatus(UUID itemId, BidStatus status);
}

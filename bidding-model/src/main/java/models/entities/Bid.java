package models.entities;/* (C) 2026
bidder.app */

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "bidding_service", name = "bid")
@EqualsAndHashCode(callSuper = true)
public class Bid extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	// References AppUser.id owned by identity-auth-service; no cross-service JPA relation
	@NotNull
	@Column(name = "bidder_id")
	private UUID bidderId;

	// References Item.id and Auction.id owned by catalog-service; no cross-service JPA relation
	@NotNull
	@Column(name = "item_id")
	private UUID itemId;

	@NotNull
	@Column(name = "auction_id")
	private UUID auctionId;

	@NotNull @DecimalMin("0.0")
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	private BidStatus status;

	private String statusDescription;

	@Builder.Default
	private LocalDateTime placedAt = LocalDateTime.now();

	// Defaults to auction end time
	private LocalDateTime expiresAt;
}

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
@ToString(exclude = {"item", "bidder"})
public class Bid extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "bidder")
//	private AppUser bidder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item")
	private Item item;

	@NotNull @DecimalMin("0.0")
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	private BidStatus status;

	private String statusDescription;

	// Field populated from the Client
	private LocalDateTime placedAt;

	// Defaults to auction end time
	private LocalDateTime expiresAt;
}

package fr.lpr.membership.service.sale;

import com.google.common.collect.Lists;
import fr.lpr.membership.domain.sale.QSale;
import fr.lpr.membership.domain.sale.Sale;
import fr.lpr.membership.domain.sale.SoldItem;
import fr.lpr.membership.repository.sale.SaleRepository;
import fr.lpr.membership.service.sale.event.SaleCreatedEvent;
import fr.lpr.membership.service.sale.event.SaleDeletedEvent;
import fr.lpr.membership.service.sale.event.SaleUpdatedEvent;
import fr.lpr.membership.service.stock.StockQuantityChangedEvent;
import fr.lpr.membership.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SaleService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SaleService.class);

	@Autowired
	private SaleRepository saleRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	/**
	 * Save a new sale.
	 */
	public Sale newSale(Sale sale) {
		Sale savedSale = save(sale);

		LOGGER.info("Enregistrement d'une vente pour {} : \n\t\t{}", sale.getAdherent().getFullName(),
				sale.getSoldItems().stream().map(item -> item.getQuantity() + " * " + item.getArticle().getName()).collect(Collectors.joining(", \n\t\t")));

		return savedSale;
	}

	/**
	 * Update a sale.
	 */
	public Sale update(Sale sale) {
		Sale existingSale = saleRepository.getOne(sale.getId());

		// TODO Code à placer dans l'entité Sale ?
		for (SoldItem item : sale.getSoldItems()) {
			// Add new sold item to the sale
			if (item.getId() == null) {
				existingSale.getSoldItems().add(item);
				eventPublisher.publishEvent(StockQuantityChangedEvent.fromSale(item.getArticle(), item.getQuantity()));
			} else {
				// FIXME Changer le type d'exception
				SoldItem existingItem = existingSale.getSoldItems().stream()
						.filter(i -> i.getId().equals(item.getId()))
						.findFirst()
						.orElseThrow(RuntimeException::new);

				if (existingItem.getPrice() != item.getPrice()) {
					existingSale.addSoldItem(item.getArticle(), item.getQuantity(), item.getPrice());
					eventPublisher.publishEvent(StockQuantityChangedEvent.fromSale(item.getArticle(), item.getQuantity()));
				} else {
					int quantityDiff = existingItem.changeQuantity(item.getQuantity());
					eventPublisher.publishEvent(StockQuantityChangedEvent.fromSale(item.getArticle(), quantityDiff));
				}
			}
		}

		existingSale.setAdherent(sale.getAdherent());
		existingSale.setFinished(sale.isFinished());
		existingSale.setPaymentType(sale.getPaymentType());

		eventPublisher.publishEvent(new SaleUpdatedEvent(existingSale));
		return saleRepository.save(existingSale);
	}

	private Sale save(Sale sale) {
		Sale savedSale = saleRepository.save(sale);
		eventPublisher.publishEvent(new SaleCreatedEvent(savedSale));
		return savedSale;
	}

	public Page<Sale> history(Integer offset, Integer limit) {
		final Sort sort = Sort.by(Direction.DESC, "createdAt");
		final Pageable pageRequest = PaginationUtil.generatePageRequest(offset, limit, sort);

		return saleRepository.findAll(QSale.sale.finished.isTrue(), pageRequest);
	}

	public List<Sale> getTemporarySales() {
		return Lists.newArrayList(saleRepository.findAll(QSale.sale.finished.isFalse()));
	}

	public void delete(Long saleId) {
		Sale sale = saleRepository.getOne(saleId);
		saleRepository.delete(sale);
		eventPublisher.publishEvent(new SaleDeletedEvent(sale));
	}
}

package fr.lpr.membership.service.sale.event;

import fr.lpr.membership.domain.sale.Sale;

public class SaleSavedEvent extends AbstractSaleEvent {

	public SaleSavedEvent(Sale sale) {
		super(sale);
	}

}

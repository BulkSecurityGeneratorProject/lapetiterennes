'use strict'

angular.module('membershipApp')
	.factory('Basket', function () {
		
		/**
		 * Constructor
		 */
		function Basket() {
			this.date = new Date();
			this.items = [];
			this.paymentType = null;
			this.finished = false;
		}

		Basket.prototype.addItem = function(article, quantity, price) {
			var basketItem = this.getBasketItem(article.id);
			
			// If item does not already exists, add it
			if (basketItem === null) {
				this.items.push({
					articleId: article.id,
					name: article.name,
					quantity: quantity,
					price: price
				});
			} 
			// Otherwise, update the quantity
			else {
				basketItem.price = price;
				basketItem.quantity += quantity;
			}
		};
		
		Basket.fromJson = function(jsonObject) {
			var basket = new Basket();
			for (var prop in jsonObject) {
				basket[prop] = jsonObject[prop];
				if (prop == 'date') {
					basket[prop] = new Date(basket[prop]);
				}
			}
			return basket;
		}
		
		Basket.prototype.removeItem = function(item) {
			var index = this.items.indexOf(item);
			if (index > -1) {
				this.items.splice(index, 1);
			}
		};
		
		Basket.prototype.setAdherent = function(adherent) {
			this.adherentId = adherent.id;
			this.adherentFullName = adherent.prenom + ' ' + adherent.nom;
		};
		
		Basket.prototype.increment = function(item) {
			item.quantity += 1;
		};
		
		Basket.prototype.decrement = function(item) {
			if (item.quantity === 1) {
				this.removeItem(item);
			} else {
				item.quantity -= 1;
			}
		};
		
		Basket.prototype.totalCost = function() {
			var totalCost = 0;
			
			for (var index in this.items) {
				var item = this.items[index];
				totalCost += item.price * item.quantity;
			};
			
			return totalCost;
		};
		
		Basket.prototype.getBasketItem = function(articleId) {
			for (var index in this.items) {
				var basketItem = this.items[index];
				if (basketItem.articleId === articleId) {
					return basketItem;
				}
			}
			return null;
		}
		
		Basket.prototype.finished = function(finished) {
			this.finished = true;
		}
		
		return Basket;
		
	});
'use strict'

angular.module('membershipApp')
	.controller('SaleStatisticsController', function ($scope, $http, Sale) {
		$scope.salesByMonth = {};
		$scope.salesAmountByMonth = [];
		
		$scope.loadAll = function() {
			Sale.statistics(function(result) {
				$scope.salesByMonth = result.itemsByPeriod;
				computeSalesAmountByMonth();
			});
		};
		
		var computeSalesAmountByMonth = function() {
			$scope.salesAmountByMonth = [];
			var total = 0;

			for (var month in $scope.salesByMonth) {
				var amount = $scope.salesByMonth[month].reduce(function(total, sale) { return total + sale.totalPrice }, 0);
				$scope.salesAmountByMonth.push({
					month: new Date(month),
					amount: toEuros(amount)
				});
				total += amount;
			}
			
			$scope.total = toEuros(total);
		};
		
		var toEuros = function(price) {
			return parseInt(price / 100) + "," + ('0' + price % 100).slice(-2);
		};
		
		$scope.exportDetails = function () {
        	$http({
                url: 'api/sales/export',
                method: "POST",
                headers: {'Content-type': 'application/json'},
                responseType: 'arraybuffer'
                })
        		.success(function(result, status, headers) {
        			downloadFile(new Blob([result], { type: headers('Content-Type')} ));
        		}
        	);
        };
        
        // Functions
        function downloadFile(data) {
        	var url = window.URL || window.webkitURL;
        	var uri = url.createObjectURL(data);
            
            // Now the little tricky part.
            // you can use either>> window.open(uri);
            // but this will not work in some browsers
            // or you will not gAet the correct file extension    
            
            //this trick will generate a temp <a /> tag
            var link = document.createElement("a");    
            link.href = uri;
            
            //set the visibility hidden so it will not effect on your web-layout
            link.style.visibility = "hidden";
            link.download = "Statistiques.xlsx";
            
            //this part will append the anchor tag and remove it after automatic click
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            
            //revoke the object from URL
            url.revokeObjectURL(data);
        }
		
		$scope.loadAll();
	});

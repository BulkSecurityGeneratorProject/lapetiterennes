'use strict';

angular.module('membershipApp')
    .controller('AdherentDetailController', function ($scope, $stateParams, Adherent, Coordonnees, Adhesion) {
    	// View Model
        $scope.adherent = {};
        $scope.adhesionToEdit = {};
        $scope.adhesionToDelete = {};
        
        // View functions
        $scope.load = function (id) {
            Adherent.get({id: id}, function(result) {
              $scope.adherent = result;
              loadAdhesions(id);
            });
        };
        
        $scope.update = function (adhesion) {
        	Adhesion.get({id: adhesion.id}, function(result) {
	        	$scope.adhesionToEdit = result;
	        	$('#saveAdhesionModal').modal('show');
        	});
        };
        
        $scope.confirmUpdate = function (adhesion) {
        	Adhesion.update($scope.adhesionToEdit,
        		function () {
        			loadAdhesions($scope.adherent.id);
        			$('#saveAdhesionModal').modal('hide');
        			$scope.clear();
        		});
        }
        
        $scope.delete = function (adhesion) {
        	$scope.adhesionToDelete = adhesion;
        	$('#deleteAdhesionConfirmation').modal('show');
        };
        
        $scope.confirmDelete = function (adhesionId) {
        	Adhesion.delete({id: adhesionId},
        		function () {
        			loadAdhesions($scope.adherent.id);
        			$('#deleteAdhesionConfirmation').modal('hide');
        			$scope.clear();
        		});
        };
        
        $scope.clear = function () {
        	$scope.adhesionToEdit = {};
        	$scope.adhesionToDelete = {};
        }
        
        $scope.load($stateParams.id);
        
        // Functions
        function loadAdhesions(adherentId) {
        	Adhesion.adherent({adherentId: adherentId}, function(result) {
        		$scope.adherent.adhesions = result;
        	});
        }
    });

'use strict';

angular.module('membershipApp')
    .controller('AdherentController', function ($scope, Adherent, Coordonnees, Adhesion, ParseLinks) {
        $scope.adherents = [];
        $scope.coordonneess = Coordonnees.query();
        $scope.adhesions = Adhesion.query();
        $scope.page = 1;
        $scope.loadAll = function() {
            Adherent.query({page: $scope.page, per_page: 20}, function(result, headers) {
                $scope.links = ParseLinks.parse(headers('link'));
                for (var i = 0; i < result.length; i++) {
                    $scope.adherents.push(result[i]);
                }
            });
        };
        $scope.reset = function() {
            $scope.page = 1;
            $scope.adherents = [];
            $scope.loadAll();
        };
        $scope.loadPage = function(page) {
            $scope.page = page;
            $scope.loadAll();
        };
        $scope.loadAll();

        $scope.create = function () {
            Adherent.update($scope.adherent,
                function () {
                    $scope.reset();
                    $('#saveAdherentModal').modal('hide');
                    $scope.clear();
                });
        };

        $scope.update = function (id) {
            Adherent.get({id: id}, function(result) {
                $scope.adherent = result;
                $('#saveAdherentModal').modal('show');
            });
        };

        $scope.delete = function (id) {
            Adherent.get({id: id}, function(result) {
                $scope.adherent = result;
                $('#deleteAdherentConfirmation').modal('show');
            });
        };

        $scope.confirmDelete = function (id) {
            Adherent.delete({id: id},
                function () {
                    $scope.reset();
                    $('#deleteAdherentConfirmation').modal('hide');
                    $scope.clear();
                });
        };

        $scope.clear = function () {
            $scope.adherent = {prenom: null, nom: null, benevole: null, remarqueBenevolat: null, genre: null, autreRemarque: null, id: null};
            $scope.editForm.$setPristine();
            $scope.editForm.$setUntouched();
        };
    });
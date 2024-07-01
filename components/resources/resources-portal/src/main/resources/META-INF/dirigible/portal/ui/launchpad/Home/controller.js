/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('page', ["ideUI", "ideView", "entityApi"])
	.config(["messageHubProvider", function (messageHubProvider) {
		messageHubProvider.eventIdPrefix = 'portal.launchpad.Home';
	}])
	.config(["entityApiProvider", function (entityApiProvider) {
		entityApiProvider.baseUrl = "/services/js/portal/ui/launchpad/Home/tiles.js";
	}])
	.controller('PageController', ['$scope', 'messageHub', 'entityApi', '$document', function ($scope, messageHub, entityApi, $document) {
		const favoritesStoreId = 'DIRIGIBLE.portal.favorites';
		$scope.state = {
			isBusy: true,
			error: false,
			busyText: 'Loading...',
		};
		$scope.modules = [];
		$scope.referenceData = [];
		$scope.favorites = [];
		$scope.hasFavorites = false;
		$scope.editMode = false;
		$scope.favIcons = {
			'report': 'sap-icon--vertical-bar-chart',
			'entity': 'sap-icon--form',
			'setting': 'sap-icon--settings'
		};

		$scope.openView = function (location, name) {
			messageHub.postMessage("openView", {
				location: (name ? `${location.substring(0, location.indexOf('/gen/ui/'))}/gen/ui/Reports/index.html?${name}` : location)
			});
		};

		$scope.toggleEditMode = function () {
			$scope.editMode = !$scope.editMode;
		};

		$scope.getName = function (name) {
			let caption = name.substring(name.indexOf('-') ? name.indexOf('-') + 1 : 0);
			return caption.slice(0, 1).toUpperCase() + caption.slice(1, caption.length);
		};

		function saveFavorites() {
			let favorites = [];
			for (let i = 0; i < $scope.favorites.length; i++) {
				favorites.push($scope.favorites[i].id);
			}
			localStorage.setItem(favoritesStoreId, JSON.stringify(favorites));
		}

		$scope.removeFavorite = function (pos) {
			$scope.favorites.splice(pos, 1);
			saveFavorites();
			$scope.hasFavorites = $scope.favorites.length > 0;
			if ($scope.favorites.length === 0) $scope.editMode = false;
		};

		$scope.toggleFavorite = function (module, type, name, location, caption) {
			const id = `${module}.${type}.${name}`;
			if (!$scope.favorites.some((elem, pos) => {
				if (elem.id === id) {
					$scope.favorites.splice(pos, 1);
					return true
				} return false;
			})) {
				$scope.favorites.push({
					id: id,
					type: type,
					name: name,
					location: location,
					caption: caption,
				});
			}
			saveFavorites();
			$scope.hasFavorites = $scope.favorites.length > 0;
		};

		$scope.isFavorite = function (module, type, name) {
			const id = `${module}.${type}.${name}`;
			if ($scope.favorites.some(e => e.id === id)) {
				return true;
			} return false;
		};

		entityApi.list().then(function (response) {
			if (response.status != 200) {
				messageHub.showAlertError("Home", `Unable to get Home Launchpad: '${response.message}'`);
				$scope.state.isBusy = false;
				$scope.state.error = true;
				return;
			}

			$scope.modules = response.data.modules.filter(module => (module.name !== 'Reports'));
			for (let i = 0; i < response.data.referenceData.length; i++) {
				$scope.referenceData.push(...response.data.referenceData[i].settings);
			}

			const storedFavorites = JSON.parse(localStorage.getItem(favoritesStoreId) || '[]');
			for (let mi = 0; mi < $scope.modules.length; mi++) {
				for (let ei = 0; ei < $scope.modules[mi].entities.length; ei++) {
					const id = `${$scope.modules[mi].name}.entity.${$scope.modules[mi].entities[ei].name}`;
					const index = storedFavorites.indexOf(id);
					if (index > -1) {
						storedFavorites[index] = {
							id: id,
							type: 'entity',
							name: $scope.modules[mi].entities[ei].name,
							location: $scope.modules[mi].entities[ei].location,
							caption: $scope.modules[mi].entities[ei].caption,
						};
					}
				}
				for (let ri = 0; ri < $scope.modules[mi].reports.length; ri++) {
					const id = `${$scope.modules[mi].name}.report.${$scope.modules[mi].reports[ri].name}`;
					const index = storedFavorites.indexOf(id);
					if (index > -1) {
						storedFavorites[index] = {
							id: id,
							type: 'report',
							name: $scope.modules[mi].reports[ri].name,
							location: $scope.modules[mi].reports[ri].location,
							caption: $scope.modules[mi].reports[ri].caption,
						};
					}
				}
				for (let ri = 0; ri < $scope.modules[mi].settings.length; ri++) {
					const id = `${$scope.modules[mi].name}.setting.${$scope.modules[mi].settings[ri].name}`;
					const index = storedFavorites.indexOf(id);
					if (index > -1) {
						storedFavorites[index] = {
							id: id,
							type: 'setting',
							name: $scope.modules[mi].settings[ri].name,
							location: $scope.modules[mi].settings[ri].location,
							caption: $scope.modules[mi].settings[ri].caption,
						};
					}
				}
			}
			$scope.favorites = storedFavorites.filter(fav => (typeof fav !== 'string'));
			$scope.hasFavorites = $scope.favorites.length > 0;
			saveFavorites();
			$scope.state.isBusy = false;
		}, function (error) {
			console.error(error);
			$scope.state.error = true;
		});

		function favoriteOrderChange(evt) {
			const elem = $scope.favorites.splice(evt.oldIndex, 1)[0];
			$scope.$apply(function () {
				$scope.favorites.splice(evt.newIndex, 0, elem);
			});
			saveFavorites();
		}

		angular.element($document[0]).ready(function () {
			Sortable.create($document[0].getElementById('favorites'), {
				group: {
					name: 'favorites',
					put: false
				},
				animation: 200,
				onEnd: favoriteOrderChange,
				onMove: () => $scope.editMode
			});
		});
	}]);

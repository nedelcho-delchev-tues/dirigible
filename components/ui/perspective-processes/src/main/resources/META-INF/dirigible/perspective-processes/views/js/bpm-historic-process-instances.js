/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const historicProcessInstances = angular.module('historic-process-instances', ['platformView', 'blimpKit']);
historicProcessInstances.constant('Dialogs', new DialogHub());
historicProcessInstances.controller('BpmHistoricProcessInstancesView', ($scope, $http, Dialogs) => {
    $scope.instances = [];
    $scope.searchField = { text: '' };
    $scope.displaySearch = false;
    $scope.selectedProcessDefinitionKey = null;
    $scope.selectedId;

    setInterval(() => { $scope.fetchData() }, 5000);

    $scope.fetchData = () => {
        $http.get('/services/bpm/bpm-processes/historic-instances', { params: { 'businessKey': $scope.searchField.text, 'definitionKey': $scope.selectedProcessDefinitionKey, 'limit': 100 } })
            .then((response) => {
                $scope.instances = response.data;
            }, (error) => {
                console.error(error);
            });
    };

    $scope.selectionChanged = (instance) => {
        $scope.selectedId = instance.id;
        Dialogs.postMessage({ topic: 'bpm.historic.instance.selected', data: { instance: instance.id, definition: instance.processDefinitionId } });
    };

    $scope.toggleSearch = () => {
        $scope.displaySearch = !$scope.displaySearch;
    };

    Dialogs.addMessageListener({
        topic: 'bpm.definition.selected',
        handler: (data) => {
            $scope.$evalAsync(() => {
                if (data.hasOwnProperty('definition')) {
                    $scope.selectedProcessDefinitionKey = data.definition;
                    $scope.fetchData();
                } else {
                    Dialogs.showAlert({
                        title: 'Missing data',
                        message: 'Process definition is missing from event!',
                        type: AlertTypes.Error,
                        preformatted: false,
                    });
                }
            });
        }
    });

    $scope.inputSearchKeyUp = (e) => {
        switch (e.key) {
            case 'Escape':
                $scope.searchField.text = '';
                toggleSearch();
                $scope.fetchData();
                break;
            case 'Enter':
                $scope.fetchData();
                break;
        }
    };
});
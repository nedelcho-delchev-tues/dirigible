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
const bpmProcessViewer = angular.module('bpm-process-viewer', ['platformView', 'blimpKit']);
bpmProcessViewer.constant('MessageHub', new MessageHubApi());
bpmProcessViewer.controller('BpmProcessViewerController', ($scope, MessageHub) => {
    $scope.state = {
        isBusy: false,
        error: false,
        busyText: 'Loading...',
    };

    $scope.processId = '';
    let instanceId = '';

    const bpmnVisualization = new bpmnvisu.BpmnVisualization({ container: 'bpmn-container', navigation: { enabled: true } });
    let style = bpmnVisualization.graph.getStylesheet().getDefaultVertexStyle();
    let edgeStyle = bpmnVisualization.graph.getStylesheet().getDefaultEdgeStyle();

    edgeStyle[bpmnvisu.mxgraph.mxConstants.STYLE_FONTCOLOR] = 'var(--font_color)';
    edgeStyle[bpmnvisu.mxgraph.mxConstants.STYLE_LABEL_COLOR] = 'var(--font_color)';
    edgeStyle[bpmnvisu.mxgraph.mxConstants.STYLE_STROKECOLOR] = 'var(--stroke_color)';
    edgeStyle[bpmnvisu.mxgraph.mxConstants.STYLE_STROKEWIDTH] = 2;
    edgeStyle[bpmnvisu.mxgraph.mxConstants.STYLE_ROUNDED] = true;

    style[bpmnvisu.mxgraph.mxConstants.STYLE_FILLCOLOR] = 'var(--fill_color)';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_FONTCOLOR] = 'var(--font_color)';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_STROKECOLOR] = 'var(--stroke_color)';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_FONTFAMILY] = 'var(--font)';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_FONTSIZE] = '12';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_ARCSIZE] = '12';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_ROUNDED] = true;

    function loadActivities() {
        fetch(`/services/bpm/bpm-processes/instance/${encodeURIComponent(instanceId)}/active`).then(response => {
            if (!response.ok) {
                throw new Error(`Failed to fetch active activities for process instance: ${instanceId}`);
            }
            return response.text();
        }).then(activities => {
            JSON.parse(activities).forEach((activity) => bpmnVisualization.bpmnElementsRegistry.addCssClasses(activity, ['highlight']))
        }).finally(() => {
            $scope.$evalAsync(() => {
                $scope.state.isBusy = false;
            });
        });
    }

    function loadBpmnFromApi() {
        const endpoint = `/services/bpm/bpm-processes/definition/bpmn?id=${encodeURIComponent($scope.processId)}`;
        fetch(endpoint).then(response => {
            if (!response.ok) {
                throw new Error(`Failed to fetch BPMN XML for process definition id: ${$scope.processId}`);
            }
            return response.text();
        }).then(bpmnXml => {
            bpmnVisualization.load(bpmnXml, { fit: { type: bpmnvisu.FitType.Center, margin: 16 } });
            if (instanceId) {
                loadActivities();
            } else {
                $scope.$evalAsync(() => {
                    $scope.state.isBusy = false;
                });
            }
        }).catch(error => {
            console.error('Error loading BPMN:', error);
        });
    }

    $scope.zoomIn = () => {
        bpmnVisualization.navigation.graph.zoomIn();
    };

    $scope.zoomOut = () => {
        bpmnVisualization.navigation.graph.zoomOut();
    };

    $scope.fit = () => {
        bpmnVisualization.navigation.fit({ type: bpmnvisu.FitType.Center, margin: 16 });
    };

    $scope.actualSize = () => {
        bpmnVisualization.navigation.graph.zoomActual();
    };

    $scope.collapseAll = () => {

    };

    $scope.expandAll = () => {
        // bpmnVisualization.graph.model.execute('expandAll');
        console.log(bpmnVisualization)
    };

    MessageHub.addMessageListener({
        topic: 'bpm.diagram.definition',
        handler: (data) => {
            $scope.$evalAsync(() => {
                $scope.state.isBusy = true;
                if (!data.hasOwnProperty('definition')) {
                    $scope.state.error = true;
                    $scope.errorMessage = 'The \'definition\' parameter is missing.';
                } else {
                    $scope.processId = data.definition;
                    instanceId = '';
                    $scope.state.error = false;
                    loadBpmnFromApi();
                }
            });
        }
    });

    MessageHub.addMessageListener({
        topic: 'bpm.diagram.instance',
        handler: (data) => {
            $scope.$evalAsync(() => {
                $scope.state.isBusy = true;
                if (!data.hasOwnProperty('instance')) {
                    $scope.state.error = true;
                    $scope.errorMessage = 'The \'definition\' parameter is missing.';
                } else {
                    instanceId = data.instance;
                    $scope.state.error = false;
                    loadActivities();
                }
            });
        }
    });
});
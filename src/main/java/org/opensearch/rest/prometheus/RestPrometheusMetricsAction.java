/*
 * Copyright [2016] [Vincent VAN HOLLEBEKE]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.opensearch.rest.prometheus;

import static org.opensearch.action.NodePrometheusMetricsAction.INSTANCE;
import static org.opensearch.rest.RestRequest.Method.GET;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.compuscene.metrics.prometheus.PrometheusMetricsCatalog;
import org.compuscene.metrics.prometheus.PrometheusMetricsCollector;
import org.compuscene.metrics.prometheus.PrometheusSettings;
import org.opensearch.action.NodePrometheusMetricsRequest;
import org.opensearch.action.NodePrometheusMetricsResponse;
import org.opensearch.client.node.NodeClient;
import org.opensearch.common.network.NetworkAddress;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.rest.*;
import org.opensearch.rest.action.RestResponseListener;

import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * REST action class for Prometheus Exporter plugin.
 */
public class RestPrometheusMetricsAction extends BaseRestHandler {

    static String METRIC_PREFIX_KEY = "prometheus.metric_name.prefix";
    static Setting.Validator<String> indexPrefixValidator = value -> {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(
                METRIC_PREFIX_KEY + " value ["+value+"] is not valid"
            );
        }
    };
    public static final Setting<String> METRIC_PREFIX = Setting.simpleString(METRIC_PREFIX_KEY, "opensearch_", indexPrefixValidator, Setting.Property.NodeScope);


    private final String metricPrefix;
    private final PrometheusSettings prometheusSettings;
    private final Logger logger = LogManager.getLogger(getClass());

    public RestPrometheusMetricsAction(Settings settings, ClusterSettings clusterSettings) {
        this.prometheusSettings = new PrometheusSettings(settings, clusterSettings);
        this.metricPrefix = METRIC_PREFIX.get(settings);
        if (logger.isTraceEnabled()) {
            logger.trace("Prometheus metric prefix set to [{}]", this.metricPrefix);
        }
    }

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(
            new Route(GET, "/_prometheus/metrics"))
        );
    }

    @Override
    public String getName() {
        return "prometheus_metrics_action";
    }

     // This method does not throw any IOException because there are no request parameters to be parsed
     // and processed. This may change in the future.
    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        if (logger.isTraceEnabled()) {
            String remoteAddress = NetworkAddress.format(request.getHttpChannel().getRemoteAddress());
            logger.trace(String.format(Locale.ENGLISH, "Received request for Prometheus metrics from %s",
                    remoteAddress));
        }

        NodePrometheusMetricsRequest metricsRequest = new NodePrometheusMetricsRequest();

        return channel -> client.execute(INSTANCE, metricsRequest,
                new RestResponseListener<NodePrometheusMetricsResponse>(channel) {

                    @Override
                    public RestResponse buildResponse(NodePrometheusMetricsResponse response) throws Exception {
                String clusterName = response.getClusterHealth().getClusterName();
                String nodeName = response.getNodeStats().getNode().getName();
                String nodeId = response.getNodeStats().getNode().getId();
                if (logger.isTraceEnabled()) {
                    logger.trace("Prepare new Prometheus metric collector for: [{}], [{}], [{}]", clusterName, nodeId,
                            nodeName);
                }
                PrometheusMetricsCatalog catalog = new PrometheusMetricsCatalog(clusterName, nodeName, nodeId, metricPrefix);
                PrometheusMetricsCollector collector = new PrometheusMetricsCollector(
                        catalog,
                        prometheusSettings.getPrometheusIndices(),
                        prometheusSettings.getPrometheusClusterSettings());
                collector.registerMetrics();
                collector.updateMetrics(response.getClusterHealth(), response.getNodeStats(), response.getIndicesStats(),
                        response.getClusterStatsData());
                return new BytesRestResponse(RestStatus.OK, collector.getCatalog().toTextFormat());
            }
        });
    }
}

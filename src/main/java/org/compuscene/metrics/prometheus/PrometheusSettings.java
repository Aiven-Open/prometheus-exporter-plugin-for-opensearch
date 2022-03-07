/*
 * Copyright [2019] [Lukáš VLČEK]
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

package org.compuscene.metrics.prometheus;

import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;

/**
 * Dynamically updatable Prometheus exporter settings.
 *
 * These settings are part of cluster state available via
 * <pre>{@code
 * curl <opensearch>/_cluster/settings?include_defaults=true&filter_path=defaults.prometheus
 * }</pre>
 */
public class PrometheusSettings {

    public static final Setting<Boolean> PROMETHEUS_CLUSTER_SETTINGS =
            Setting.boolSetting("prometheus.cluster.settings", true,
                    Setting.Property.Dynamic, Setting.Property.NodeScope);
    public static final Setting<Boolean> PROMETHEUS_INDICES =
            Setting.boolSetting("prometheus.indices", true,
                    Setting.Property.Dynamic, Setting.Property.NodeScope);
    public static final Setting<String> PROMETHEUS_NODES_FILTER =
            Setting.simpleString("prometheus.nodes.filter", "_local",
                    Setting.Property.Dynamic, Setting.Property.NodeScope);

    private volatile boolean clusterSettings;
    private volatile boolean indices;
    private volatile String nodesFilter;

    public PrometheusSettings(Settings settings, ClusterSettings clusterSettings) {
        setPrometheusClusterSettings(PROMETHEUS_CLUSTER_SETTINGS.get(settings));
        setPrometheusIndices(PROMETHEUS_INDICES.get(settings));
        setPrometheusNodesFilter(PROMETHEUS_NODES_FILTER.get(settings));
        clusterSettings.addSettingsUpdateConsumer(PROMETHEUS_CLUSTER_SETTINGS, this::setPrometheusClusterSettings);
        clusterSettings.addSettingsUpdateConsumer(PROMETHEUS_INDICES, this::setPrometheusIndices);
        clusterSettings.addSettingsUpdateConsumer(PROMETHEUS_NODES_FILTER, this::setPrometheusNodesFilter);
    }

    private void setPrometheusClusterSettings(boolean flag) {
        this.clusterSettings = flag;
    }

    private void setPrometheusIndices(boolean flag) {
        this.indices = flag;
    }

    private void setPrometheusNodesFilter(String filter) { this.nodesFilter = filter; }

    public boolean getPrometheusClusterSettings() {
        return this.clusterSettings;
    }

    public boolean getPrometheusIndices() {
        return this.indices;
    }

    public String getNodesFilter() { return this.nodesFilter; }
}

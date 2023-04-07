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

    static String PROMETHEUS_CLUSTER_SETTINGS_KEY = "prometheus.cluster.settings";
    static String PROMETHEUS_INDICES_KEY = "prometheus.indices";
    static String PROMETHEUS_NODES_FILTER_KEY = "prometheus.nodes.filter";
    static String PROMETHEUS_INDICES_FILTER_KEY = "prometheus.indices_filter";

    /**
     * This setting is used configure weather to expose cluster settings metrics or not. The default value is true.
     * Can be configured in opensearch.yml file or update dynamically under key {@link #PROMETHEUS_CLUSTER_SETTINGS_KEY}.
     */
    public static final Setting<Boolean> PROMETHEUS_CLUSTER_SETTINGS =
            Setting.boolSetting(PROMETHEUS_CLUSTER_SETTINGS_KEY, true,
                    Setting.Property.Dynamic, Setting.Property.NodeScope);

    /**
     * This setting is used configure weather to expose low level index metrics or not. The default value is true.
     * Can be configured in opensearch.yml file or update dynamically under key {@link #PROMETHEUS_INDICES_KEY}.
     */
    public static final Setting<Boolean> PROMETHEUS_INDICES =
            Setting.boolSetting(PROMETHEUS_INDICES_KEY, true,
                    Setting.Property.Dynamic, Setting.Property.NodeScope);

    /**
     * This setting is used configure which cluster nodes to gather metrics from. The default value is _local.
     * Can be configured in opensearch.yml file or update dynamically under key {@link #PROMETHEUS_NODES_FILTER_KEY}.
     */
    public static final Setting<String> PROMETHEUS_NODES_FILTER =
            Setting.simpleString(PROMETHEUS_NODES_FILTER_KEY, "_local",
                    Setting.Property.Dynamic, Setting.Property.NodeScope);

    /**
     * This setting is used configure to filter indices statistics with indices starting with prefixes. The default value is "".
     * Can be configured in opensearch.yml file or update dynamically under key {@link #PROMETHEUS_INDICES_FILTER_KEY}.
     */
    public static final Setting<String> PROMETHEUS_INDICES_FILTER =
            Setting.simpleString(PROMETHEUS_INDICES_FILTER_KEY, "",
                    Setting.Property.Dynamic, Setting.Property.NodeScope);

    private volatile boolean clusterSettings;
    private volatile boolean indices;
    private volatile String nodesFilter;
    private volatile String indicesFilter;

    /**
     * A constructor.
     * @param settings Settings
     * @param clusterSettings Cluster settings
     */
    public PrometheusSettings(Settings settings, ClusterSettings clusterSettings) {
        setPrometheusClusterSettings(PROMETHEUS_CLUSTER_SETTINGS.get(settings));
        setPrometheusIndices(PROMETHEUS_INDICES.get(settings));
        setPrometheusNodesFilter(PROMETHEUS_NODES_FILTER.get(settings));
        setPrometheusIndicesFilter(PROMETHEUS_INDICES_FILTER.get(settings));
        clusterSettings.addSettingsUpdateConsumer(PROMETHEUS_CLUSTER_SETTINGS, this::setPrometheusClusterSettings);
        clusterSettings.addSettingsUpdateConsumer(PROMETHEUS_INDICES, this::setPrometheusIndices);
        clusterSettings.addSettingsUpdateConsumer(PROMETHEUS_NODES_FILTER, this::setPrometheusNodesFilter);
        clusterSettings.addSettingsUpdateConsumer(PROMETHEUS_INDICES_FILTER, this::setPrometheusIndicesFilter);
    }

    private void setPrometheusClusterSettings(boolean flag) {
        this.clusterSettings = flag;
    }

    private void setPrometheusIndices(boolean flag) {
        this.indices = flag;
    }

    private void setPrometheusNodesFilter(String filter) { this.nodesFilter = filter; }

    private void setPrometheusIndicesFilter(String indicesFilter) {
        this.indicesFilter = indicesFilter;
    }

    /**
     * Get value of settings key {@link #PROMETHEUS_CLUSTER_SETTINGS_KEY}.
     * @return boolean value of the key
     */
    public boolean getPrometheusClusterSettings() {
        return this.clusterSettings;
    }

    /**
     * Get value of settings key {@link #PROMETHEUS_INDICES_KEY}.
     * @return boolean value of the key
     */
    public boolean getPrometheusIndices() {
        return this.indices;
    }

    /**
     * Get value of settings key {@link #PROMETHEUS_NODES_FILTER_KEY}.
     * @return boolean value of the key
     */
    public String getNodesFilter() { return this.nodesFilter; }

    /**
     * Get value of settings key {@link #PROMETHEUS_INDICES_FILTER_KEY}.
     * @return string value of the key
     */
    public String getPrometheusIndicesFilter() {
        return this.indicesFilter;
    }
}

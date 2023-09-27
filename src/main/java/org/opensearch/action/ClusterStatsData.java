/*
 * Copyright [2018] [Vincent VAN HOLLEBEKE]
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
package org.opensearch.action;

import static org.opensearch.cluster.routing.allocation.DiskThresholdSettings.*;

import org.opensearch.OpenSearchParseException;
import org.opensearch.action.admin.cluster.state.ClusterStateResponse;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.common.Nullable;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsException;
import org.opensearch.common.unit.RatioValue;

import java.io.IOException;


/**
 * Selected settings from OpenSearch cluster settings.
 *
 * Disk-based shard allocation [1] settings play important role in how OpenSearch decides where to allocate
 * new shards or if existing shards are relocated to different nodes. The tricky part about these settings is
 * that they can be expressed either in percent or bytes value (they cannot be mixed) and they can be updated on the fly.
 *
 * TODO[lukas-vlcek]: Update docs URL
 * [1] https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html#disk-allocator
 *
 * In order to make it easy for Prometheus to consume the data we expose these settings in both formats (pct and bytes)
 * and we do our best in determining if they are currently set as pct or bytes filling appropriate variables with data
 * or null value.
 */
// TODO(lukas-vlcek): should this extend TransportMessage instead?
public class ClusterStatsData extends ActionResponse {

    private Boolean thresholdEnabled = null;

    @Nullable private Long diskLowInBytes;
    @Nullable private Long diskHighInBytes;
    @Nullable private Long floodStageInBytes;

    @Nullable private Double diskLowInPct;
    @Nullable private Double diskHighInPct;
    @Nullable private Double floodStageInPct;

    private Long[] diskLowInBytesRef = new Long[]{diskLowInBytes};
    private Long[] diskHighInBytesRef = new Long[]{diskHighInBytes};
    private Long[] floodStageInBytesRef = new Long[]{floodStageInBytes};

    private Double[] diskLowInPctRef = new Double[]{diskLowInPct};
    private Double[] diskHighInPctRef = new Double[]{diskHighInPct};
    private Double[] floodStageInPctRef = new Double[]{floodStageInPct};

    /**
     * A constructor.
     * @param in A streamInput to materialize the instance from
     * @throws IOException if reading from streamInput is not successful
     */
    public ClusterStatsData(StreamInput in) throws IOException {
        super(in);
        thresholdEnabled = in.readOptionalBoolean();
        //
        diskLowInBytes = in.readOptionalLong();
        diskHighInBytes = in.readOptionalLong();
        floodStageInBytes = in.readOptionalLong();
        //
        diskLowInPct = in.readOptionalDouble();
        diskHighInPct = in.readOptionalDouble();
        floodStageInPct = in.readOptionalDouble();
    }

    @SuppressWarnings({"checkstyle:LineLength"})
    ClusterStatsData(ClusterStateResponse clusterStateResponse, Settings settings, ClusterSettings clusterSettings) {

        Metadata m = clusterStateResponse.getState().getMetadata();
        // There are several layers of cluster settings in Elasticsearch each having different priority.
        // We need to traverse them from the top priority down to find relevant value of each setting.
        // See https://www.elastic.co/guide/en/elasticsearch/reference/master/cluster-update-settings.html#_order_of_precedence
        // TODO[lukas-vlcek]: update to OpenSearch referenced
        for (Settings s : new Settings[]{
                // See: RestClusterGetSettingsAction#response
                // or: https://github.com/elastic/elasticsearch/pull/33247/files
                // We do not filter the settings, but we use the clusterSettings.diff()
                // In the end we expose just a few selected settings ATM.
                m.transientSettings(),
                m.persistentSettings(),
                clusterSettings.diff(m.settings(), settings)
        }) {
            thresholdEnabled = thresholdEnabled == null ?
                    s.getAsBoolean(CLUSTER_ROUTING_ALLOCATION_DISK_THRESHOLD_ENABLED_SETTING.getKey(), null) : thresholdEnabled;

            parseValue(s, CLUSTER_ROUTING_ALLOCATION_LOW_DISK_WATERMARK_SETTING.getKey(), diskLowInBytesRef, diskLowInPctRef);
            parseValue(s, CLUSTER_ROUTING_ALLOCATION_HIGH_DISK_WATERMARK_SETTING.getKey(), diskHighInBytesRef, diskHighInPctRef);
            parseValue(s, CLUSTER_ROUTING_ALLOCATION_DISK_FLOOD_STAGE_WATERMARK_SETTING.getKey(), floodStageInBytesRef, floodStageInPctRef);
        }

        diskLowInBytes = diskLowInBytesRef[0];
        diskHighInBytes = diskHighInBytesRef[0];
        floodStageInBytes = floodStageInBytesRef[0];

        diskLowInPct = diskLowInPctRef[0];
        diskHighInPct = diskHighInPctRef[0];
        floodStageInPct = floodStageInPctRef[0];
    }

    /**
     * Try to extract and parse value from settings for given key.
     * First it tries to parse it as a RatioValue (pct) then as byte size value.
     * It assigns parsed value to corresponding argument references (passed via array hack).
     * If parsing fails the method fires exception, however, this should not happen - we rely on Elasticsearch
     * to already have parsed and validated these values before. Unless we screwed something up...
     */
    private void parseValue(Settings s, String key, Long[] bytesPointer, Double[] pctPointer) {
        String value = s.get(key);
        if (value != null && pctPointer[0] == null) {
            try {
                pctPointer[0] = RatioValue.parseRatioValue(s.get(key, null)).getAsPercent();
            } catch (SettingsException | OpenSearchParseException | NullPointerException e1) {
                if (bytesPointer[0] == null) {
                    try {
                        bytesPointer[0] = s.getAsBytesSize(key, null).getBytes();
                    } catch (SettingsException | OpenSearchParseException | NullPointerException e2) {
                        // TODO(lvlcek): log.debug("This went wrong, but 'Keep Calm and Carry On'")
                        // We should avoid using logs in this class (due to perf impact), instead we should
                        // consider moving this logic to some static helper class/method going forward.
                    }
                }
            }
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalBoolean(thresholdEnabled);
        //
        out.writeOptionalLong(diskLowInBytes);
        out.writeOptionalLong(diskHighInBytes);
        out.writeOptionalLong(floodStageInBytes);
        //
        out.writeOptionalDouble(diskLowInPct);
        out.writeOptionalDouble(diskHighInPct);
        out.writeOptionalDouble(floodStageInPct);
    }

    /**
     * Get value of setting controlled by {@link org.opensearch.cluster.routing.allocation.DiskThresholdSettings#CLUSTER_ROUTING_ALLOCATION_DISK_THRESHOLD_ENABLED_SETTING}.
     * @return A Boolean value of the setting.
     */
    public Boolean getThresholdEnabled() {
        return thresholdEnabled;
    }

    /**
     * Get value of setting controlled by {@link org.opensearch.cluster.routing.allocation.DiskThresholdSettings#CLUSTER_ROUTING_ALLOCATION_LOW_DISK_WATERMARK_SETTING}.
     * @return A Long value of the setting.
     */
    @Nullable
    public Long getDiskLowInBytes() {
        return diskLowInBytes;
    }

    /**
     * Get value of setting controlled by {@link org.opensearch.cluster.routing.allocation.DiskThresholdSettings#CLUSTER_ROUTING_ALLOCATION_HIGH_DISK_WATERMARK_SETTING}.
     * @return A Long value of the setting.
     */
    @Nullable
    public Long getDiskHighInBytes() {
        return diskHighInBytes;
    }

    /**
     * Get value of setting controlled by {@link org.opensearch.cluster.routing.allocation.DiskThresholdSettings#CLUSTER_ROUTING_ALLOCATION_DISK_FLOOD_STAGE_WATERMARK_SETTING}.
     * @return A Long value of the setting.
     */
    @Nullable
    public Long getFloodStageInBytes() {
        return floodStageInBytes;
    }

    /**
     * Get value of setting controlled by {@link org.opensearch.cluster.routing.allocation.DiskThresholdSettings#CLUSTER_ROUTING_ALLOCATION_LOW_DISK_WATERMARK_SETTING}.
     * @return A Double value of the setting.
     */
    @Nullable
    public Double getDiskLowInPct() {
        return diskLowInPct;
    }

    /**
     * Get value of setting controlled by {@link org.opensearch.cluster.routing.allocation.DiskThresholdSettings#CLUSTER_ROUTING_ALLOCATION_HIGH_DISK_WATERMARK_SETTING}.
     * @return A Double value of the setting.
     */
    @Nullable
    public Double getDiskHighInPct() {
        return diskHighInPct;
    }

    /**
     * Get value of setting controlled by {@link org.opensearch.cluster.routing.allocation.DiskThresholdSettings#CLUSTER_ROUTING_ALLOCATION_DISK_FLOOD_STAGE_WATERMARK_SETTING}.
     * @return A Double value of the setting.
     */
    @Nullable
    public Double getFloodStageInPct() {
        return floodStageInPct;
    }
}

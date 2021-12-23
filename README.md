# Prometheus Exporter Plugin for OpenSearch

The [Prometheus exporter](https://prometheus.io/docs/instrumenting/writing_exporters/) plugin for OpenSearch exposes many OpenSearch metrics in [Prometheus format](https://prometheus.io/docs/instrumenting/exposition_formats/).

This plugin is based on [Prometheus exporter for Elasticsearch](https://github.com/vvanholl/elasticsearch-prometheus-exporter) and all the great work the community has done. The Elasticsearch plugin was forked in version 7.10.2.0 (commit hash: [8dc7f85](https://github.com/vvanholl/elasticsearch-prometheus-exporter/commit/8dc7f85109fe1601a68010e9de598a9b131afd02)) which is the latest when Elasticsearch was still using AL2.

**Currently, the available metrics are:**

- Cluster status
- Nodes status:
    - JVM
    - Indices (global)
    - Transport
    - HTTP
    - Scripts
    - Process
    - Operating System
    - File System
    - Circuit Breaker
- Indices status
- Cluster settings (notably [disk watermarks](https://opensearch.org/docs/latest/opensearch/popular-api/#change-disk-watermarks-or-other-cluster-settings) that can be updated dynamically)

## Compatibility matrix

| OpenSearch | Plugin   | Release date    |
|------------|----------|-----------------|
| 1.2.3      | 1.2.3.0  | TBD |

## Install

Before you start OpenSearch cluster install the plugin on each cluster node that will be scraped by Prometheus:

_Notice: the URL location is temporary for now._ 

`./bin/opensearch-plugin install https://github.com/TBD/prometheus-exporter/releases/download/1.2.3.0/prometheus-exporter-1.2.3.0.zip`

Start OpenSearch cluster.

## Plugin configuration

If you have a lot of indices and think this data is irrelevant, you can disable in the main configuration file:

```
prometheus.indices: false
```

To disable exporting cluster settings use:
```
prometheus.cluster.settings: false
```

These settings can be also [updated dynamically](https://opensearch.org/docs/latest/opensearch/configuration/#update-cluster-settings-using-the-api).

## Uninstall

You need to remove the plugin from all nodes where it was installed.

`./bin/elasticsearch-plugin remove prometheus-exporter`

Do not forget to restart the node after installation.

## Usage

Metrics are directly available at:

    http(s)://<your-opensearch-host>:9200/_prometheus/metrics

As a sample result, you get:

```
# HELP es_process_mem_total_virtual_bytes Memory used by ES process
# TYPE es_process_mem_total_virtual_bytes gauge
es_process_mem_total_virtual_bytes{cluster="develop",node="develop01",} 3.626733568E9
# HELP es_indices_indexing_is_throttled_bool Is indexing throttling ?
# TYPE es_indices_indexing_is_throttled_bool gauge
es_indices_indexing_is_throttled_bool{cluster="develop",node="develop01",} 0.0
# HELP es_jvm_gc_collection_time_seconds Time spent for GC collections
# TYPE es_jvm_gc_collection_time_seconds counter
es_jvm_gc_collection_time_seconds{cluster="develop",node="develop01",gc="old",} 0.0
es_jvm_gc_collection_time_seconds{cluster="develop",node="develop01",gc="young",} 0.0
# HELP es_indices_requestcache_memory_size_bytes Memory used for request cache
# TYPE es_indices_requestcache_memory_size_bytes gauge
es_indices_requestcache_memory_size_bytes{cluster="develop",node="develop01",} 0.0
# HELP es_indices_search_open_contexts_number Number of search open contexts
# TYPE es_indices_search_open_contexts_number gauge
es_indices_search_open_contexts_number{cluster="develop",node="develop01",} 0.0
# HELP es_jvm_mem_nonheap_used_bytes Memory used apart from heap
# TYPE es_jvm_mem_nonheap_used_bytes gauge
es_jvm_mem_nonheap_used_bytes{cluster="develop",node="develop01",} 5.5302736E7
...
```

### Configure the Prometheus target

On your Prometheus servers, configure a new job as usual.

For example, if you have a cluster of 3 nodes:

```YAML
- job_name: opensearch
  scrape_interval: 10s
  metrics_path: "/_prometheus/metrics"
  static_configs:
  - targets:
    - node1:9200
    - node2:9200
    - node3:9200
```

Of course, you could use the service discovery service instead of a static config.

Just keep in mind that `metrics_path` must be `/_prometheus/metrics`, otherwise Prometheus will find no metric.

## Testing

Project contains [integration tests](src/test/resources/rest-api-spec) implemented using
[rest layer](https://github.com/opensearch-project/OpenSearch/blob/main/TESTING.md#testing-the-rest-layer)
framework.

Complete test suite is run using:
```
./gradlew clean check
```

To run individual integration rest test file use:
```
./gradlew :yamlRestTest \
  -Dtests.class=org.opensearch.plugin.prometheus.PrometheusRestHandlerClientYamlTestSuiteIT \
  -Dtests.method="test {yaml=test/20_metrics/Prometheus metrics can be pulled}"
```

## Credits

This plugin mainly uses the [Prometheus JVM Client](https://github.com/prometheus/client_java).

## License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

# Prometheus Exporter Plugin for OpenSearch

The [Prometheus® exporter](https://prometheus.io/docs/instrumenting/writing_exporters/) plugin for OpenSearch® exposes many OpenSearch metrics in [Prometheus format](https://prometheus.io/docs/instrumenting/exposition_formats/).

- [Preface](#preface)
- [Introduction](#introduction)
- [Compatibility Matrix](#compatibility-matrix)
- [Install or Remove Plugin](#install-or-remove-plugin)
- [Plugin Configuration](#plugin-configuration)
  - [Static settings](#static-settings) 
  - [Dynamic settings](#dynamic-settings) 
- [Usage](#usage)
- [Build from Source](#build-from-source)
- [Testing](#testing)
- [License](#license)
- [Trademarks & Attributions](#trademarks--attributions)

---

## Preface

This repository contains two projects that are related, but each can be used independently:

1. The first project is a Java plugin – Prometheus Exporter Plugin for OpenSearch® – this code lives in the `main` branch.
2. The second project is a Jsonnet bundle – OpenSearch® Mixin – this code lives in the `mixin` branch.

## Introduction

This plugin was started as a fork of [Prometheus exporter for Elasticsearch®](https://github.com/vvanholl/elasticsearch-prometheus-exporter) (it was forked in version 7.10.2.0; commit hash: [8dc7f85](https://github.com/vvanholl/elasticsearch-prometheus-exporter/commit/8dc7f85109fe1601a68010e9de598a9b131afd02)) utilizing [OpenSearch plugin template](https://github.com/opensearch-project/opensearch-plugin-template-java). It uses the official [Prometheus Java Simpleclient](https://github.com/prometheus/client_java/tree/master/simpleclient).

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

## Compatibility Matrix
NOTE: OpenSearch plugins much match _exactly_ in major.minor.path version to the OpenSearch instance it's running in, e.g. plugins versions 2.8.0.x work only with OpenSearch 2.8.0, see also [Opensearch Plugins](https://opensearch.org/docs/latest/install-and-configure/plugins/#available-plugins). Therefore you must keep your [prometheus-exporter-plugin-for-opensearch version](https://github.com/aiven/prometheus-exporter-plugin-for-opensearch/releases) in sync with your OpenSearch version.

| OpenSearch |      Plugin |  Release date |
|-----------:|------------:|--------------:|
|      2.10.0|     2.10.0.0|  Sep 25, 2023 |
|      2.9.0 |     2.9.0.0 |  Aug 01, 2023 |
|      2.8.0 |     2.8.0.0 |  Jun 13, 2023 |
|      2.7.0 |     2.7.0.0 |  May 10, 2023 |
|      2.6.0 |     2.6.0.0 |  Mar 03, 2023 |
|      2.5.0 |     2.5.0.0 |  Jan 30, 2023 |
|      2.4.1 |     2.4.1.0 |  Dec 19, 2022 |
|      2.4.0 |     2.4.0.0 |  Nov 18, 2022 |
|      2.3.0 |     2.3.0.0 |  Sep 19, 2022 |
|      2.2.1 |     2.2.1.0 |  Sep  5, 2022 |
|      2.2.0 |     2.2.0.0 |  Aug 13, 2022 |
|      2.1.0 |     2.1.0.0 | July 13, 2022 |
|      2.0.1 |     2.0.1.0 | July 12, 2022 |
|      2.0.0 |     2.0.0.0 |  May 27, 2022 |
|  2.0.0-rc1 | 2.0.0.0-rc1 |  May 19, 2022 |
|     1.3.12 |    1.3.12.0 |  Aug 31, 2023 |
|     1.3.11 |    1.3.11.0 |  Jul 06, 2023 |
|     1.3.10 |    1.3.10.0 |  May 23, 2023 |
|      1.3.9 |     1.3.9.0 |  Mar 20, 2023 |
|      1.3.8 |     1.3.8.0 |  Mar 03, 2023 |
|      1.3.7 |     1.3.7.0 |  Dec 19, 2022 |
|      1.3.6 |     1.3.6.0 |  Oct 10, 2022 |
|      1.3.5 |     1.3.5.0 |  Sep 05, 2022 |
|      1.3.4 |     1.3.4.0 |  Jul 15, 2022 |
|      1.3.3 |     1.3.3.0 |  Jun 15, 2022 |
|      1.3.2 |     1.3.2.0 |  May 10, 2022 |
|      1.3.1 |     1.3.1.0 |  Apr 08, 2022 |
|      1.3.0 |     1.3.0.0 |  Mar 22, 2022 |
|   <= 1.2.4 |         (*) |           (*) |

_(*) If you are looking for plugin releases supporting earlier (`<= 1.2.4`) OpenSearch versions please visit
<https://github.com/aparo/opensearch-prometheus-exporter/releases>._

## Install or Remove Plugin

You need to install the plugin on every OpenSearch node that will be scraped by Prometheus.

To **install** the plugin:

`./bin/opensearch-plugin install https://github.com/aiven/prometheus-exporter-plugin-for-opensearch/releases/download/2.10.0.0/prometheus-exporter-2.10.0.0.zip`

To **remove** the plugin.

`./bin/opensearch-plugin remove prometheus-exporter`

For more info about plugin CLI, visit: <https://opensearch.org/docs/latest/opensearch/install/plugins/>

## Plugin Configuration

### Static settings

Static settings are configured in `config/opensearch.yml`.

#### Metric name prefix

All metric names share common prefix, by default set to `opensearch_`.

The value of this prefix can be customized using setting:
```
prometheus.metric_name.prefix: "opensearch_"
```

### Dynamic settings

Dynamic settings are configured in `config/opensearch.yml` but they can also be [updated](https://opensearch.org/docs/latest/opensearch/configuration/#update-cluster-settings-using-the-api) at any time via REST API.

#### Index level metrics

Whether to export detailed index level metrics or not. Default value: `true`.

Detailed index level metrics can represent a lot of data and can lead to high-cardinality labels in Prometheus.
If you do not need detailed index level metrics **it is recommended to disable it** via setting:

```
prometheus.indices: false
```

#### Cluster settings

To disable exporting cluster settings use:
```
prometheus.cluster.settings: false
```

#### Nodes filter

Metrics include statistics about individual OpenSearch nodes.
By default, only statistics from the node that received the request are included.

Prometheus exporter can be configured to include statistics from other nodes as well.
This filter is directly utilizing OpenSearch [Node filters](https://opensearch.org/docs/latest/opensearch/rest-api/nodes-apis/index/#node-filters) feature.
Default value: `"_local"`.

For example to get stats for all cluster nodes from any node use settings:
```
prometheus.nodes.filter: "_all"
```

#### Indices filter

Prometheus exporter can be configured to filter indices statistics from selected indices.
To target all indices, use "" or * or _all or null.
Default value: `""`.

For example to filter indices statistics:
```
prometheus.indices_filter.selected_indices: "log-*,*log,*log*,log*-test"
```

Users can select which option for filtering indices statistics.
Default value: `"STRICT_EXPAND_OPEN_FORBID_CLOSED"`.

For example to select an option:
```
prometheus.indices_filter.selected_option: "LENIENT_EXPAND_OPEN"
```
Here is a list of indices options available for selection
```
STRICT_EXPAND_OPEN: indices options that requires every specified index to exist, expands wildcards only to open indices and allows that no indices are resolved from wildcard expressions (not returning an error).
STRICT_EXPAND_OPEN_HIDDEN: indices options that requires every specified index to exist, expands wildcards only to open indices, includes hidden indices, and allows that no indices are resolved from wildcard expressions (not returning an error).
STRICT_EXPAND_OPEN_FORBID_CLOSED: indices options that requires every specified index to exist, expands wildcards only to open indices, allows that no indices are resolved from wildcard expressions (not returning an error) and forbids the use of closed indices by throwing an error.
STRICT_EXPAND_OPEN_HIDDEN_FORBID_CLOSED: indices options that requires every specified index to exist, expands wildcards only to open indices, includes hidden indices, and allows that no indices are resolved from wildcard expressions (not returning an error) and forbids the use of closed indices by throwing an error.
STRICT_EXPAND_OPEN_FORBID_CLOSED_IGNORE_THROTTLED: indices options that requires every specified index to exist, expands wildcards only to open indices, allows that no indices are resolved from wildcard expressions (not returning an error), forbids the use of closed indices by throwing an error and ignores indices that are throttled.
STRICT_EXPAND_OPEN_CLOSED: indices option that requires every specified index to exist, expands wildcards to both open and closed indices and allows that no indices are resolved from wildcard expressions (not returning an error).
STRICT_EXPAND_OPEN_CLOSED_HIDDEN: indices option that requires every specified index to exist, expands wildcards to both open and closed indices, includes hidden indices, and allows that no indices are resolved from wildcard expressions (not returning an error).
STRICT_SINGLE_INDEX_NO_EXPAND_FORBID_CLOSED: indices option that requires each specified index or alias to exist, doesn't expand wildcards and throws error if any of the aliases resolves to multiple indices.
LENIENT_EXPAND_OPEN: indices options that ignores unavailable indices, expands wildcards only to open indices and allows that no indices are resolved from wildcard expressions (not returning an error).
LENIENT_EXPAND_OPEN_HIDDEN: indices options that ignores unavailable indices, expands wildcards to open and hidden indices, and allows that no indices are resolved from wildcard expressions (not returning an error).
LENIENT_EXPAND_OPEN_CLOSED: indices options that ignores unavailable indices, expands wildcards to both open and closed indices and allows that no indices are resolved from wildcard expressions (not returning an error).
LENIENT_EXPAND_OPEN_CLOSED_HIDDEN: indices options that ignores unavailable indices, expands wildcards to all open and closed indices and allows that no indices are resolved from wildcard expressions (not returning an error).
```

## Usage

Metrics are directly available at:

    http(s)://<opensearch-host>:9200/_prometheus/metrics

As a sample result, you get:

```
# HELP opensearch_process_mem_total_virtual_bytes Memory used by ES process
# TYPE opensearch_process_mem_total_virtual_bytes gauge
opensearch_process_mem_total_virtual_bytes{cluster="develop",node="develop01",} 3.626733568E9
# HELP opensearch_indices_indexing_is_throttled_bool Is indexing throttling ?
# TYPE opensearch_indices_indexing_is_throttled_bool gauge
opensearch_indices_indexing_is_throttled_bool{cluster="develop",node="develop01",} 0.0
# HELP opensearch_jvm_gc_collection_time_seconds Time spent for GC collections
# TYPE opensearch_jvm_gc_collection_time_seconds counter
opensearch_jvm_gc_collection_time_seconds{cluster="develop",node="develop01",gc="old",} 0.0
opensearch_jvm_gc_collection_time_seconds{cluster="develop",node="develop01",gc="young",} 0.0
# HELP opensearch_indices_requestcache_memory_size_bytes Memory used for request cache
# TYPE opensearch_indices_requestcache_memory_size_bytes gauge
opensearch_indices_requestcache_memory_size_bytes{cluster="develop",node="develop01",} 0.0
# HELP opensearch_indices_search_open_contexts_number Number of search open contexts
# TYPE opensearch_indices_search_open_contexts_number gauge
opensearch_indices_search_open_contexts_number{cluster="develop",node="develop01",} 0.0
# HELP opensearch_jvm_mem_nonheap_used_bytes Memory used apart from heap
# TYPE opensearch_jvm_mem_nonheap_used_bytes gauge
opensearch_jvm_mem_nonheap_used_bytes{cluster="develop",node="develop01",} 5.5302736E7
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

## Build from Source

To build the plugin you need JDK 14:

```
./gradlew clean build
```
If you have doubts about the system requirements, please check the [CI.yml](.github/workflows/CI.yml) file for more information.

## Testing

Project contains:
- [integration tests](src/yamlRestTest/resources/rest-api-spec) implemented using
[rest layer](https://github.com/opensearch-project/OpenSearch/blob/main/TESTING.md#testing-the-rest-layer)
framework
- [internal cluster tests](src/test)

Complete test suite is run using:
```
./gradlew clean check
```

To run individual integration rest test file use:
```
./gradlew :yamlRestTest \
  -Dtests.method="test {yaml=/20_11_index_level_metrics_disabled/Dynamically disable index level metrics}"
```

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

## Trademarks & Attributions

Prometheus is a registered trademark of The Linux Foundation. OpenSearch is a registered trademark of Amazon Web Services. Elasticsearch is a registered trademark of Elasticsearch BV.

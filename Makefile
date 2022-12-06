DISTRIBUTION_FOLDER = distribution
JSONNET_FMT := jsonnetfmt -n 2 --max-blank-lines 2 --string-style s --comment-style s

.PHONY: clean dashboards

all: fmt dashboards prometheus_alerts.yaml prometheus_recording_rules.yaml lint

fmt:
	find . -name 'vendor' -prune -o -name '*.libsonnet' -print -o -name '*.jsonnet' -print | \
		xargs -n 1 -- $(JSONNET_FMT) -i

dashboards: mixin.libsonnet lib/dashboards.jsonnet dashboards/*.libsonnet
	@mkdir -p ${DISTRIBUTION_FOLDER}
	jsonnet -J vendor -m ${DISTRIBUTION_FOLDER} lib/dashboards.jsonnet

prometheus_alerts.yaml: mixin.libsonnet lib/alerts.jsonnet alerts/*.libsonnet
	@mkdir -p ${DISTRIBUTION_FOLDER}
	jsonnet -S lib/alerts.jsonnet > ${DISTRIBUTION_FOLDER}/$@

prometheus_recording_rules.yaml: mixin.libsonnet lib/rules.jsonnet rules/*.libsonnet
	@mkdir -p ${DISTRIBUTION_FOLDER}
	jsonnet -S lib/rules.jsonnet > ${DISTRIBUTION_FOLDER}/$@

lint: prometheus_alerts.yaml prometheus_recording_rules.yaml
	find . -name 'vendor' -prune -o -name '*.libsonnet' -print -o -name '*.jsonnet' -print | \
		while read f; do \
			$(JSONNET_FMT) "$$f" | diff -u "$$f" -; \
		done

	promtool check rules ${DISTRIBUTION_FOLDER}/prometheus_recording_rules.yaml
	promtool check rules ${DISTRIBUTION_FOLDER}/prometheus_alerts.yaml

clean:
	rm -rf ${DISTRIBUTION_FOLDER}
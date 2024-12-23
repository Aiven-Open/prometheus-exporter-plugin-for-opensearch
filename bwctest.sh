#!/bin/bash

set -e

function usage() {
  echo ""
    echo "This script is used to run Backwards Compatibility tests"
    echo "--------------------------------------------------------------------------"
    echo "Usage: $0 [args]"
    echo ""
    echo "Required arguments:"
    echo "None"
    echo ""
    echo -e "-h\tPrint this message."
    echo "--------------------------------------------------------------------------"
}

while getopts ":h" arg; do
    case $arg in
        h)
            usage
            exit 1
            ;;
        ?)
            echo "Invalid option: -${OPTARG}"
            exit 1
            ;;
    esac
done

# Warning!
# This should be done from gradle, see bwcTestSuite task, but that task is skipped.
# TODO: Skipping task ':bwcTestSuite' as it has no source files and no previous output files.
rm -rf src/test/resources/org/opensearch/prometheus-exporter/bwc/prometheus-exporter

./gradlew bwcTestSuite -Dtests.security.manager=false
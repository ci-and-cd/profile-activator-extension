#!/bin/bash
set -e -u

#
# produce documentation site
#

cd "${BASH_SOURCE%/*}/.."

./mvnw.sh javadoc:jar -B -P skip-test

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -f scripts/env.sh ]; then
 set +u
 source scripts/env.sh
 set -u
fi
FINANCE_ACTION="${1:-launch}"
case "$FINANCE_ACTION" in
  acceptance) exec java -cp 'build:libs/gaussdbjdbc.jar' expt.db.finance.Acceptance config/db.properties init ;;
  resume-reports) exec java -cp 'build:libs/gaussdbjdbc.jar' expt.db.finance.Acceptance config/db.properties resume-reports ;;
  verify) exec java -cp 'build:libs/gaussdbjdbc.jar' expt.db.finance.Acceptance config/db.properties verify ;;
  launch) exec java -cp 'build:libs/gaussdbjdbc.jar' expt.db.finance.launch config/db.properties ;;
  reports) exec java -cp 'build:libs/gaussdbjdbc.jar' expt.db.finance.Reports config/db.properties ;;
  provision) exec java -cp 'build:libs/gaussdbjdbc.jar' expt.db.finance.Provision "${2:?database private IP required}" ;;
  permissions) exec java -cp 'build:libs/gaussdbjdbc.jar' expt.db.finance.PermissionCheck "${2:?database private IP required}" ;;
  *) echo 'Actions: provision IP | acceptance | launch | reports | verify | permissions IP'; exit 2 ;;
esac

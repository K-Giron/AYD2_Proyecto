#!/usr/bin/env bash
set -uo pipefail

suite="${1:-all}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
env_file="${PERF_ENV_FILE:-$script_dir/.env.performance}"
results_dir="$script_dir/results"

if [[ ! -f "$env_file" ]]; then
  echo "No existe $env_file. Copie .env.performance.example y ajuste el ambiente." >&2
  exit 2
fi
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker no está disponible en PATH." >&2
  exit 2
fi

load_scripts=(
  load-01-eligibility.js
  load-02-mixed-checkout.js
  load-03-erp-history.js
  load-04-notifications.js
  load-05-state-audit.js
)
stress_scripts=(
  stress-01-progressive-limit.js
  stress-02-checkout-spike.js
  stress-03-credit-vs-payklik.js
  stress-04-notification-backlog.js
  stress-05-recovery.js
)

case "$suite" in
  load) scripts=("${load_scripts[@]}") ;;
  stress) scripts=("${stress_scripts[@]}") ;;
  all) scripts=("${load_scripts[@]}" "${stress_scripts[@]}") ;;
  *) echo "Suite inválida: use load, stress o all." >&2; exit 2 ;;
esac

mkdir -p "$results_dir"
failed=()
for script in "${scripts[@]}"; do
  name="${script%.js}"
  stamp="$(date +%Y%m%d-%H%M%S)"
  summary="$stamp-$name-summary.json"
  log_file="$results_dir/$stamp-$name.log"
  echo "Ejecutando $script"
  docker run --rm \
    --add-host host.docker.internal:host-gateway \
    --env-file "$env_file" \
    -v "$script_dir:/scripts:ro" \
    -v "$results_dir:/results" \
    grafana/k6 run \
    --summary-trend-stats='avg,min,med,p(50),p(90),p(95),p(99),max' \
    --summary-export="/results/$summary" \
    "/scripts/$script" 2>&1 | tee "$log_file"
  result=${PIPESTATUS[0]}
  if [[ $result -ne 0 ]]; then
    failed+=("$script")
  fi
done

if [[ ${#failed[@]} -gt 0 ]]; then
  echo "Fallaron: ${failed[*]}. Revise results/." >&2
  exit 1
fi
echo "Suite $suite finalizada. Evidencia cruda: $results_dir"


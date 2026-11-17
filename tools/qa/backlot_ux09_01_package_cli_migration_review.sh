#!/usr/bin/env bash
# UX09-01: package a migration for REVIEW ONLY using the installed Supabase CLI.
# No hosted project is linked; no network, SQL execution, db push, or migration history writes.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SOURCE="${ROOT}/supabase/prototypes/ux09_01_project_idea_sync_contract_v1.sql"
OUTPUT_ROOT="${1:-${RUNNER_TEMP:-/tmp}/backlot-ux09-01-migration-review}"
STAGE="${OUTPUT_ROOT}/isolated-cli-project"
REVIEW="${OUTPUT_ROOT}/review-artifact"

if [[ ! -f "${SOURCE}" ]]; then
  echo "STOP: reviewed UX09-01 SQL prototype missing" >&2
  exit 2
fi
if ! command -v supabase >/dev/null 2>&1; then
  echo "STOP: install pinned Supabase CLI v2.122.1 before generating this review package" >&2
  exit 2
fi
VERSION="$(supabase --version)"
if [[ "${VERSION}" != "2.122.1" ]]; then
  echo "STOP: reviewed contract was tested against CLI v2.122.1, found ${VERSION}" >&2
  exit 2
fi
if [[ -e "${STAGE}" || -e "${REVIEW}" ]]; then
  echo "STOP: output directory already exists; use a fresh disposable path" >&2
  exit 2
fi
mkdir -p "${STAGE}" "${REVIEW}"

# Create an independent project: never point Supabase CLI at the checked-in
# supabase/migrations directory (which includes unrelated original-app history).
(
  cd "${STAGE}"
  env -u SUPABASE_ACCESS_TOKEN -u SUPABASE_DB_PASSWORD -u SUPABASE_DB_URL \
    supabase init --yes
  env -u SUPABASE_ACCESS_TOKEN -u SUPABASE_DB_PASSWORD -u SUPABASE_DB_URL \
    supabase migration new backlot_ux09_01_creator_project_idea_records
)

shopt -s nullglob
MIGRATIONS=("${STAGE}"/supabase/migrations/*_backlot_ux09_01_creator_project_idea_records.sql)
if [[ ${#MIGRATIONS[@]} -ne 1 ]]; then
  echo "STOP: CLI did not generate exactly one isolated versioned migration" >&2
  exit 3
fi

# Never hand-edit, wrap, or relax the already-disposable-tested source contract.
cp "${SOURCE}" "${MIGRATIONS[0]}"
cmp -s "${SOURCE}" "${MIGRATIONS[0]}" || {
  echo "STOP: packaged migration differs from reviewed source" >&2
  exit 3
}
cp "${MIGRATIONS[0]}" "${REVIEW}/"
{
  echo "UX09-01 LOCAL REVIEW PACKAGE ONLY"
  echo "Supabase CLI: ${VERSION}"
  echo "Generated filename: $(basename "${MIGRATIONS[0]}")"
  echo "Original SQL: supabase/prototypes/ux09_01_project_idea_sync_contract_v1.sql"
  echo "Source SHA256: $(sha256sum "${SOURCE}" | cut -d' ' -f1)"
  echo "Packaged SHA256: $(sha256sum "${MIGRATIONS[0]}" | cut -d' ' -f1)"
  echo "Original repository supabase/migrations NOT USED."
  echo "No Lab/project link, remote SQL, schema change, migration history write or deploy."
  echo "STOP before applying: verify Data API Exposed Schemas and request separate Lab-only DDL approval."
} > "${REVIEW}/REVIEW_ONLY.txt"

echo "Created local-only review package at ${REVIEW}"
cat "${REVIEW}/REVIEW_ONLY.txt"

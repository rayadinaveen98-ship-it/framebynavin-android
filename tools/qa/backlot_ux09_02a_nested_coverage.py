#!/usr/bin/env python3
"""Read-only, exact-span nested Creator workspace export audit.

This is NOT a cloud codec and does not prove end-to-end round-trip semantics.
All source material is local repository text. No dependencies, network or writes.
"""
import argparse
import copy
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "docs/engineering/UX09_02A_NESTED_SERIALIZATION_MANIFEST_20261117.json"
TOP_LEVEL = ROOT / "docs/engineering/UX09_02A_FIELD_COVERAGE_MANIFEST_20261117.json"
EXPECTED_FIELDS = {"CreatorProjectAsset": "location", "CreatorEditVersion": "sourceLocation"}
REQUIRED_CLASSES = {
    "CreatorContentDna", "CreatorContentWorkspace", "CreatorStageWork",
    "CreatorStageChecklistItem", "CreatorStageEvidence", "CreatorStageDecision",
    "CreatorStageTransitionReceipt", "CreatorOutputScript", "CreatorOutputScriptSnapshot",
    "CreatorEditVersion", "CreatorProjectReference", "CreatorChecklistItem",
    "CreatorProjectAsset", "CreatorVariantIdea", "CreatorPublishGateItem",
    "CreatorPublicationEvent", "CreatorDeliverable", "CreatorHookIdea",
    "CreatorTitleIdea", "CreatorScriptBeat", "CreatorScriptStudio",
}

def require(ok, message):
    if not ok:
        raise AssertionError(message)

def class_fields(source, name):
    marker = "data class " + name + "("
    start = source.find(marker)
    require(start >= 0, "Kotlin model absent: " + name)
    end = source.find("\n)", start)
    require(end > start, "Kotlin declaration shape changed for " + name)
    fields = re.findall(r"\bval\s+([A-Za-z_][A-Za-z0-9_]*)\s*:", source[start:end])
    require(fields and len(fields) == len(set(fields)), "Duplicate or empty Kotlin model: " + name)
    return fields

def exact_span(source, entry):
    at = source.find(entry["method_anchor"])
    require(at >= 0, "Encoder method missing: " + entry["id"])
    start = source.find(entry["span_start"], at)
    require(start >= at, "Encoder start missing: " + entry["id"])
    end = source.find(entry["span_end"], start + len(entry["span_start"]))
    require(end > start, "Encoder end missing: " + entry["id"])
    return source[start:end]

def serialized_keys(source, entry):
    section = exact_span(source, entry)
    result = re.findall(r'\.put\(\s*"([a-zA-Z][A-Za-z0-9_]*)"', section)
    require(result, "No nested JSON keys in " + entry["id"])
    require(len(result) == len(set(result)), "Duplicate nested JSON key in " + entry["id"])
    return set(result)

def validate(manifest, overrides=None):
    require(manifest["version"] == 1 and manifest["backend_cap_bytes"] == 16384,
            "Unapproved manifest/server envelope revision")
    require(manifest["feature_gate"] == "OFF", "Sync activation is forbidden")
    entries = manifest["entries"]
    require(len(entries) == 22, "Audited nested entry count changed")
    require(len({e["id"] for e in entries}) == len(entries), "Duplicate nested IDs")
    require({e["kotlin_model"] for e in entries} == REQUIRED_CLASSES,
            "Incomplete or substituted nested class coverage")
    require(sum(e["kotlin_model"] == "CreatorVariantIdea" for e in entries) == 2,
            "Title/thumbnail variant encoders must both be covered")
    sources = {}
    def read(path):
        require(path.startswith("app/src/main/java/com/framebynavin/app/data/") and
                path.endswith(".kt") and ".." not in path,
                "Unexpected Kotlin source path in nested audit")
        if path not in sources:
            sources[path] = (overrides or {}).get(path) or (ROOT/path).read_text(encoding="utf-8")
        return sources[path]
    total = 0
    for entry in entries:
        label = entry["id"]
        fields = class_fields(read(entry["model_path"]), entry["kotlin_model"])
        expected = entry["fields"]
        require(fields == expected, "MODEL FIELD DRIFT " + label
                + " actual=" + str(fields) + " reviewed=" + str(expected))
        implicit = entry["implicit_from_parent"]
        require(len(set(implicit)) == len(implicit), "Duplicate implicit field: " + label)
        require(set(implicit).issubset(set(fields)), "Implicit field was removed: " + label)
        encoded = serialized_keys(read(entry["encoder_path"]), entry)
        require(encoded == (set(fields) - set(implicit)), "NESTED ENCODER FIELD DRIFT " + label
                + " missing=" + str(sorted((set(fields)-set(implicit))-encoded))
                + " unreviewed=" + str(sorted(encoded-(set(fields)-set(implicit)))))
        total += len(fields)
    studio = next(e for e in entries if e["id"] == "CreatorScriptStudio")
    require(studio["implicit_from_parent"] == ["projectId"],
            "CreatorScriptStudio.projectId must be reconstructed only from parent Project")
    require({e["kotlin_model"]: e["fields"] for e in entries}["CreatorProjectAsset"].count("location") == 1,
            "Project asset local path must be reviewed")
    require({e["kotlin_model"]: e["fields"] for e in entries}["CreatorEditVersion"].count("sourceLocation") == 1,
            "Edit version local path must be reviewed")
    require("content:// or file paths" in " ".join(manifest["hard_blocks"]),
            "Location and file safety warning missing")
    require(total == 143, "Audited field count changed")
    top = json.loads(TOP_LEVEL.read_text(encoding="utf-8"))
    require(top["hard_gates"]["feature_gate"] == "OFF", "Top level gate no longer OFF")
    require("workspace" in top["wire_domains"]["creator_project"]["block_until_design"],
            "Recursive coverage is NOT permission to enable workspace syncing")
    return len(entries), len(REQUIRED_CLASSES), total

def selftest(manifest):
    # All mutations are in-memory; source files are untouched.
    checks = 0
    clone = copy.deepcopy(manifest)
    clone["entries"][0]["fields"].append("unreviewedField")
    try:
        validate(clone)
    except AssertionError:
        checks += 1
    clone = copy.deepcopy(manifest)
    clone["entries"] = [e for e in clone["entries"] if e["id"] != "CreatorDeliverable"]
    try:
        validate(clone)
    except AssertionError:
        checks += 1
    clone = copy.deepcopy(manifest)
    clone["entries"][-1]["implicit_from_parent"] = []
    try:
        validate(clone)
    except AssertionError:
        checks += 1
    target = next(e for e in manifest["entries"] if e["id"] == "CreatorProjectAsset")
    file = target["encoder_path"]
    old = (ROOT/file).read_text(encoding="utf-8")
    require('.put("location", asset.location)' in old,
            "Expected serializer fixture missing; update controlled mutation test")
    bad = old.replace('.put("location", asset.location)',
                      '.put("missingLocation", asset.location)', 1)
    try:
        validate(manifest, {file:bad})
    except AssertionError:
        checks += 1
    require(checks == 4, "Synthetic tamper/selftest incomplete")
    return checks

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    count, classes, fields = validate(manifest)
    print(f"PASS: nested Creator export audit: {count} paths, {classes} models, {fields} reviewed fields")
    print("Audit only; NO cloud conversion, no actual upload, no real data or Android app modifications")
    if args.selftest:
        print(f"PASS: {selftest(manifest)} fail-closed synthetic mutation checks")

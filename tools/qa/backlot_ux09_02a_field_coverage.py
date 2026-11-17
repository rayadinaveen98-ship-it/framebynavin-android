#!/usr/bin/env python3
"""UX09-02A audit-only Project/Idea model field-coverage fence.

Read-only Python stdlib check. Never creates records, alters Android files, connects
to Supabase, or claims to be a production codec. Regex is intentionally scoped to
the current Kotlin declaration/encoder style; unusual syntax fails closed.
"""
import argparse
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "docs/engineering/UX09_02A_FIELD_COVERAGE_MANIFEST_20261117.json"
PARTITIONS = ("portable_candidate", "device_local", "block_until_design")

def require(condition, message):
    if not condition:
        raise ValueError(message)

def declared_fields(kotlin_source, class_name):
    marker = "data class " + class_name + "("
    at = kotlin_source.find(marker)
    require(at >= 0, "MISSING Kotlin model: " + class_name)
    end = kotlin_source.find("\n)", at)
    require(end > at, "UNRECOGNIZED Kotlin model declaration: " + class_name)
    names = re.findall(r"\bval\s+([A-Za-z_][A-Za-z0-9_]*)\s*:", kotlin_source[at:end])
    require(len(names) == len(set(names)) and len(names) > 0, "DUPLICATE/EMPTY Kotlin model fields")
    return set(names)

def encoded_keys(kotlin_source, method_marker):
    at = kotlin_source.find(method_marker)
    require(at >= 0, "MISSING Kotlin export encoder: " + method_marker)
    end = kotlin_source.find("return array.toString()", at)
    require(end > at, "UNRECOGNIZED Kotlin encoder body")
    keys = re.findall(r'\.put\("([A-Za-z_][A-Za-z0-9_]*)"', kotlin_source[at:end])
    require(len(keys) == len(set(keys)) and len(keys) > 0,
            "DUPLICATE/EMPTY Kotlin top-level export keys")
    return set(keys)

def coverage(name, manifest_section, model_source, store_source=None):
    actual = declared_fields(model_source, manifest_section["model_class"]) if "model_class" in manifest_section else declared_fields(model_source, name)
    partitions = {group: manifest_section[group] for group in PARTITIONS}
    joined = [field for group in PARTITIONS for field in partitions[group]]
    require(all(isinstance(x, str) and x for x in joined), "BLANK field name: " + name)
    require(len(joined) == len(set(joined)), "DOUBLE-CLASSIFIED field in " + name)
    expected = set(joined)
    require(actual == expected, "MODEL FIELD DRIFT " + name
            + ": unclassified=" + str(sorted(actual - expected))
            + " vanished=" + str(sorted(expected - actual)))
    if store_source is not None:
        exported = encoded_keys(store_source, manifest_section["store_encoder"])
        require(exported == actual, "LOCAL EXPORT FIELD DRIFT " + name
                + ": missing=" + str(sorted(actual - exported))
                + " extra=" + str(sorted(exported - actual)))
    return len(actual)

def verify(manifest):
    require(manifest["manifest_version"] == 1, "Unexpected manifest version")
    require(manifest["hard_gates"]["server_payload_json_utf8_max_bytes"] == 16_384,
            "Server payload bound changed without review")
    require(manifest["hard_gates"]["feature_gate"] == "OFF", "SYNC MUST REMAIN OFF")
    require(manifest["hard_gates"]["real_app_writers"] == "NOT connected",
            "Unsafe app writer activation")
    p = manifest["wire_domains"]["creator_project"]
    i = manifest["wire_domains"]["creator_idea"]
    w = manifest["nested_audit"]["CreatorContentWorkspace"]
    counts = {}
    for name, spec in (("creator_project", p), ("creator_idea", i)):
        model = (ROOT / spec["model_path"]).read_text(encoding="utf-8")
        store = (ROOT / spec["store_path"]).read_text(encoding="utf-8")
        counts[name] = coverage(name, spec, model, store)
    workspace = (ROOT / w["model_path"]).read_text(encoding="utf-8")
    counts["CreatorContentWorkspace"] = coverage("CreatorContentWorkspace", w, workspace)
    require("workspace" in p["block_until_design"], "Workspace must not silently sync")
    require("alarmSoundUri" in p["device_local"], "Device alarm URI must not sync")
    require("audioLocalPath" in i["device_local"], "Voice Idea local file path must not sync")
    require("projectTaskId" in i["block_until_design"], "Legacy linked task IDs need mapping")
    require("audioRemoteUrl" in i["block_until_design"], "Unvetted remote media URL must be blocked")
    require("script" in w["block_until_design"] and
            "assets" in w["block_until_design"], "Nested script/assets need explicit policy")
    require("trashedAtMillis" in i["portable_candidate"],
            "Reversible Idea trash state must not be treated as irreversible tombstone")
    require(counts == {"creator_project":47, "creator_idea":26, "CreatorContentWorkspace":17},
            "Audited model size changed: update versioned manifest after review")
    return counts

def selftest(manifest):
    # Synthetic in-memory mutations: no source file is edited.
    p = manifest["wire_domains"]["creator_project"]
    source = (ROOT / p["model_path"]).read_text(encoding="utf-8")
    store = (ROOT / p["store_path"]).read_text(encoding="utf-8")
    caught = 0
    trials = [
        (source.replace("val title: String,", "val title: String,\n    val unreviewedField: String = \"\",", 1), store),
        (source, store.replace('.put("title", task.title)', '.put("missingTitle", task.title)', 1)),
    ]
    for model, local_store in trials:
        try:
            coverage("synthetic_failure", p, model, local_store)
        except ValueError:
            caught += 1
    require(caught == len(trials), "Guard failed to detect synthetic model/export drift")
    unsafe = json.loads(json.dumps(manifest))
    unsafe["wire_domains"]["creator_project"]["portable_candidate"].append("alarmSoundUri")
    try:
        verify(unsafe)
    except ValueError:
        caught += 1
    require(caught == len(trials) + 1, "Guard failed to catch duplicate classification")
    return caught

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    sizes = verify(manifest)
    print("PASS UX09-02A source model/export field manifest: "
          + ", ".join(f"{k}={v}" for k,v in sizes.items()))
    print("NOT A CLOUD CODEC. BLOCKED field groups are intentional and feature remains OFF.")
    if args.selftest:
        print("PASS " + str(selftest(manifest)) + " synthetic fail-closed drift checks")

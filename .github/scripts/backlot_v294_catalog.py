#!/usr/bin/env python3
"""Read-only V294 audit of production UI text and UI density.
Creates candidate catalogs only: not a runtime reachability/screenshot audit.
Never reads app-private user data, secrets, tokens or cloud contents.
"""
import argparse
from collections import Counter
import csv
import json
from pathlib import Path
import re

DISPLAY_CALLS={"Text","BasicText","V144CategoryCard","V144ActionCard",
 "V144SettingsPage","V144SettingsNote","V144SectionTitle","PEmptyState",
 "V23InfoRow","V144PermissionRow","setContentTitle","setContentText",
 "setTicker","setMessage","makeText","showSnackbar"}
ASSIGNMENT=re.compile(r"\b(?:label|title|subtitle|description|contentDescription|placeholder|hint|message|headline|body)\s*=\s*$",re.I)
SENSITIVE=re.compile(r"secret|credential|api.?key|access.?token|refresh.?token|password|private.?key|authorization|bearer",re.I)
INTERNAL=re.compile(r"^(https?://|[a-z0-9_.-]+@[a-z0-9_.-]+|[0-9a-f]{24,}|[0-9a-f]{8}-[0-9a-f-]{27,})",re.I)
MEANINGFUL=re.compile(r"\b(backup|restore|sync|delete|erase|privacy|photo|alarm|permission|recovery)\b",re.I)
FONT=re.compile(r"\bfontSize\s*=\s*(\d+(?:\.\d+)?)\.sp")
SAMPLE=re.compile(r"\b\w*(?:Seed|Demo|Sample|Preset|Placeholder|Mock)\w*\b|\b(?:loadOrSeed|defaultSlots|isLegacyTask|withoutUntouchedDemos)\b",re.I)
COMP=re.compile(r"@Composable\s+(?:(?:internal|private|public|inline)\s+)*fun\s+(\w+)",re.S)
ACTIVITY=re.compile(r"\bclass\s+(\w+Activity)\b")

def lex(src):
    """Record Kotlin strings and mask comments/string bodies for context."""
    mask=list(src); tokens=[]; n=len(src); i=0
    while i<n:
        start=i
        if src.startswith("//",i):
            end=src.find("\n",i); i=end if end>=0 else n
        elif src.startswith("/*",i):
            level=1;i+=2
            while i<n and level:
                if src.startswith("/*",i): level+=1;i+=2
                elif src.startswith("*/",i): level-=1;i+=2
                else:i+=1
        elif src.startswith('"""',i):
            end=src.find('"""',i+3)
            if end<0:end=n
            tokens.append((i,src[i+3:end]));i=min(n,end+3)
        elif src[i]=='"':
            i+=1;parts=[]
            while i<n:
                if src[i]=="\\" and i+1<n:parts.append(src[i:i+2]);i+=2
                elif src[i]=='"':i+=1;break
                else:parts.append(src[i]);i+=1
            tokens.append((start,"".join(parts)))
        elif src[i]=="'":
            i+=1
            while i<n:
                if src[i]=="\\" and i+1<n:i+=2
                elif src[i]=="'":i+=1;break
                else:i+=1
        else:
            i+=1;continue
        for j in range(start,i):
            if mask[j]!="\n":mask[j]=" "
    return tokens,"".join(mask)

def ancestors(mask,positions):
    frames=[];cursor=0;results=[]
    for pos in positions:
        while cursor<pos:
            if mask[cursor]=="(":
                match=re.search(r"([A-Za-z_]\w*(?:\.[A-Za-z_]\w*)?)\s*$",mask[max(0,cursor-130):cursor])
                frames.append(match.group(1).split(".")[-1] if match else "")
            elif mask[cursor]==")" and frames:frames.pop()
            cursor+=1
        results.append(tuple(f for f in frames if f))
    return results

def classify(value,prior,calls):
    t=value.strip()
    if not t or SENSITIVE.search(prior[-130:]) or len(t)>4000 or INTERNAL.search(t):
        return "excluded"
    if any(c in DISPLAY_CALLS for c in calls):return "direct-ui"
    if ASSIGNMENT.search(prior[-90:]):return "possible-ui"
    if MEANINGFUL.search(t) and len(t)>22 and " " in t:return "possible-ui"
    return "other"

def write_csv(path,fields,rows):
    with path.open("w",encoding="utf-8",newline="") as f:
        writer=csv.DictWriter(f,fieldnames=fields,extrasaction="ignore")
        writer.writeheader();writer.writerows(rows)

def audit(root,out):
    source=root/"app/src/main"
    if not source.is_dir():raise ValueError("Missing production app/src/main sources")
    out.mkdir(parents=True,exist_ok=True)
    copy=[];files=[];micro=[];samples=[];composables=[];stats=Counter()
    kt=sorted(source.rglob("*.kt"))
    xml=sorted(source.rglob("*.xml"))
    for file in kt:
        rel=file.relative_to(root).as_posix()
        code=file.read_text(encoding="utf-8")
        tokens,mask=lex(code)
        calls=ancestors(mask,[item[0] for item in tokens])
        file_counts=Counter()
        for (pos,value),stack in zip(tokens,calls):
            group=classify(value,mask[max(0,pos-190):pos],stack)
            file_counts[group]+=1;stats[group]+=1
            if group not in ("direct-ui","possible-ui"):continue
            copy.append({"file":rel,"line":code.count("\n",0,pos)+1,"category":group,
                "call_stack":" > ".join(stack[-5:]),"dynamic":"yes" if "$" in value else "no",
                "chars":len(value),"text":value.replace("\n",r"\n")})
        for match in FONT.finditer(mask):
            size=float(match.group(1))
            if size<12:
                micro.append({"file":rel,"line":code.count("\n",0,match.start())+1,
                              "sp":size,"severity":"P1" if size<11 else "P2"})
        for match in SAMPLE.finditer(mask):
            samples.append({"file":rel,"line":code.count("\n",0,match.start())+1,
                            "token":match.group(0)})
        for regex,kind in ((COMP,"composable"),(ACTIVITY,"activity")):
            for match in regex.finditer(mask):
                composables.append({"file":rel,"line":code.count("\n",0,match.start())+1,
                                    "kind":kind,"name":match.group(1)})
        files.append({"file":rel,"lines":code.count("\n")+1,"bytes":len(code.encode("utf-8")),
                      "literal_count":len(tokens),"direct_ui":file_counts["direct-ui"],
                      "possible_ui":file_counts["possible-ui"],
                      "other_unclassified":file_counts["other"],
                      "small_font":sum(row["file"]==rel for row in micro)})
    # Include resources, but do not dump unclassified code literals (may contain secrets).
    for path in xml:
        if "/values/" not in path.as_posix():continue
        src=path.read_text("utf-8")
        for match in re.finditer(r'<string\s+name=["\x27]([^"\x27]+)["\x27][^>]*>(.*?)</string>',src,re.S):
            value=match.group(2).strip()
            if value and not SENSITIVE.search(match.group(1)):
                copy.append({"file":path.relative_to(root).as_posix(),
                    "line":src.count("\n",0,match.start())+1,"category":"xml-string",
                    "call_stack":"resources","dynamic":"yes" if "%" in value else "no",
                    "chars":len(value),"text":value.replace("\n",r"\n")})
    copy.sort(key=lambda row:(row["file"],row["line"],row["text"]))
    repeated=Counter(row["text"] for row in copy)
    duplicates=[{"text":s,"count":n} for s,n in repeated.most_common() if n>=4 and len(s)>3][:100]
    long=[row for row in copy if row["chars"]>170]
    write_csv(out/"copy-candidates.csv",
              ["file","line","category","call_stack","dynamic","chars","text"],copy)
    write_csv(out/"source-file-summary.csv",
              ["file","lines","bytes","literal_count","direct_ui","possible_ui","other_unclassified","small_font"],files)
    write_csv(out/"microtext.csv",["file","line","sp","severity"],micro)
    write_csv(out/"sample-source-review.csv",["file","line","token"],samples)
    write_csv(out/"composable-and-activity-index.csv",["file","line","kind","name"],composables)
    write_csv(out/"long-copy-review.csv",
              ["file","line","category","call_stack","dynamic","chars","text"],long)
    write_csv(out/"repeated-copy-review.csv",["text","count"],duplicates)
    report={"scope":"production source candidates only","kotlin_files":len(kt),
      "xml_files":len(xml),"direct_ui_candidates":stats["direct-ui"],
      "possible_ui_candidates":stats["possible-ui"],
      "copy_candidates_total":len(copy),"font_declarations_under_12sp":len(micro),
      "font_declarations_under_11sp":sum(x["sp"]<11 for x in micro),
      "potential_sample_references":len(samples),
      "composable_functions":sum(x["kind"]=="composable" for x in composables),
      "android_activities":sum(x["kind"]=="activity" for x in composables),
      "long_copy_items_over_170_chars":len(long),
      "limitations":["All production source, including possibly unreachable older UI components.",
                     "Only candidate visible strings; dynamic and backend text require human review.",
                     "No screenshots, emulator reachability test or confirmation of a real seeded record.",
                     "No app database, accounts, photos, user data or signing keys were read or modified."]}
    (out/"summary.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    (out/"README.md").write_text(
      "# Backlot V294 read-only copy/UX source audit\n\n"
      "Review the generated CSVs, then cross-check *reachable* routes and screenshots. "
      "Not a completed rendered UI audit. No production sources or owner data changed.\n",
      encoding="utf-8")
    return report

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--out",type=Path,default=Path("output/v294-source-audit"))
    parser.add_argument("--repo",type=Path,default=Path("."))
    args=parser.parse_args()
    print(json.dumps(audit(args.repo.resolve(),args.out.resolve()),indent=2))

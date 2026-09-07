#!/usr/bin/env python3
"""Reconstruct the exact Alpha23 generated source and apply the v1.8.1 patch."""
import base64
import hashlib
import io
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import tempfile
import urllib.parse
import urllib.request
import zipfile
import lzma

ROOT = Path(__file__).resolve().parents[2]
PARTS = ROOT / 'hardening' / 'rc2'
ARTIFACT_ID = 9995482152
ARCHIVE_SHA = '791744f1eb6d55ec4dc50176410958adfaf5a3650bfbcaf4e99fbde3b917c583'
TAR_SHA = '7d35164839468b87f26aba80e268aad8c2d9a7d50982249618ba0bc8bf8a475d'
PATCH_SHA = 'a5b92d5614ace5f45dd013987365185f74d086428851b9f324a897d19211e7cb'
PATCH_RAW_SHA = 'f30ad6dedc921c072455f1778f993b66081caa4c286584beb60be772b9821c45'

class SafeArtifactRedirect(urllib.request.HTTPRedirectHandler):
    """Do not send GitHub's bearer token to signed artifact storage URLs."""
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        target = urllib.parse.urlsplit(newurl)
        if target.scheme != 'https':
            raise RuntimeError('Artifact redirect must use HTTPS')
        redirected = super().redirect_request(req, fp, code, msg, headers, newurl)
        if redirected is not None and target.netloc != urllib.parse.urlsplit(req.full_url).netloc:
            for name in list(redirected.headers):
                if name.lower() in ('authorization', 'proxy-authorization', 'x-github-api-version'):
                    del redirected.headers[name]
            redirected.add_header('Accept', 'application/octet-stream')
        return redirected

def verified(data, expected, label):
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected:
        raise RuntimeError(f'{label} SHA256 mismatch: {actual}')
    print(f'{label}: {actual}')
    return data

def safe_extract(tar, dest):
    dest = dest.resolve()
    for member in tar.getmembers():
        target = (dest / member.name).resolve()
        if not target.is_relative_to(dest) or member.issym() or member.islnk() or member.isdev():
            raise RuntimeError('Unsafe archive member: ' + member.name)
        if member.name.startswith('./'):
            raise RuntimeError('Unexpected archive path')
    tar.extractall(dest, filter='data')

def main():
    token = os.environ.get('GITHUB_TOKEN', '')
    if not token:
        raise RuntimeError('GITHUB_TOKEN is required to retrieve the authoritative audit artifact')
    endpoint = f'https://api.github.com/repos/rayadinaveen98-ship-it/framebynavin-android/actions/artifacts/{ARTIFACT_ID}/zip'
    req = urllib.request.Request(endpoint, headers={
        'Authorization': 'Bearer ' + token,
        'Accept': 'application/vnd.github+json',
        'X-GitHub-Api-Version': '2022-11-28',
        'User-Agent': 'FrameByNavin-Hardening-CI',
    })
    opener = urllib.request.build_opener(SafeArtifactRedirect())
    with opener.open(req, timeout=180) as response:
        archive = verified(response.read(), ARCHIVE_SHA, 'Alpha23 audit artifact')
    with zipfile.ZipFile(io.BytesIO(archive)) as z:
        names = z.namelist()
        if names != ['FrameByNavin-v1.8.0-product-alpha23-audit-source.tar.gz']:
            raise RuntimeError('Unexpected audit artifact contents')
        tar_bytes = verified(z.read(names[0]), TAR_SHA, 'Alpha23 generated source')
    compressed = b''.join(base64.b64decode((PARTS / f'part-{i:02d}.txt').read_text().strip(), validate=True) for i in range(1, 10))
    verified(compressed, PATCH_SHA, 'Hardening patch XZ')
    patch = verified(lzma.decompress(compressed), PATCH_RAW_SHA, 'Hardening patch')
    with tempfile.TemporaryDirectory() as td:
        source = Path(td)
        with tarfile.open(fileobj=io.BytesIO(tar_bytes), mode='r:gz') as tar:
            safe_extract(tar, source)
        for name in ['app/src', 'app/build.gradle.kts', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties']:
            src = source / name
            dst = ROOT / name
            if not src.exists():
                raise RuntimeError('Missing generated source: ' + name)
            if dst.is_dir():
                shutil.rmtree(dst)
            elif dst.exists():
                dst.unlink()
            if src.is_dir():
                shutil.copytree(src, dst)
            else:
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, dst)
    patch_path = ROOT / 'hardening' / 'rc2' / 'candidate.patch'
    patch_path.write_bytes(patch)
    subprocess.run(['git', 'apply', '--check', str(patch_path)], cwd=ROOT, check=True)
    subprocess.run(['git', 'apply', str(patch_path)], cwd=ROOT, check=True)
    gradle = (ROOT / 'app/build.gradle.kts').read_text()
    if 'versionCode = 64' not in gradle or 'versionName = "1.8.1-hardening-rc1"' not in gradle:
        raise RuntimeError('Hardening version gate failed')
    if 'versionCode = 63' in gradle or '1.7.5-original-frames' in gradle:
        raise RuntimeError('Stale source version detected')
    expected = [
        'app/src/main/java/com/framebynavin/app/data/CreatorDataGate.kt',
        'app/src/main/java/com/framebynavin/app/data/CreatorPublicationEngine.kt',
        'app/src/main/java/com/framebynavin/app/data/CreatorDeltaEngine.kt',
        'app/src/main/java/com/framebynavin/app/data/CreatorHeroArchive.kt',
        'app/src/main/java/com/framebynavin/app/ui/V181PublicationDialog.kt',
        'app/src/test/java/com/framebynavin/app/data/CreatorHardeningV181Test.kt',
    ]
    for name in expected:
        if not (ROOT / name).is_file():
            raise RuntimeError('Missing hardening source: ' + name)
    print('Exact Alpha23 source + complete RC2 patch successfully materialized')

if __name__ == '__main__':
    main()

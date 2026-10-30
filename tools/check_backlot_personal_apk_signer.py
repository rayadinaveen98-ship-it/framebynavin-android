#!/usr/bin/env python3
"""Check the APK v2 signing certificate against the verified personal-stable signer.

This checks certificate identity, not signature cryptography. Android's package
installer performs signature validation when installing an update.
"""
import argparse
import hashlib
import struct
import sys
from pathlib import Path

SCHEME_V2 = 0x7109871A
MAGIC = b'APK Sig Block 42'


def u32(data: bytes, offset: int) -> int:
    if offset < 0 or offset + 4 > len(data):
        raise ValueError('APK signing block truncated (u32)')
    return struct.unpack_from('<I', data, offset)[0]


def u64(data: bytes, offset: int) -> int:
    if offset < 0 or offset + 8 > len(data):
        raise ValueError('APK signing block truncated (u64)')
    return struct.unpack_from('<Q', data, offset)[0]


def sequence(data: bytes, offset: int) -> tuple[bytes, int]:
    size = u32(data, offset)
    end = offset + 4 + size
    if end > len(data):
        raise ValueError('Invalid length-prefixed signing data')
    return data[offset + 4:end], end


def signer_hash(apk: bytes) -> str:
    eocd = -1
    for cursor in range(len(apk) - 22, max(-1, len(apk) - 65558), -1):
        if apk[cursor:cursor+4] == b'PK\x05\x06' and cursor + 22 + struct.unpack_from('<H', apk, cursor+20)[0] == len(apk):
            eocd = cursor
            break
    if eocd < 0:
        raise ValueError('Not a valid ZIP APK EOCD')
    central = u32(apk, eocd + 16)
    footer = central - 24
    if footer < 0 or apk[central - 16:central] != MAGIC:
        raise ValueError('Missing APK Signing Block')
    block_size = u64(apk, footer)
    beginning = central - block_size - 8
    if beginning < 0 or u64(apk, beginning) != block_size:
        raise ValueError('APK Signing Block length mismatch')
    offset = beginning + 8
    signatures = []
    while offset < footer:
        part_length = u64(apk, offset)
        end = offset + 8 + part_length
        if part_length < 4 or end > footer:
            raise ValueError('Malformed APK signature scheme entry')
        scheme = u32(apk, offset + 8)
        if scheme == SCHEME_V2:
            content = apk[offset + 12:end]
            all_signers, _ = sequence(content, 0)
            signer, after = sequence(all_signers, 0)
            if after != len(all_signers):
                raise ValueError('Unexpected additional APK signers')
            signed_data, _ = sequence(signer, 0)
            _digests, certificates_offset = sequence(signed_data, 0)
            certificates, _ = sequence(signed_data, certificates_offset)
            certificate_der, after_cert = sequence(certificates, 0)
            if after_cert != len(certificates):
                raise ValueError('Unexpected additional certificate chains')
            signatures.append(hashlib.sha256(certificate_der).hexdigest())
        offset = end
    if offset != footer or len(signatures) != 1:
        raise ValueError(f'Expected exactly one APK v2 signer, got {len(signatures)}')
    return signatures[0]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--expected-cert-sha256', required=True)
    args = parser.parse_args()
    try:
        data = args.apk.read_bytes()
        cert_sha = signer_hash(data)
        apk_sha = hashlib.sha256(data).hexdigest()
        print(f'APK SHA-256: {apk_sha}')
        print(f'APK v2 signer certificate SHA-256: {cert_sha}')
        if cert_sha.lower() != args.expected_cert_sha256.lower():
            print('ERROR: Signer differs from verified V195.5 personal-stable APK', file=sys.stderr)
            return 1
        print('PASS: Signer matches V195.5 personal-stable certificate')
    except (OSError, ValueError, struct.error) as error:
        print(f'ERROR: Signing inspection failed: {error}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())

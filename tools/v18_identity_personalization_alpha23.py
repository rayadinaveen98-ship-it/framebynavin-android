from pathlib import Path
import base64
import gzip
import hashlib

ROOT = Path(__file__).resolve().parent.parent
CHUNKS = [('v18_identity_personalization_alpha23_payload_01.txt', '9e5f3761c510519b4b9a783339ea55be60e91293fbd3ef7e9118f1246eed70b1'), ('v18_identity_personalization_alpha23_payload_02.txt', '7d2ea26f5ad6c55d4a0e6a2e4ffc8f60dc86e6e904fc953f8d9a2e9f62c9a0a1'), ('v18_identity_personalization_alpha23_payload_03.txt', '2b5cc76964ee3e3f1b072279567d3bb686a6d68194c76303689a226f66f8d4e2'), ('v18_identity_personalization_alpha23_payload_04.txt', 'd35c3b5b3bce90a95643f4061e291b546ca570b3df8cfd6b35c612f0e0e13ad0'), ('v18_identity_personalization_alpha23_payload_05.txt', 'c2f78ed540aaf668a28dbcdd81b72a3933c558420b792e784118d7abfc8a8277'), ('v18_identity_personalization_alpha23_payload_06.txt', 'a3847c61463dcd5c445ffeb14765eebc45aff98e52410bccce969eea07382797'), ('v18_identity_personalization_alpha23_payload_07.txt', 'c08d89a0761b210b4ae044b8bfe6f4987e692e6e8c90e317bad7065c861cfc32'), ('v18_identity_personalization_alpha23_payload_08.txt', '123f41f4a81160440380f74ed8c9a80e09f21c8c15d253c32755c1ad835568f8'), ('v18_identity_personalization_alpha23_payload_09.txt', 'e41615144f2c6668b9386b11f337d296068e8c344b191d4ba2ba816313a9835e'), ('v18_identity_personalization_alpha23_payload_10.txt', '4d414f87e4793e153937df565db475400d4408c87d0979fbc4250863551f8809'), ('v18_identity_personalization_alpha23_payload_11.txt', '6adf4baf375f649df95bd4bbee0205acaaba5986fd2afe6eda4bd7a9f411e43e'), ('v18_identity_personalization_alpha23_payload_12.txt', '8c0b2847b730d4b110173738711adfae0aa8c0de7a7ce3769e5498839984ce66'), ('v18_identity_personalization_alpha23_payload_13.txt', '2683d4f0acc2c685197cb9de3dd6de8def0e5cae2f6ff651860c19afd4149a9d'), ('v18_identity_personalization_alpha23_payload_14.txt', '2faa8a58b616286ae31ba4c1605d9d2d7c9755657e703c87c88568f74e212945'), ('v18_identity_personalization_alpha23_payload_15.txt', '276ba3fb6e94a4c14d673985976705cce1c7cd673e7897e8f12dbf9aa4521571'), ('v18_identity_personalization_alpha23_payload_16.txt', '15e1cce364c434f0d3e0e828381829d1a2f54c558a5c2d0af450907dd7718e93')]
EXPECTED_SCRIPT_SHA = 'd8e0439001c1684145ddbad1fbfcfaab8aadcef28dbfb3057c368665d2c7ce11'

parts = []
for name, expected in CHUNKS:
    path = ROOT / "tools" / name
    text = path.read_text().strip()
    actual = hashlib.sha256(text.encode()).hexdigest()
    if actual != expected:
        raise SystemExit(f"Alpha23 payload chunk checksum mismatch: {name}: {actual} != {expected}")
    parts.append(text)
encoded = "".join(parts)
try:
    script = gzip.decompress(base64.b64decode(encoded)).decode("utf-8")
except Exception as exc:
    raise SystemExit(f"Alpha23 payload decode failed: {exc}") from exc
actual_script_sha = hashlib.sha256(script.encode()).hexdigest()
if actual_script_sha != EXPECTED_SCRIPT_SHA:
    raise SystemExit(f"Alpha23 decoded script checksum mismatch: {actual_script_sha} != {EXPECTED_SCRIPT_SHA}")
exec(compile(script, "<alpha23-payload>", "exec"), {"__name__": "__main__"})

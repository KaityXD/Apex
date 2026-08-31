#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path


def find_repo_root() -> Path:
    cur = Path(__file__).resolve().parent
    for _ in range(4):
        if (cur / "build.gradle.kts").exists() or (cur / ".git").exists():
            return cur
        cur = cur.parent
    return Path(__file__).resolve().parent.parent


def get_git_info(root: Path) -> dict:
    info = {
        "hash": "unknown",
        "branch": "main",
        "message": "Manual build",
        "author": "Kat",
        "date": ""
    }
    try:
        info["hash"] = subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"],
            cwd=root, stderr=subprocess.DEVNULL
        ).decode().strip()
        info["branch"] = subprocess.check_output(
            ["git", "branch", "--show-current"],
            cwd=root, stderr=subprocess.DEVNULL
        ).decode().strip() or "main"
        info["message"] = subprocess.check_output(
            ["git", "log", "-1", "--pretty=%s"],
            cwd=root, stderr=subprocess.DEVNULL
        ).decode().strip()
        info["author"] = subprocess.check_output(
            ["git", "log", "-1", "--pretty=%an"],
            cwd=root, stderr=subprocess.DEVNULL
        ).decode().strip()
    except Exception:
        pass
    return info


def find_latest_jar(root: Path) -> Path | None:
    libs_dir = root / "build" / "libs"
    if not libs_dir.exists():
        return None
    jars = [j for j in libs_dir.glob("*.jar") if not j.name.endswith("-sources.jar")]
    if jars:
        jars.sort(key=lambda p: p.stat().st_mtime, reverse=True)
        return jars[0]
    return None


def send_to_webhook(webhook_url: str, jar_path: Path, git_info: dict, notes: str = None) -> bool:
    with open(jar_path, "rb") as f:
        jar_bytes = f.read()

    file_size_mb = len(jar_bytes) / (1024 * 1024)
    md5_hash = hashlib.md5(jar_bytes).hexdigest()
    sha256_hash = hashlib.sha256(jar_bytes).hexdigest()
    filename = jar_path.name

    description_parts = []
    if notes:
        description_parts.append(f"**Notes / Changes:**\n{notes}\n")
    description_parts.append(f"**Latest Commit:** `{git_info['message']}`\n**Author:** {git_info['author']}")

    payload = {
        "username": "Apex Build Bot",
        "avatar_url": "https://cdn.discordapp.com/emojis/1206603099955728444.webp",
        "content": f"New Apex Anti-Cheat Build Available! (`{git_info['hash']}` on `{git_info['branch']}`)",
        "embeds": [
            {
                "title": f"Apex Anti-Cheat — Build `{git_info['hash']}`",
                "description": "\n".join(description_parts),
                "color": 0x00F2FE,
                "fields": [
                    {"name": "Artifact", "value": f"`{filename}`", "inline": True},
                    {"name": "Size", "value": f"`{file_size_mb:.2f} MB`", "inline": True},
                    {"name": "Branch", "value": f"`{git_info['branch']}`", "inline": True},
                    {"name": "Platform", "value": "`Spigot / Paper / Folia (1.8 - 26.2+)`", "inline": True},
                    {"name": "MD5 Checksum", "value": f"`{md5_hash}`", "inline": False},
                    {"name": "SHA-256", "value": f"`{sha256_hash}`", "inline": False},
                ],
                "footer": {
                    "text": "Apex v1.0.0"
                }
            }
        ]
    }

    boundary = f"----WebKitFormBoundary{uuid.uuid4().hex}"
    body = bytearray()
    body.extend(f"--{boundary}\r\n".encode("utf-8"))
    body.extend(b"Content-Disposition: form-data; name=\"payload_json\"\r\n")
    body.extend(b"Content-Type: application/json\r\n\r\n")
    body.extend(json.dumps(payload).encode("utf-8"))
    body.extend(b"\r\n")
    body.extend(f"--{boundary}\r\n".encode("utf-8"))
    body.extend(f"Content-Disposition: form-data; name=\"files[0]\"; filename=\"{filename}\"\r\n".encode("utf-8"))
    body.extend(b"Content-Type: application/java-archive\r\n\r\n")
    body.extend(jar_bytes)
    body.extend(b"\r\n")
    body.extend(f"--{boundary}--\r\n".encode("utf-8"))

    req = urllib.request.Request(
        webhook_url,
        data=bytes(body),
        headers={
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "User-Agent": "Apex-Build-Uploader/1.0"
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return resp.status in (200, 204)
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="ignore")
        print(f"[send_build] Error: HTTP {e.code} - {err_msg}", file=sys.stderr)
        return False
    except Exception as e:
        print(f"[send_build] Error: {e}", file=sys.stderr)
        return False


def main():
    parser = argparse.ArgumentParser(description="Send Apex build to Discord.")
    parser.add_argument("--jar", help="Path to jar file")
    parser.add_argument("--webhook", help="Discord webhook URL")
    parser.add_argument("--notes", help="Optional release notes")
    args = parser.parse_args()

    root = find_repo_root()
    webhook_url = args.webhook or os.environ.get("APEX_BUILD_WEBHOOK_URL")
    if not webhook_url:
        print("[send_build] No webhook configured. Set APEX_BUILD_WEBHOOK_URL or use --webhook", file=sys.stderr)
        sys.exit(1)

    jar_path = Path(args.jar) if args.jar else find_latest_jar(root)
    if not jar_path or not jar_path.exists():
        print(f"[send_build] JAR not found at {jar_path or (root / 'build' / 'libs')}", file=sys.stderr)
        sys.exit(1)

    git_info = get_git_info(root)
    print(f"==> Uploading {jar_path.name} ({jar_path.stat().st_size / (1024 * 1024):.2f} MB) to Discord...")
    success = send_to_webhook(webhook_url, jar_path, git_info, args.notes)
    if success:
        print(f"Successfully sent {jar_path.name} to Discord!")
    else:
        print(f"Failed to send build to Discord.", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()

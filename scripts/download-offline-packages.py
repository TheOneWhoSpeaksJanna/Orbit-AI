#!/usr/bin/env python3
"""
Download Termux .deb packages for offline installation.

This script runs during the Gradle build to pre-bundle nodejs, git, python3
and all their dependencies as .deb files in app/src/main/assets/offline-debs/.

At runtime, TermuxRuntime uses `dpkg -i` to install these offline, eliminating
the 5-minute apt download on first launch.

Usage:
    python3 scripts/download-offline-packages.py [output_dir]

If output_dir is not specified, defaults to app/src/main/assets/offline-debs/.
"""
import os
import sys
import urllib.request

PACKAGES_URL = "https://packages.termux.dev/apt/termux-main/dists/stable/main/binary-aarch64/Packages"
BASE_URL = "https://packages.termux.dev/apt/termux-main/"
OUTPUT_DIR = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/offline-debs"

# Packages needed for nodejs, git, python3 + all their dependencies.
# This list comes from the apt install output on-device:
# "The following additional packages will be installed: c-ares clang gdbm..."
NEEDED_PACKAGES = [
    "c-ares", "clang", "gdbm", "git", "glib", "krb5", "ldns",
    "libcompiler-rt", "libcrypt", "libdb", "libedit", "libexpat",
    "libffi", "libicu", "libllvm", "libresolv-wrapper", "libsqlite",
    "libxml2", "lld", "llvm", "make", "ncurses-ui-libs", "ndk-sysroot",
    "nodejs", "npm", "openssh", "openssh-sftp-server", "pkg-config",
    "python", "python-ensurepip-wheels", "python-pip", "termux-auth",
]

def parse_packages_index(text):
    """Parse RFC 822-style Packages index."""
    packages = {}
    current = {}
    for line in text.split('\n'):
        if line.strip() == '':
            if 'Package' in current:
                packages[current['Package']] = current
            current = {}
        elif ':' in line:
            key, value = line.split(':', 1)
            current[key.strip()] = value.strip()
    if 'Package' in current:
        packages[current['Package']] = current
    return packages

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # Check if we already have all packages (skip if cached)
    existing = set(os.listdir(OUTPUT_DIR))
    if len(existing) >= len(NEEDED_PACKAGES):
        print(f"Offline packages already present ({len(existing)} files), skipping download")
        return 0

    print(f"Downloading Packages index from {PACKAGES_URL}...")
    try:
        with urllib.request.urlopen(PACKAGES_URL, timeout=30) as resp:
            index_text = resp.read().decode('utf-8')
    except Exception as e:
        print(f"ERROR: Failed to download Packages index: {e}")
        print("Falling back to online apt install at runtime.")
        return 1

    packages = parse_packages_index(index_text)
    print(f"Parsed {len(packages)} packages from index")

    downloaded = 0
    skipped = 0
    failed = 0

    for name in NEEDED_PACKAGES:
        if name not in packages:
            print(f"  WARNING: Package '{name}' not found in index, skipping")
            failed += 1
            continue

        filename = packages[name].get('Filename', '')
        if not filename:
            print(f"  WARNING: No Filename for package '{name}', skipping")
            failed += 1
            continue

        basename = os.path.basename(filename)
        local_path = os.path.join(OUTPUT_DIR, basename)

        if os.path.exists(local_path):
            print(f"  EXISTS: {basename}")
            skipped += 1
            continue

        url = BASE_URL + filename
        try:
            print(f"  Downloading: {basename}...")
            urllib.request.urlretrieve(url, local_path)
            downloaded += 1
        except Exception as e:
            print(f"  ERROR downloading {basename}: {e}")
            failed += 1
            # Remove partial file
            if os.path.exists(local_path):
                os.remove(local_path)

    print(f"\nDone! Downloaded: {downloaded}, Skipped (cached): {skipped}, Failed: {failed}")
    if failed > 0:
        print("WARNING: Some packages failed to download. Runtime will fall back to apt install.")
    return 0

if __name__ == '__main__':
    sys.exit(main())

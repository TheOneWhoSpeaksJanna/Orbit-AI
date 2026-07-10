#!/usr/bin/env python3
"""
Download OpenClaude npm package as a tarball for offline installation.

Downloads @gitlawb/openclaude from npm registry and saves it as a .tgz
file in the flavor's assets directory. This lets the app install
OpenClaude without network access on first launch.

Usage: python3 download-openclaude-tarball.py [output_dir]
"""
import os
import sys
import subprocess
import shutil

PACKAGE = "@gitlawb/openclaude"
OUTPUT_DIR = sys.argv[1] if len(sys.argv) > 1 else "app/src/openclaude/assets"

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    # Check if tarball already exists
    existing = [f for f in os.listdir(OUTPUT_DIR) if f.endswith('.tgz') and 'openclaude' in f.lower()]
    if existing:
        print(f"OpenClaude tarball already exists: {existing[0]}, skipping download")
        print(f"  Delete {OUTPUT_DIR}/{existing[0]} to force re-download")
        return 0
    
    print(f"Downloading {PACKAGE} tarball from npm...")
    
    # Use npm pack to download the tarball
    result = subprocess.run(
        ["npm", "pack", PACKAGE, "--pack-destination", OUTPUT_DIR],
        capture_output=True, text=True, timeout=120
    )
    
    if result.returncode != 0:
        print(f"ERROR: npm pack failed: {result.stderr}")
        return 1
    
    # Find the generated tarball
    tarballs = [f for f in os.listdir(OUTPUT_DIR) if f.endswith('.tgz')]
    if not tarballs:
        print("ERROR: No tarball was created by npm pack")
        return 1
    
    tarball = tarballs[0]
    # Rename to a predictable name
    target = os.path.join(OUTPUT_DIR, "openclaude.tgz")
    if tarball != "openclaude.tgz":
        src = os.path.join(OUTPUT_DIR, tarball)
        shutil.move(src, target)
    
    size = os.path.getsize(target)
    print(f"Done! Tarball saved: {target} ({size / 1024 / 1024:.1f} MB)")
    return 0

if __name__ == '__main__':
    sys.exit(main())

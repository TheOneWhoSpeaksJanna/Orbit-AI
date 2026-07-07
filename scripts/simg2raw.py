#!/usr/bin/env python3
"""Convert Android sparse image (simg) to raw ext4 image.

Usage: python3 simg2raw.py <input.img> <output.raw>

If the input is not a sparse image, copies it as-is.
"""
import struct
import sys
import os

SPARSE_MAGIC = 0x3AFF26ED

def convert_sparse(input_path, output_path):
    with open(input_path, 'rb') as f:
        magic = struct.unpack('<I', f.read(4))[0]
        if magic != SPARSE_MAGIC:
            print(f"Not a sparse image (magic=0x{magic:08X}), copying as-is")
            f.seek(0)
            with open(output_path, 'wb') as out:
                out.write(f.read())
            return

        # Parse sparse image header
        header = struct.unpack('<HHHHIIII', f.read(36))
        major_version, minor_version = header[0], header[1]
        file_hdr_sz = header[2]
        chunk_hdr_sz = header[3]
        blk_sz = header[4]
        total_blks = header[5]
        total_chunks = header[6]

        print(f"Sparse: v{major_version}.{minor_version} blk_size={blk_sz} "
              f"total_blks={total_blks} chunks={total_chunks}")

        # Skip any extra header bytes
        if file_hdr_sz > 40:
            f.seek(file_hdr_sz - 40, 1)

        with open(output_path, 'wb') as out:
            for i in range(total_chunks):
                chunk_data = f.read(12)
                if len(chunk_data) < 12:
                    print(f"Truncated chunk header at chunk {i}")
                    break
                chunk_type, chunk_sz, chunk_sz_blks = struct.unpack('<HHI', chunk_data)

                # Skip extra chunk header bytes
                if chunk_hdr_sz > 12:
                    f.seek(chunk_hdr_sz - 12, 1)

                data_sz = chunk_sz_blks * blk_sz
                if chunk_type == 0xCAC1:  # RAW
                    out.write(f.read(data_sz))
                elif chunk_type == 0xCAC2:  # FILL
                    fill = f.read(4)
                    out.write(fill * (data_sz // 4))
                elif chunk_type == 0xCAC3:  # DON'T CARE
                    out.write(b'\x00' * data_sz)
                else:
                    print(f"  Chunk {i}: unknown type 0x{chunk_type:04X}, "
                          f"writing zeros ({data_sz} bytes)")
                    out.write(b'\x00' * data_sz)

        print(f"Converted to raw: {os.path.getsize(output_path)} bytes")

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage: simg2raw.py <input.img> <output.raw>")
        sys.exit(1)
    convert_sparse(sys.argv[1], sys.argv[2])

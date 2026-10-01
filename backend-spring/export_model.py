"""
One-time script: export all-MiniLM-L6-v2 to ONNX format.

Run once before first launch:
    pip install optimum[onnxruntime]
    python export_model.py
"""

import subprocess, sys

subprocess.run([
    sys.executable, "-m", "optimum.exporters.onnx",
    "--model", "sentence-transformers/all-MiniLM-L6-v2",
    "--task", "feature-extraction",
    "src/main/resources/model/"
], check=True)

print("Model exported to src/main/resources/model/")

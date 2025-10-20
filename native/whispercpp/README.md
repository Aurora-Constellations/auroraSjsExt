# Whisper.cpp Native Assets

This folder contains prebuilt Whisper.cpp binaries for the extension.

## Included
- `macos-arm64/whisper-cli`: compiled on Apple Silicon (macOS 15, clang via CMake `Release`).
- Dependent dylibs (`libwhisper`, `libggml*`) with install names rewritten to load from the same directory.
- Metal shader blob `ggml-metal.metal`.

## Models
Drop GGML models under `native/whispercpp/models/`, eg :

```
# download base.en model
your/project$ ./native/whispercpp/download-model.sh base.en
```

or manually copy `ggml-*.bin` files into the `models/` directory. Set `WHISPER_CPP_MODEL` if you store them elsewhere.

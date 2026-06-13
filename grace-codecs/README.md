# Grace Codecs

`grace-codecs` is a foundational module that provides codec extension methods for encoding and decoding operations in Grace applications. It works in conjunction with `grace-plugin-codecs` to deliver a comprehensive codec system for data transformation and security.

The `grace-codecs` module provides the low-level codec extension methods, while `grace-plugin-codecs` provides the plugin layer that integrates these codecs with the Grace framework's artefact system.

## Important APIs in grace-codecs

### Groovy Extension Module

**Extension Module Configuration** - The module registers as a Groovy extension module (`grace-codecs`) that provides codec extension methods to String and other types. The extension classes include:

- `Base64CodecExtensionMethods` - Base64 encoding/decoding
- `HexCodecExtensionMethods` - Hexadecimal encoding/decoding
- `MD5BytesCodecExtensionMethods` - MD5 hash (bytes)
- `MD5CodecExtensionMethods` - MD5 hash (string)
- `SHA1BytesCodecExtensionMethods` - SHA-1 hash (bytes)
- `SHA1CodecExtensionMethods` - SHA-1 hash (string)
- `SHA256BytesCodecExtensionMethods` - SHA-256 hash (bytes)
- `SHA256CodecExtensionMethods` - SHA-256 hash (string)

## Important APIs in grace-plugin-codecs

### Codec Artefact Interface

**`GrailsCodecClass`** - Interface for codec classes that provide encode and decode methods or closure properties. It extends `InjectableGrailsClass` and `CodecFactory`, with a method to configure codec methods.

### Codec Lookup

**`DefaultCodecLookup`** - Implementation that discovers and registers codec artefacts from the GrailsApplication. It:
- Retrieves all codec artefacts via `CodecArtefactHandler.TYPE`
- Sorts codecs by order and registers them
- Configures codec methods before registration

### Plugin Configuration

**`CodecsGrailsPlugin`** - The Grace plugin that configures pluggable codecs and provides built-in codec artefacts. Provided artefacts include:

- `HTMLCodec` - HTML encoding
- `HTML4Codec` - HTML4 encoding
- `JavaScriptCodec` - JavaScript encoding
- `HTMLJSCodec` - Combined HTML/JavaScript encoding
- `URLCodec` - URL encoding
- `RawCodec` - No encoding (passthrough)


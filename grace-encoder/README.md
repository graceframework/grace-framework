# Grace Encoder

`grace-encoder` is a foundational module that provides the core encoding and decoding infrastructure for Grace applications, including interfaces and classes for content encoding, codec factories, and streaming encoders.


### Core Encoder Interfaces

* `Encoder` - Interface for encoding data, used throughout the framework for content transformation and security purposes.
* `Decoder` - Interface for decoding data, complementing the encoder interface.
* `CodecFactory` - Factory interface for creating encoder and decoder instances, used by codec classes to provide their encoding/decoding implementations.
* `EncoderAware` - Interface that marks an instance as capable of providing information about the current encoder in use.

### Streaming and State Management

* `StreamingEncoder` - Interface for streaming encoding operations that can handle large data efficiently.
* `EncodingState` - Represents the current state of encoding operations.
* `EncodingStateRegistry` - Registry for managing encoding states across the application.
* `EncodedAppender` - Interface for appending encoded content.
* `Encodeable` - Interface for objects that can be encoded.

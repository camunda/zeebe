# Messaging

Zeebe uses Netty for inter-cluster messaging, namely
its [NettyMessagingService](/zeebe/atomix/cluster/src/main/java/io/atomix/cluster/messaging/impl/NettyMessagingService.java)
and [NettyUnicastService](/zeebe/atomix/cluster/src/main/java/io/atomix/cluster/messaging/impl/NettyUnicastService.java).

> [!Warning]
> This document still needs to be expanded to better document how messaging in handled in Zeebe.
> Don't hesitate to add to it.

## Reliable messaging

[NettyMessagingService](/zeebe/atomix/cluster/src/main/java/io/atomix/cluster/messaging/impl/NettyMessagingService.java)
allows sending messages reliably over TCP (with optional TLSv1.3) between cluster nodes.

Each message is framed with a lightweight protocol. To support backwards and future compatibility,
the server and client will negotiate the protocol version upon connection.

- [Version 1](/zeebe/atomix/cluster/src/main/java/io/atomix/cluster/messaging/impl/MessagingProtocolV1.java)
- [Version 2](/zeebe/atomix/cluster/src/main/java/io/atomix/cluster/messaging/impl/MessagingProtocolV2.java)

### Protocol handshake

The protocol negotiation works as follows:

- Client writes its fixed preamble, _the hash of the cluster name_ (using the built-in Java hash
  code of `String`) as a 32-bit signed integer.
- Client then writes its protocol version as a 16-bit signed short.
- Server reads the preamble.
  - If it **does not** match its own, the connection is simply closed preemptively.
- If the preamble matches, the server will read the protocol version.
  - If it understands this version, it will write
    back that version to the client.
  - If the server cannot understand this protocol version, it will write back its own latest
    protocol version.
- The client reads whichever version the server wrote back, and if it can, activates this version.
- If the client cannot, it will close the connection preemptively.
- If the handshake is successful, _all messages on this channel are expected to be of this version,
  and the handshake is never performed again._

### Protocol format

> [!Note]
> Version 2 has been around for years, so it's almost impossible to run into version 1 in
> the wild. As such, documentation will focus on version 2 for now.

Once the handshake is performed, messages for this channel will now be framed with the following
envelope for V2:

- **Length-prefixed sender address**: 16-bit signed short (indicating the `length` of the
  sender address), followed by `length` bytes representing the sender's advertised address. This is
  a UTF-8 character encoded hostname.
- The port of the sender: 32-bit signed integer of the port on which replies should be sent, used
  in conjunction with the IP address.

The address is only sent with the first message on this channel. Subsequent messages will omit
framing the message with the address.

After this, each message is framed with the following:

- **Message type**: a 8-bit signed byte representing the ID of one
  of [ProtocolMessage.Type](/zeebe/atomix/cluster/src/main/java/io/atomix/cluster/messaging/impl/ProtocolMessage.java)
- **Message ID**: 64-bit signed long representing a pseudo-unique ID of the message. This
  message is unique per node iteration, as it's generated by an in-memory counter which restarts at
  0 on every node restart.
- **Length-prefixed message payload**: a 32-bit signed integer representing the `length` of the
  payload in bytes, followed by `length` bytes representing the payload itself. This payload is a
  raw bytes array provided by the caller, meaning its semantics are dependent on the message subject
  itself.

If the message is a `request`, then this is followed by:

- **Length-prefixed message subject**: a 32-bit signed integer representing the `length` of the
  subject in bytes, followed by `length` bytes of subject's UTF-8 encoded string representation.

If the message is a `reply`, then this is instead followed by:

- **ID of the reply status**: an 8-bit signed byte representing the status of the reply, e.g. it was
  successful, failed, etc.
  See [ProtocolReply.Status](/zeebe/atomix/cluster/src/main/java/io/atomix/cluster/messaging/impl/ProtocolReply.java)
  for more.

### Messaging exceptions

When sending messages between nodes, there's a shared set of exceptions that may be returned by the
messaging service on top of whatever subject specific exceptions your own code may return.

The recommendation is to encode business logic failures as successful protocol messages, and handle
their own semantics near the calling site and **not** in the messaging layer.

As such, exceptions returned by the messaging layer typically will indicate a real issue, and you
should handle them whenever sending any requests through the messaging layer:

- `java.lang.IllegalStateException` - indicates the underlying service cannot send the given
  request (e.g. the service is not running)
- `java.net.ConnectException` - indicates the recipient is unreachable; typically this is temporary,
  and should be logged as a warning and retried.
- `java.util.concurrent.TimeoutException` - indicates no response came back after the default
  timeout.
- `io.atomix.cluster.messaging.MessagingException.ProtocolException` - indicates the recipient
  failed to parse the incoming message. This is typically not retry-able, as the encoding logic is
  independent of the input, so if two nodes cannot encode/decode in a format they both understand,
  there's no point retrying.
- `io.atomix.cluster.messaging.MessagingException.NoRemoteHandler` - indicates the recipient
  received the message, but has no handler registered for it. This can be transient - for example,
  if the recipient is shutting down, starting up, or in a state transition such that it is
  recreating its handlers. In such cases, this can be retried. However, if this is consistently
  happening, then this indicates a real error.
- `io.atomix.cluster.messaging.MessagingException.RemoteHandlerFailure` - indicates the recipient
  parsed and expected the message, but it failed unexpectedly to process it. Whether this is
  retry-able or not depends on the business logic around this subject, and is up to the caller to
  decide. Best practice, however, is to handle expected business errors as successful protocol
  replies, such that this error is returned _only_ for unexpected exceptions, which would normally
  not be retry-able.

On top of these, you also have an additional exception when using
the [ClusterCommunicationService](/zeebe/atomix/cluster/src/main/java/io/atomix/cluster/messaging/ClusterCommunicationService.java),
which is used specifically to communicate between members of the same cluster via their IDs.

- `io.atomix.cluster.messaging.MessagingException.NoSuchMemberException` - this can occur in two
  ways. This can be returned immediately at the calling site (so on the client side) if the there is
  no known member with the given ID in the cluster (obtained via the membership protocol). It can also
  be returned by the server if the request was received, parsed, but the server does not know any
  member matching the sender's address. Make sure to read the exception message to figure out which
  case. In either case, this is typically transient and can be retried, but if it persists, this
  likely indicates an actual error.


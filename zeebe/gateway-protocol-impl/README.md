# Zeebe Gateway Protocol Implementation

Zeebe client grpc protobuf protocol.

## Go Code Generation

### Prerequisite

To generate the go grpc stubs you need the following tools:

1. Go >= 1.19 (https://golang.org/dl/)
2. The go protoc generator

   ```
   go install github.com/golang/protobuf/protoc-gen-go@latest
   ```
3. Maven >= 3.3.1 (https://maven.apache.org/download.cgi)

### Generate Stubs

To generate the go stubs run

```
mvn clean generate-sources -P golang
```

the stubs will be generated in the `clients/go/pkg/pb` directory.


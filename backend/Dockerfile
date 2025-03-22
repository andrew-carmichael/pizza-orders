# Build stage
FROM golang:1.21 AS build
WORKDIR /app
COPY . .
ENV CGO_ENABLED=0 GOOS=linux GOARCH=amd64
RUN go build -o server .

# Run stage
FROM debian:bullseye-slim
WORKDIR /app
COPY --from=build /app/server .
EXPOSE 8080
CMD ["./server"]

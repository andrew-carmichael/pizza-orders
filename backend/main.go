package main

import (
    "encoding/json"
    "log"
    "net/http"
)

// This is the data structure we'll return as JSON
type HelloResponse struct {
    Message string `json:"message"`
}

// This handles GET requests to /hello
func helloHandler(w http.ResponseWriter, r *http.Request) {
    response := HelloResponse{Message: "Hello from your Go backend!"}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response)
}

func main() {
    http.HandleFunc("/hello", helloHandler)
    log.Println("Listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

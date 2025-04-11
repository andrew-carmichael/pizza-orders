package main

import (
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"

	"pizza-orders/orders"
	"pizza-orders/clients"
)

var (
	simulating      = false
	simulationMutex sync.Mutex

	upgrader = websocket.Upgrader{
		CheckOrigin: func(r *http.Request) bool { return true },
	}
)

func main() {
	rand.Seed(time.Now().UnixNano())

	go func() {
		for {
			time.Sleep(1 * time.Minute)
			orders.CleanupOldOrders(5 * time.Minute)
		}
	}()

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintln(w, "Pizza order WebSocket backend is running.")
	})
	http.HandleFunc("/ws", handleWebSocket)
	log.Println("Server listening on :8080")
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func handleWebSocket(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("WebSocket upgrade error:", err)
		return
	}
	defer conn.Close()

	clients.Register(conn)
	defer clients.Unregister(conn)

	log.Println("Client connected:", conn.RemoteAddr())

	for _, order := range orders.GetActive() {
		data, err := json.Marshal(order)
		if err != nil {
			log.Println("Failed to marshal order: ", err)
			continue
		}
		err = conn.WriteMessage(websocket.TextMessage, data)
		if err != nil {
			log.Println("Failed to sync active orders: ", err)
			conn.Close()
		}
	}

	if clients.Count() == 1 {
		go startSimulation()
	}

	for {
		_, msg, err := conn.ReadMessage()
		if err != nil {
			log.Println("Read error:", err)
			return
		}

		var action struct {
			Action  string `json:"action"`
			OrderID string `json:"orderId"`
		}
		if err := json.Unmarshal(msg, &action); err != nil {
			log.Println("Invalid message format:", err)
			continue
		}

		if action.Action == "complete" {
			order := orders.Get(action.OrderID)
			if order != nil {
				order.MarkDone()
				Broadcast(order)
			}
		}

	}
}

func startSimulation() {
	simulationMutex.Lock()
	if simulating {
		simulationMutex.Unlock()
		return
	}
	simulating = true
	simulationMutex.Unlock()

	log.Println("Starting simulation")

	for i := 0; i < rand.Intn(3)+3; i++ {
		log.Println("Generating initial order...")
		order := orders.MakeFakeOrder()
		if order == nil {
			log.Println("Generated order is nil")
		} else {
			log.Printf("Generated order: %+v", order)
		}
		log.Println("Calling Broadcast()")
		Broadcast(order)
	}

	for {
		time.Sleep(20 * time.Second)

		if clients.Count() == 0 {
			log.Println("No clients connected. Stopping simulation.")
			simulationMutex.Lock()
			simulating = false
			simulationMutex.Unlock()
			return
		}

		activeOrders := orders.GetActive()

		if len(activeOrders) == 0 || rand.Float64() < 0.6 {
			order := orders.MakeFakeOrder()
			Broadcast(order)
		} else {
			index := rand.Intn(len(activeOrders))
			order := activeOrders[index]
			order.MarkDone()
			Broadcast(order)
		}
	}
}


func Broadcast(o *orders.Order) {
	log.Println("===> Broadcast() entered")
	if o == nil {
		log.Println("Broadcast called with nil order")
		return
	}

	data, err := json.Marshal(o)
	if err != nil {
		log.Println("Failed to marshal order:", err)
		return
	}

	log.Printf("Broadcasting order: %+v\n", o)

	clients.ForEach(func(conn *websocket.Conn) {
		log.Printf("Sending order to client: %v", conn.RemoteAddr())
		err := conn.WriteMessage(websocket.TextMessage, data)
		if err != nil {
			log.Println("Write error, closing connection:", err)
			conn.Close()
			clients.Unregister(conn)
		}
	})
}


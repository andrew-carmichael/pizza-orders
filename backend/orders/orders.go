package orders

import (
	"fmt"
	"math/rand"
	"sync"
	"time"
)

var (
	orders      = make(map[string]*Order)
	ordersMutex sync.Mutex
)

type Order struct {
	ID           string `json:"id"`
	Pizza        string `json:"pizza"`
	CustomerName string `json:"customerName"`
	Address      string `json:"address"`
	PhoneNumber  string `json:"phoneNumber"`
	Status       string `json:"status"`
	Timestamp    string `json:"timestamp"`
}

func (o *Order) MarkDone() *Order {
	o.Status = "done"
	return o
}

func MakeFakeOrder() *Order {
	ordersMutex.Lock()
	defer ordersMutex.Unlock()
	order := generateFakeOrder()
	orders[order.ID] = order
	return order
}

func Get(orderId string) *Order {
	ordersMutex.Lock()
	defer ordersMutex.Unlock()
	return orders[orderId]
}

func GetActive() []*Order {
	ordersMutex.Lock()
	defer ordersMutex.Unlock()
	var active []*Order
	for _, order := range orders {
		if order.Status != "done" {
			active = append(active, order)
		}
	}
	return active
}

func CleanupOldOrders(cutoff time.Duration) {
	ordersMutex.Lock()
	defer ordersMutex.Unlock()
	now := time.Now()
	removed := 0
	for id, order := range orders {
		if order.Status == "done" {
			t, err := time.Parse(time.RFC3339, order.Timestamp)
			if err != nil {
				continue
			}
			if now.Sub(t) > cutoff {
				delete(orders, id)
				removed++
			}
		}
	}
	if removed > 0 {
		fmt.Println("Cleaned up %d old orders", removed)
	}
}

func generateFakeOrder() *Order {
	id := fmt.Sprintf("order_%d", time.Now().UnixNano())
	return &Order{
		ID:           id,
		Pizza:        randomPizza(),
		CustomerName: randomName(),
		Address:      fmt.Sprintf("%d %s", rand.Intn(9999)+1, randomStreet()),
		PhoneNumber:  fmt.Sprintf("(555) %03d-%04d", rand.Intn(800)+100, rand.Intn(10000)),
		Status:       "new",
		Timestamp:    time.Now().Format(time.RFC3339),
	}
}

var pizzas = []string{"Margherita", "Pepperoni", "Hawaiian", "Veggie", "BBQ Chicken", "Meat Lovers", "Four Cheese"}
var firstNames = []string{"Alex", "Jordan", "Taylor", "Sam", "Jamie", "Morgan", "Chris", "Casey"}
var lastNames = []string{"Smith", "Johnson", "Lee", "Martinez", "Brown", "Garcia", "Nguyen", "Williams"}
var streetNames = []string{"Main St", "Maple Ave", "Elm St", "Pine Rd", "Oak Dr", "Sunset Blvd", "River Rd"}

func randomPizza() string {
	return pizzas[rand.Intn(len(pizzas))]
}

func randomName() string {
	return fmt.Sprintf("%s %s", firstNames[rand.Intn(len(firstNames))], lastNames[rand.Intn(len(lastNames))])
}

func randomStreet() string {
	return streetNames[rand.Intn(len(streetNames))]
}


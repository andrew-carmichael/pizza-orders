package clients

import(
	"sync"

	"github.com/gorilla/websocket"
)

var (
	clients      = make(map[*websocket.Conn]bool)
	clientsMutex sync.Mutex
)

func Register(conn *websocket.Conn) {
	clientsMutex.Lock()
	clients[conn] = true
	clientsMutex.Unlock()
}

func Unregister(conn *websocket.Conn) {
	clientsMutex.Lock()
	delete(clients, conn)
	clientsMutex.Unlock()
}

func Count() int {
	clientsMutex.Lock()
	defer clientsMutex.Unlock()
	return len(clients)
}

func ForEach(fn func(conn *websocket.Conn)) {
	clientsMutex.Lock()
	conns := make([]*websocket.Conn, 0, len(clients))
	for conn := range clients {
		conns = append(conns, conn)
	}
	clientsMutex.Unlock()
	for _, conn := range conns {
		fn(conn)
	}
}

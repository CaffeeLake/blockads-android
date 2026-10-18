package tuic

import (
	"encoding/binary"
	"net"
	"testing"

	"github.com/gofrs/uuid/v5"
)

func TestTUICConfigParsing(t *testing.T) {
	jsonStr := `{
		"tag": "tuic-sg",
		"server": "1.2.3.4",
		"port": 8443,
		"uuid": "22ff1129-b68e-42c2-b7e1-8f5b84c0a52d",
		"password": "secretpassword",
		"sni": "tuic.example.com",
		"alpn": "h3",
		"allow_insecure": true
	}`

	cfg, err := FromJSON(jsonStr)
	if err != nil {
		t.Fatalf("FromJSON error: %v", err)
	}

	if cfg.Tag != "tuic-sg" || cfg.Server != "1.2.3.4" || cfg.Port != 8443 {
		t.Errorf("unexpected parsed config: %+v", cfg)
	}
	if cfg.SNI != "tuic.example.com" || !cfg.AllowInsecure {
		t.Errorf("unexpected tls fields: %+v", cfg)
	}

	client, err := NewClient(cfg, nil)
	if err != nil {
		t.Fatalf("NewClient error: %v", err)
	}

	if client.Protocol() != "tuic" {
		t.Errorf("expected protocol 'tuic', got '%s'", client.Protocol())
	}
	if client.ServerAddress() != "1.2.3.4:8443" {
		t.Errorf("expected serverAddr '1.2.3.4:8443', got '%s'", client.ServerAddress())
	}
}

func TestTUICHeaderConstruction(t *testing.T) {
	// 1. Auth Header: VER(1)=0x05, TYPE(1)=0x00, UUID(16), TOKEN(32)
	testUUIDStr := "22ff1129-b68e-42c2-b7e1-8f5b84c0a52d"
	parsedUUID, err := uuid.FromString(testUUIDStr)
	if err != nil {
		t.Fatalf("uuid.FromString error: %v", err)
	}

	token := make([]byte, 32)
	for i := range token {
		token[i] = byte(i)
	}

	authHeader := make([]byte, 50)
	authHeader[0] = 0x05
	authHeader[1] = 0x00
	copy(authHeader[2:18], parsedUUID.Bytes())
	copy(authHeader[18:50], token)

	if len(authHeader) != 50 {
		t.Fatalf("auth header length should be 50, got %d", len(authHeader))
	}
	if authHeader[0] != 0x05 || authHeader[1] != 0x00 {
		t.Errorf("invalid auth header ver/type")
	}

	// 2. Connect Header IPv4: VER(1)=0x05, TYPE(1)=0x01, ADDR_TYPE(1)=0x01, ADDR(4), PORT(2)
	ip4 := net.ParseIP("192.168.1.1").To4()
	header4 := make([]byte, 2+1+4+2)
	header4[0] = 0x05
	header4[1] = 0x01
	header4[2] = 0x01
	copy(header4[3:7], ip4)
	binary.BigEndian.PutUint16(header4[7:9], 443)

	if len(header4) != 9 {
		t.Fatalf("ipv4 connect header length should be 9, got %d", len(header4))
	}
	if header4[0] != 0x05 || header4[1] != 0x01 || header4[2] != 0x01 {
		t.Errorf("invalid ipv4 connect header flags")
	}
	port := binary.BigEndian.Uint16(header4[7:9])
	if port != 443 {
		t.Errorf("expected port 443, got %d", port)
	}
}

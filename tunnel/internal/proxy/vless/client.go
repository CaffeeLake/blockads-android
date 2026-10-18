package vless

import (
	"crypto/tls"
	"encoding/binary"
	"fmt"
	"io"
	"net"
	"strconv"
	"syscall"
	"time"

	"github.com/nqmgaming/blockads-tunnel/internal/proxy"
)

// Client handles communication to a VLESS server.
type Client struct {
	config     *Config
	rawUUID    [16]byte
	serverAddr string
	protectFn  func(fd int) bool
}

// Ensure Client implements proxy.Client
var _ proxy.Client = (*Client)(nil)

// NewClient creates an initialized VLESS client.
func NewClient(cfg *Config, protectFn func(fd int) bool) (*Client, error) {
	rawUUID, err := ParseUUID(cfg.UUID)
	if err != nil {
		return nil, err
	}

	return &Client{
		config:     cfg,
		rawUUID:    rawUUID,
		serverAddr: net.JoinHostPort(cfg.Server, strconv.Itoa(cfg.Port)),
		protectFn:  protectFn,
	}, nil
}

// Protocol returns "vless".
func (c *Client) Protocol() string {
	return "vless"
}

// ServerAddress returns host:port.
func (c *Client) ServerAddress() string {
	return c.serverAddr
}

// Tag returns profile tag or name.
func (c *Client) Tag() string {
	return c.config.Tag
}

// Config returns underlying config.
func (c *Client) Config() *Config {
	return c.config
}

// SetProtectFn updates the socket protection callback.
func (c *Client) SetProtectFn(fn func(fd int) bool) {
	c.protectFn = fn
}

// DialTCP establishes a VLESS tunnel to targetIP:targetPort.
func (c *Client) DialTCP(targetIP net.IP, targetPort int) (net.Conn, error) {
	dialer := &net.Dialer{
		Timeout: 10 * time.Second,
		Control: func(network, address string, cRaw syscall.RawConn) error {
			if c.protectFn == nil {
				return nil
			}
			var protectErr error
			err := cRaw.Control(func(fd uintptr) {
				if !c.protectFn(int(fd)) {
					protectErr = fmt.Errorf("protect socket fd=%d failed", fd)
				}
			})
			if err != nil {
				return err
			}
			return protectErr
		},
	}

	rawConn, err := dialer.Dial("tcp", c.serverAddr)
	if err != nil {
		return nil, fmt.Errorf("dial vless server %s failed: %w", c.serverAddr, err)
	}

	var sessionConn net.Conn = rawConn

	var tlsConfig *tls.Config
	if c.config.Security == "tls" {
		tlsConfig = &tls.Config{
			ServerName:         c.config.SNI,
			InsecureSkipVerify: c.config.AllowInsecure,
			NextProtos:         c.config.ALPN,
		}
		tlsConn := tls.Client(rawConn, tlsConfig)
		if err := tlsConn.Handshake(); err != nil {
			rawConn.Close()
			return nil, fmt.Errorf("vless tls handshake to %s failed: %w", c.serverAddr, err)
		}
		sessionConn = tlsConn
	}

	if c.config.Network == "ws" {
		wsPath := c.config.WSPath
		if wsPath == "" {
			wsPath = "/"
		}
		wsScheme := "ws"
		if c.config.Security == "tls" {
			wsScheme = "wss"
		}
		wsURL := fmt.Sprintf("%s://%s%s", wsScheme, c.serverAddr, wsPath)
		wsConn, err := proxy.DialWebSocket(sessionConn, wsURL, c.config.WSHost, tlsConfig)
		if err != nil {
			sessionConn.Close()
			return nil, err
		}
		sessionConn = wsConn
	}

	// Build VLESS request header:
	// [1 byte version (0x00)]
	// [16 bytes raw UUID]
	// [1 byte addon len (0x00)]
	// [1 byte command (0x01 TCP)]
	// [2 bytes port (BigEndian)]
	// [1 byte address type: 0x01=IPv4, 0x03=IPv6]
	// [address bytes]
	header := make([]byte, 0, 1+16+1+1+2+1+16)
	header = append(header, 0x00) // version 0
	header = append(header, c.rawUUID[:]...)
	header = append(header, 0x00) // addon length 0
	header = append(header, 0x01) // TCP command

	portBuf := make([]byte, 2)
	binary.BigEndian.PutUint16(portBuf, uint16(targetPort))
	header = append(header, portBuf...)

	ipv4 := targetIP.To4()
	if ipv4 != nil {
		header = append(header, 0x01) // IPv4
		header = append(header, ipv4...)
	} else {
		header = append(header, 0x03) // IPv6 (VLESS: 0x03 is IPv6)
		header = append(header, targetIP.To16()...)
	}

	if _, err := sessionConn.Write(header); err != nil {
		sessionConn.Close()
		return nil, fmt.Errorf("write vless header failed: %w", err)
	}

	// Read VLESS response header:
	// [1 byte version: 0x00]
	// [1 byte addon length: M]
	// [M bytes addon payload]
	respHeader := make([]byte, 2)
	if _, err := io.ReadFull(sessionConn, respHeader); err != nil {
		sessionConn.Close()
		return nil, fmt.Errorf("read vless response header failed: %w", err)
	}

	addonLen := int(respHeader[1])
	if addonLen > 0 {
		addonBuf := make([]byte, addonLen)
		if _, err := io.ReadFull(sessionConn, addonBuf); err != nil {
			sessionConn.Close()
			return nil, fmt.Errorf("read vless addon payload failed: %w", err)
		}
	}

	return sessionConn, nil
}

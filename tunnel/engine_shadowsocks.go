package tunnel

import (
	"net"

	"github.com/nqmgaming/blockads-tunnel/internal/shadowsocks"
)

// SetShadowsocksConfig configures the Shadowsocks / Outline client.
// An empty string clears the active Shadowsocks client.
func (e *Engine) SetShadowsocksConfig(configJSON string) error {
	if configJSON == "" {
		e.ssClient.Store(nil)
		logf("Shadowsocks client cleared")
		return nil
	}
	cfg, err := shadowsocks.FromJSON(configJSON)
	if err != nil {
		return err
	}
	client, err := shadowsocks.NewClient(cfg, e.protectFn)
	if err != nil {
		return err
	}
	e.ssClient.Store(client)
	logf("Shadowsocks client configured for server %s (tag: %s)", client.ServerAddress(), cfg.Tag)
	return nil
}

// GetShadowsocksConfig returns the current Shadowsocks config JSON or empty string.
func (e *Engine) GetShadowsocksConfig() string {
	client := e.ssClient.Load()
	if client == nil || client.Config() == nil {
		return ""
	}
	return client.Config().ToJSON()
}

// DialProxy attempts to dial target via Shadowsocks if active.
// Returns (conn, true, err) if Shadowsocks handled it, or (nil, false, nil) if Shadowsocks is not active.
func (e *Engine) DialProxy(targetIP net.IP, targetPort int) (net.Conn, bool, error) {
	if ss := e.ssClient.Load(); ss != nil {
		conn, err := ss.DialTCP(targetIP, targetPort)
		return conn, true, err
	}
	return nil, false, nil
}


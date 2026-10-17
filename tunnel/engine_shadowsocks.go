package tunnel

import (
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
